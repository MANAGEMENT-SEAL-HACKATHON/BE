package com.sealhackathon.api.submissions.service.impl;

import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.GitHubProperties;
import com.sealhackathon.api.submissions.dto.response.SubmissionGithubResponse;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionMetadataRepository;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.support.GitHubApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionMetadataServiceImplTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock SubmissionMetadataRepository submissionMetadataRepository;
    @Mock GitHubApiClient gitHubApiClient;
    @Mock CurrentUserAccessor currentUserAccessor;

    private GitHubProperties properties;
    private SubmissionMetadataServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new GitHubProperties();
        properties.setEnabled(false);
        properties.setToken("");
        properties.setCommitLimit(30);
        properties.setCacheTtlSeconds(300);
        service = new SubmissionMetadataServiceImpl(
                submissionRepository,
                submissionMetadataRepository,
                gitHubApiClient,
                properties,
                currentUserAccessor);
    }

    @Test
    void disabledModeReturnsEmptyPayloadWithoutCallingGithub() {
        Submission submission = Submission.builder()
                .id(42)
                .repoUrl("https://github.com/octocat/Hello-World")
                .build();
        when(submissionRepository.findById(42)).thenReturn(Optional.of(submission));

        SubmissionGithubResponse response = service.getGithubInfo(42, false);

        assertThat(response.isEnabled()).isFalse();
        assertThat(response.getFetchStatus()).isEqualTo("DISABLED");
        assertThat(response.getCommits()).isEmpty();
        assertThat(response.isRateLimited()).isFalse();
        verify(gitHubApiClient, never()).getRepo(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void missingTokenReturnsEmptyPayloadWithFlag() {
        properties.setEnabled(true);
        properties.setToken("  ");
        Submission submission = Submission.builder()
                .id(7)
                .repoUrl("https://github.com/octocat/Hello-World")
                .build();
        when(submissionRepository.findById(7)).thenReturn(Optional.of(submission));

        SubmissionGithubResponse response = service.getGithubInfo(7, false);

        assertThat(response.isEnabled()).isTrue();
        assertThat(response.isMissingToken()).isTrue();
        assertThat(response.getFetchStatus()).isEqualTo("DISABLED");
        assertThat(response.getCommits()).isEmpty();
        verify(gitHubApiClient, never()).getRepo(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
