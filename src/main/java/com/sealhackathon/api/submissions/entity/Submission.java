package com.sealhackathon.api.submissions.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [BC-06] Submission — Sơ loại gắn {@code track_id}; Chung kết gắn {@code round_id} (FINAL).
 *
 * <p>UNIQUE {@code (team_id, scoring_key)} — generated column {@code scoring_key} ở DB (BUG-2).
 * Sơ loại: {@code T{track_id}}; Chung kết: {@code R{round_id}}.
 *
 * <p>Trigger {@code trg_check_submission_round_is_final} ({@code docs/db/schema-v3.0-mysql.md} §5.7):
 * <ul>
 *   <li>Nếu {@code track_id = NULL} thì {@code round_id} phải trỏ Round FINAL.</li>
 *   <li>Final HARD_LOCK ⇒ không cho phép trạng thái {@code LATE_PENDING}.</li>
 * </ul>
 */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    /**
     * [BC-06] FK — Track (Sơ loại). Nullable khi Chung kết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    /**
     * [BC-06] FK — Round (denormalized từ track hoặc FINAL). NOT NULL sau v4.1 migration.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(name = "repo_url", columnDefinition = "TEXT")
    private String repoUrl;

    @Column(name = "demo_url", columnDefinition = "TEXT")
    private String demoUrl;

    @Column(name = "report_url", columnDefinition = "TEXT")
    private String reportUrl;

    @Column(name = "slide_url", columnDefinition = "TEXT")
    private String slideUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @Builder.Default
    @Column(name = "is_late", nullable = false)
    private Boolean isLate = false;

    @Column(name = "late_reason", columnDefinition = "TEXT")
    private String lateReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Builder.Default
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();
}
