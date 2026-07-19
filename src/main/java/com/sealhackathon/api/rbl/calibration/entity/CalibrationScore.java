package com.sealhackathon.api.rbl.calibration.entity;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rbl_calibration_scores", uniqueConstraints = @UniqueConstraint(
        name = "uk_calibration_score", columnNames = {"prompt_id", "judge_id", "criterion_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private CalibrationPrompt prompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private Criteria criterion;

    @Column(name = "score_value", nullable = false)
    private Float scoreValue;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Builder.Default
    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt = LocalDateTime.now();
}
