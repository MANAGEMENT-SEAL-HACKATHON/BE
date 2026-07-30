package com.sealhackathon.api.submissions.service.impl;

import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.GitHubProperties;
import com.sealhackathon.api.submissions.dto.response.SubmissionGithubResponse;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.entity.SubmissionMetadata;
import com.sealhackathon.api.submissions.repository.SubmissionMetadataRepository;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.service.SubmissionMetadataService;
import com.sealhackathon.api.submissions.support.GitHubApiClient;
import com.sealhackathon.api.submissions.support.GitHubRepoUrlParser;
import com.sealhackathon.api.submissions.value_object.MetadataFetchStatus;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** FR-17 — enqueue + fetch GitHub repo metadata/commits (server PAT). */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionMetadataServiceImpl implements SubmissionMetadataService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionMetadataRepository submissionMetadataRepository;
    private final GitHubApiClient gitHubApiClient;
    private final GitHubProperties gitHubProperties;
    private final CurrentUserAccessor currentUserAccessor;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(
            Instant expiresAt,
            GitHubApiClient.Status status,
            GitHubApiClient.RepoInfo repo,
            List<GitHubApiClient.CommitInfo> commits,
            String message
    ) {
    }

    @Override
    @Transactional
    public void enqueueFetch(Integer submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || !StringUtils.hasText(submission.getRepoUrl())) {
            return;
        }
        if (submissionMetadataRepository.existsById(submissionId)) {
            return;
        }
        SubmissionMetadata meta = SubmissionMetadata.builder()
                .submission(submission)
                .metadataFetchStatus(MetadataFetchStatus.PENDING)
                .build();
        submissionMetadataRepository.save(meta);
        log.debug("[FR-17] Enqueued metadata fetch for submission #{}", submissionId);
    }

    @Override
    @Transactional
    public int processPendingBatch(int limit) {
        if (!gitHubProperties.isEnabled() || !StringUtils.hasText(gitHubProperties.getToken())) {
            return 0;
        }
        List<SubmissionMetadata> pending = submissionMetadataRepository
                .findTop50ByMetadataFetchStatusOrderBySubmissionIdAsc(MetadataFetchStatus.PENDING);
        int processed = 0;
        int max = Math.max(1, Math.min(limit, pending.size()));
        for (int i = 0; i < max; i++) {
            if (fetchAndPersist(pending.get(i).getSubmissionId())) {
                processed++;
            }
        }
        return processed;
    }

    @Override
    @Transactional
    public SubmissionGithubResponse getGithubInfo(Integer submissionId, boolean anonymous) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));

        boolean redact = anonymous || isJudgeAnonymousView();

        if (!gitHubProperties.isEnabled()) {
            return emptyFlags(false, false, false, "DISABLED",
                    "GitHub metadata integration is disabled", null);
        }
        if (!StringUtils.hasText(gitHubProperties.getToken())) {
            return emptyFlags(true, true, false, "DISABLED",
                    "GitHub API token is not configured", null);
        }
        if (!StringUtils.hasText(submission.getRepoUrl())) {
            return emptyFlags(true, false, false, "NONE",
                    "Submission has no repository URL", null);
        }

        var refOpt = GitHubRepoUrlParser.parse(submission.getRepoUrl());
        if (refOpt.isEmpty()) {
            return emptyFlags(true, false, true, "FAILED",
                    "Invalid GitHub repository URL", null);
        }
        var ref = refOpt.get();

        enqueueFetch(submissionId);
        SubmissionMetadata meta = submissionMetadataRepository.findById(submissionId).orElse(null);
        if (meta != null && meta.getMetadataFetchStatus() == MetadataFetchStatus.PENDING) {
            fetchAndPersist(submissionId);
            meta = submissionMetadataRepository.findById(submissionId).orElse(meta);
        }

        CacheEntry cached = getOrFetch(ref);
        if (cached.status() == GitHubApiClient.Status.RATE_LIMITED) {
            return buildFromCache(meta, cached, true, false, redact);
        }
        if (cached.status() == GitHubApiClient.Status.DISABLED) {
            return emptyFlags(true, true, false, "DISABLED", cached.message(), meta);
        }
        if (cached.status() == GitHubApiClient.Status.NOT_FOUND
                || cached.status() == GitHubApiClient.Status.FORBIDDEN) {
            return emptyFlags(true, false, true,
                    meta != null ? meta.getMetadataFetchStatus().name() : "FAILED",
                    cached.message(), meta);
        }
        if (cached.status() != GitHubApiClient.Status.OK) {
            return emptyFlags(true, false, false,
                    meta != null ? meta.getMetadataFetchStatus().name() : "FAILED",
                    cached.message(), meta);
        }
        return buildFromCache(meta, cached, false, false, redact);
    }

    private boolean isJudgeAnonymousView() {
        var user = currentUserAccessor.currentUser();
        return user != null && user.getRole() == UserRole.JUDGE;
    }

    private CacheEntry getOrFetch(GitHubRepoUrlParser.RepoRef ref) {
        String key = ref.fullName().toLowerCase();
        CacheEntry existing = cache.get(key);
        Instant now = Instant.now();
        if (existing != null && existing.expiresAt().isAfter(now)) {
            return existing;
        }

        GitHubApiClient.ApiResult<GitHubApiClient.RepoInfo> repoResult =
                gitHubApiClient.getRepo(ref.owner(), ref.repo());
        if (repoResult.status() != GitHubApiClient.Status.OK) {
            CacheEntry entry = new CacheEntry(
                    now.plusSeconds(Math.max(30, gitHubProperties.getCacheTtlSeconds() / 5)),
                    repoResult.status(),
                    null,
                    List.of(),
                    repoResult.message());
            cache.put(key, entry);
            return entry;
        }

        GitHubApiClient.ApiResult<List<GitHubApiClient.CommitInfo>> commitsResult =
                gitHubApiClient.getCommits(ref.owner(), ref.repo(), gitHubProperties.getCommitLimit());
        if (commitsResult.status() == GitHubApiClient.Status.RATE_LIMITED) {
            CacheEntry entry = new CacheEntry(
                    now.plusSeconds(60),
                    GitHubApiClient.Status.RATE_LIMITED,
                    repoResult.body(),
                    List.of(),
                    commitsResult.message());
            cache.put(key, entry);
            return entry;
        }

        List<GitHubApiClient.CommitInfo> commits =
                commitsResult.status() == GitHubApiClient.Status.OK && commitsResult.body() != null
                        ? commitsResult.body()
                        : List.of();
        CacheEntry entry = new CacheEntry(
                now.plusSeconds(Math.max(30, gitHubProperties.getCacheTtlSeconds())),
                GitHubApiClient.Status.OK,
                repoResult.body(),
                commits,
                null);
        cache.put(key, entry);
        return entry;
    }

    boolean fetchAndPersist(Integer submissionId) {
        SubmissionMetadata meta = submissionMetadataRepository.findById(submissionId).orElse(null);
        if (meta == null) {
            return false;
        }
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || !StringUtils.hasText(submission.getRepoUrl())) {
            meta.setMetadataFetchStatus(MetadataFetchStatus.FAILED);
            meta.setFetchedAt(LocalDateTime.now());
            submissionMetadataRepository.save(meta);
            return true;
        }

        var refOpt = GitHubRepoUrlParser.parse(submission.getRepoUrl());
        if (refOpt.isEmpty()) {
            meta.setMetadataFetchStatus(MetadataFetchStatus.FAILED);
            meta.setFetchedAt(LocalDateTime.now());
            submissionMetadataRepository.save(meta);
            return true;
        }

        CacheEntry cached = getOrFetch(refOpt.get());
        if (cached.status() == GitHubApiClient.Status.RATE_LIMITED
                || cached.status() == GitHubApiClient.Status.DISABLED) {
            // Keep PENDING for later retry
            return false;
        }
        if (cached.status() == GitHubApiClient.Status.OK && cached.repo() != null) {
            GitHubApiClient.RepoInfo repo = cached.repo();
            meta.setRepoName(StringUtils.hasText(repo.fullName()) ? repo.fullName() : repo.name());
            meta.setRepoLanguage(repo.language());
            LocalDateTime lastCommit = repo.pushedAt();
            if (lastCommit == null
                    && cached.commits() != null
                    && !cached.commits().isEmpty()
                    && cached.commits().get(0).date() != null) {
                lastCommit = LocalDateTime.ofInstant(cached.commits().get(0).date(), ZoneOffset.UTC);
            }
            meta.setRepoLastCommitAt(lastCommit);
            meta.setMetadataFetchStatus(MetadataFetchStatus.SUCCESS);
            meta.setFetchedAt(LocalDateTime.now());
            submissionMetadataRepository.save(meta);
            return true;
        }

        meta.setMetadataFetchStatus(MetadataFetchStatus.FAILED);
        meta.setFetchedAt(LocalDateTime.now());
        submissionMetadataRepository.save(meta);
        return true;
    }

    private SubmissionGithubResponse buildFromCache(
            SubmissionMetadata meta,
            CacheEntry cached,
            boolean rateLimited,
            boolean unavailable,
            boolean redact) {
        GitHubApiClient.RepoInfo repo = cached.repo();
        List<SubmissionGithubResponse.CommitItem> commits = new ArrayList<>();
        if (cached.commits() != null) {
            for (GitHubApiClient.CommitInfo c : cached.commits()) {
                commits.add(SubmissionGithubResponse.CommitItem.builder()
                        .sha(c.sha())
                        .message(c.message())
                        .authorName(redact ? null : c.authorName())
                        .authorAvatarUrl(redact ? null : c.authorAvatarUrl())
                        .date(c.date())
                        .htmlUrl(c.htmlUrl())
                        .build());
            }
        }

        String fetchStatus = meta != null && meta.getMetadataFetchStatus() != null
                ? meta.getMetadataFetchStatus().name()
                : (rateLimited ? "PENDING" : "SUCCESS");

        return SubmissionGithubResponse.builder()
                .enabled(true)
                .missingToken(false)
                .rateLimited(rateLimited)
                .unavailable(unavailable)
                .fetchStatus(fetchStatus)
                .message(cached.message())
                .repoName(repo != null ? repo.name() : (meta != null ? meta.getRepoName() : null))
                .repoFullName(repo != null ? repo.fullName() : (meta != null ? meta.getRepoName() : null))
                .language(repo != null ? repo.language() : (meta != null ? meta.getRepoLanguage() : null))
                .description(repo != null ? repo.description() : null)
                .htmlUrl(repo != null ? repo.htmlUrl() : null)
                .stars(repo != null ? repo.stars() : null)
                .lastCommitAt(repo != null && repo.pushedAt() != null
                        ? repo.pushedAt()
                        : (meta != null ? meta.getRepoLastCommitAt() : null))
                .fetchedAt(meta != null ? meta.getFetchedAt() : null)
                .commits(commits)
                .build();
    }

    private SubmissionGithubResponse emptyFlags(
            boolean enabled,
            boolean missingToken,
            boolean unavailable,
            String fetchStatus,
            String message,
            SubmissionMetadata meta) {
        return SubmissionGithubResponse.builder()
                .enabled(enabled)
                .missingToken(missingToken)
                .rateLimited(false)
                .unavailable(unavailable)
                .fetchStatus(fetchStatus)
                .message(message)
                .repoName(meta != null ? meta.getRepoName() : null)
                .language(meta != null ? meta.getRepoLanguage() : null)
                .lastCommitAt(meta != null ? meta.getRepoLastCommitAt() : null)
                .fetchedAt(meta != null ? meta.getFetchedAt() : null)
                .commits(List.of())
                .build();
    }
}
