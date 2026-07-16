package com.sealhackathon.api.rounds.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.tracks.entity.Track;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [BC-01] Round là CON TRỰC TIẾP của Hackathon (đảo FK so với v2.1).
 *
 * <p>Mỗi Hackathon có ≥ 1 Round. Round Sơ loại / Bán kết có Track con,
 * Round Chung kết ({@code isFinal = TRUE}) KHÔNG có Track con — Submission/Judge/Criteria
 * gắn trực tiếp qua {@code round_id}.
 *
 * <p>Xem chi tiết: {@code docs/db/schema-v3.0-mysql.md} §2 (rounds).
 */
@Entity
@Table(name = "rounds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // [BC-01] FK đảo: track_id -> hackathon_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    /**
     * Ví dụ: "Vòng Sơ loại", "Vòng Chung kết".
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Ngày và giờ thi (dùng sắp xếp trình tự vòng trong hackathon; khác deadline nộp bài).
     * Nullable trên DB cũ; seed dev / Hibernate ddl-auto backfill trước khi enforce NOT NULL.
     */
    @Column(name = "exam_at", nullable = true)
    private LocalDateTime examAt;

    /**
     * [BC-01] TRUE = Round Chung kết — KHÔNG có Track con.
     * Submission/Criteria/JudgeAssignment gắn trực tiếp qua {@code round_id}.
     */
    @Builder.Default
    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    /**
     * [BC-01] Phân loại Round — phải nhất quán với {@link #isFinal}
     * (DB CHECK constraint enforce).
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false, length = 20)
    private RoundType roundType = RoundType.PRELIMINARY;

    @Column(name = "coding_duration_hours")
    private Integer codingDurationHours;

    @Column(name = "submission_open")
    private LocalDateTime submissionOpen;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDateTime submissionDeadline;

    /**
     * [BC-01] Chính sách nộp bài trễ.
     * Sơ loại: {@code ALLOW_LATE_PENDING}. Chung kết: {@code HARD_LOCK}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "late_submission_policy", nullable = false, length = 20)
    private LateSubmissionPolicy lateSubmissionPolicy = LateSubmissionPolicy.ALLOW_LATE_PENDING;

    @Column(name = "problem_statement_url", columnDefinition = "TEXT")
    private String problemStatementUrl;

    @Column(name = "problem_statement_storage_key", length = 512)
    private String problemStatementStorageKey;

    @Column(name = "problem_statement_original_filename", length = 255)
    private String problemStatementOriginalFilename;

    @Column(name = "problem_released_at")
    private LocalDateTime problemReleasedAt;

    /**
     * CK-05: thời điểm migration xóa PDF đề riêng trên vòng Chung kết (null = chưa clear / không áp dụng).
     * Banner coord hiện khi field này khác null và {@link #finalProblemMigrationBannerDismissedAt} null.
     */
    @Column(name = "final_problem_migration_cleared_at")
    private LocalDateTime finalProblemMigrationClearedAt;

    /**
     * CK-05: coordinator đã dismiss banner migration đề CK (một lần theo round, không per-user).
     */
    @Column(name = "final_problem_migration_banner_dismissed_at")
    private LocalDateTime finalProblemMigrationBannerDismissedAt;

    /**
     * Số đội Top N MỖI BẢNG (assigned_group) vào Round tiếp.
     * NULL khi {@code isFinal = TRUE}.
     */
    @Column(name = "top_n_advance")
    private Integer topNAdvance;

    /**
     * Số đội tối đa dự kiến vào Chung kết — kích hoạt Wild Card nếu thiếu so với thực tế.
     */
    @Column(name = "min_teams_final")
    private Integer minTeamsFinal;

    /**
     * Per-round flag — runtime WC pool chỉ check field này (không AND hackathon).
     */
    @Builder.Default
    @Column(name = "wildcard_enabled", nullable = false)
    private Boolean wildcardEnabled = false;

    /**
     * Plan C — thời điểm Coordinator xác nhận đề xuất vé vớt (LOCKED).
     * NULL = chưa xác nhận; sau lock chỉ sửa qua Override.
     */
    @Column(name = "wildcard_proposal_confirmed_at")
    private LocalDateTime wildcardProposalConfirmedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "tiebreak_rule", length = 50)
    private TiebreakRule tiebreakRule = TiebreakRule.COORDINATOR_DECISION;

    /**
     * Thời điểm kích hoạt Round lần cuối (FR-15/25). NULL khi chưa từng activate.
     */
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Builder.Default
    @Column(name = "scoring_locked", nullable = false)
    private Boolean scoringLocked = false;

    @Column(name = "scoring_locked_at")
    private LocalDateTime scoringLockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scoring_locked_by")
    private User scoringLockedBy;

    @Builder.Default
    @Column(name = "force_locked", nullable = false)
    private Boolean forceLocked = false;

    @Column(name = "force_lock_reason", columnDefinition = "TEXT")
    private String forceLockReason;

    /** FR-24 — công bố kết quả Sơ loại; gate trước kích hoạt Chung kết (FR-25). */
    @Builder.Default
    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Builder.Default
    @Column(name = "default_presentation_minutes", nullable = false)
    private Integer defaultPresentationMinutes = 10;

    @Builder.Default
    @Column(name = "default_qa_minutes", nullable = false)
    private Integer defaultQaMinutes = 5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controller_judge_id")
    private User controllerJudge;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;

    /** Set when the submission-deadline reminder (student + judge) was sent (idempotent scheduler). */
    @Column(name = "deadline_reminder_sent_at")
    private LocalDateTime deadlineReminderSentAt;

    /**
     * Set when Coordinator ends exam / closes submission early (idempotent).
     * Clamps {@code submissionDeadline} and {@code examAt} to that moment so phase becomes JUDGING.
     */
    @Column(name = "submission_closed_early_at")
    private LocalDateTime submissionClosedEarlyAt;

    /**
     * Chung kết: đã gọi shuffle hàng đợi ít nhất một lần (kể cả 0 slot / 0 gradable).
     * Sơ loại dùng {@code tracks.presentation_shuffled}.
     */
    @Builder.Default
    @Column(name = "presentation_shuffled", nullable = false)
    private Boolean presentationShuffled = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * [BC-01 — back-compat shim]
     *
     * <p>Round không còn FK tới Track ở v3.0 (đảo chiều: Track → Round).
     * Method này giữ lại tạm thời để service-layer/mapper cũ vẫn compile
     * mà không phải sửa ngay. Luôn trả {@code null}; service rewrite sau sẽ bỏ.
     *
     * @deprecated dùng {@code track.getRound()} (đảo chiều) thay cho {@code round.getTrack()}.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    @Transient
    public Track getTrack() {
        return null;
    }
}
