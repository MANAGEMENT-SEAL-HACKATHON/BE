package com.sealhackathon.api.submissions.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionGithubResponse {

    /** app.github.enabled */
    private boolean enabled;
    /** Missing or blank server PAT */
    private boolean missingToken;
    /** GitHub rate limit hit */
    private boolean rateLimited;
    /** Repo not found / private / forbidden */
    private boolean unavailable;
    /** PENDING | SUCCESS | FAILED | DISABLED | NONE */
    private String fetchStatus;
    private String message;

    private String repoName;
    private String repoFullName;
    private String language;
    private String description;
    private String htmlUrl;
    private Integer stars;
    private LocalDateTime lastCommitAt;
    private LocalDateTime fetchedAt;

    private List<CommitItem> commits;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommitItem {
        private String sha;
        private String message;
        private String authorName;
        private String authorAvatarUrl;
        private Instant date;
        private String htmlUrl;
    }
}
