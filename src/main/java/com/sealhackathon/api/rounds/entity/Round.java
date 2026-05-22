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
     * DB có thể nullable tạm khi migrate; {@link com.sealhackathon.api.config.RoundExamAtSchemaMigration} backfill + NOT NULL.
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

    @Column(name = "problem_released_at")
    private LocalDateTime problemReleasedAt;

    /**
     * Số đội Top N MỖI BẢNG (assigned_group) vào Round tiếp.
     * NULL khi {@code isFinal = TRUE}.
     */
    @Column(name = "top_n_advance")
    private Integer topNAdvance;

    /**
     * [FIX-03] Số đội tối thiểu vào Round tiếp — kích hoạt Wild Card nếu thiếu.
     */
    @Column(name = "min_teams_final")
    private Integer minTeamsFinal;

    /**
     * Per-round override. Chỉ có hiệu lực nếu {@code hackathon.wildcardEnabled = TRUE}.
     */
    @Builder.Default
    @Column(name = "wildcard_enabled", nullable = false)
    private Boolean wildcardEnabled = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "tiebreak_rule", length = 50)
    private TiebreakRule tiebreakRule = TiebreakRule.PENALTY_SCORE;

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
