package com.se194093.be.submissions.entity;

import com.se194093.be.rounds.entity.Round;
import com.se194093.be.submissions.value_object.SubmissionStatus;
import com.se194093.be.teams.entity.Team;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [BC-06] Submission dùng XOR FK — Track (Sơ loại) HOẶC Round (Chung kết).
 *
 * <p>Partial UNIQUE mô phỏng bằng generated columns {@code track_uk}, {@code round_uk}
 * ở DB layer (KHÔNG khai báo trong entity).
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

    /**
     * [BC-06] FK XOR — Track (Sơ loại). Nullable=true.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    /**
     * [BC-06] FK XOR — Round (Chung kết). Nullable=true.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
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
