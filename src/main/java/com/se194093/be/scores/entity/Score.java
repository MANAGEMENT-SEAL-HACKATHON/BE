package com.se194093.be.scores.entity;

import com.se194093.be.calibration_sessions.entity.CalibrationSession;
import com.se194093.be.criteria.entity.Criteria;
import com.se194093.be.scores.value_object.ScoreType;
import com.se194093.be.submissions.entity.Submission;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Điểm Judge chấm cho 1 Submission theo 1 Criterion.
 *
 * <p>UNIQUE ({@code submission_id}, {@code judge_id}, {@code criterion_id}, {@code score_type}).
 *
 * <p>Trigger {@code trg_lock_score_insert/update} ({@code docs/db/schema-v3.0-mysql.md} §5.1)
 * chặn ghi khi Round chứa Submission đã {@code scoring_locked = TRUE}.
 */
@Entity
@Table(
        name = "scores",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_scores_subm_judge_crit_type",
                        columnNames = {"submission_id", "judge_id", "criterion_id", "score_type"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private Criteria criterion;

    @Column(name = "score_value", nullable = false)
    private Float scoreValue;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "score_type", nullable = false, length = 20)
    private ScoreType scoreType = ScoreType.NORMAL;

    @Builder.Default
    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calibration_session_id")
    private CalibrationSession calibrationSession;

    @Builder.Default
    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
