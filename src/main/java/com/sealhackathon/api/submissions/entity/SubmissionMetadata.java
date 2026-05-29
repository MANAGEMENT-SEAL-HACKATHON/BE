package com.sealhackathon.api.submissions.entity;

import com.sealhackathon.api.submissions.value_object.MetadataFetchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** FR-17 — metadata repo async (tùy chọn). */
@Entity
@Table(name = "submission_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionMetadata {

    @Id
    @Column(name = "submission_id")
    private Integer submissionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @Column(name = "repo_name", length = 255)
    private String repoName;

    @Column(name = "repo_language", length = 100)
    private String repoLanguage;

    @Column(name = "repo_last_commit_at")
    private LocalDateTime repoLastCommitAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "metadata_fetch_status", nullable = false, length = 20)
    private MetadataFetchStatus metadataFetchStatus = MetadataFetchStatus.PENDING;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;
}
