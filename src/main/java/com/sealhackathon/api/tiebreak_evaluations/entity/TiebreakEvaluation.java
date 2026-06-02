package com.sealhackathon.api.tiebreak_evaluations.entity;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Đánh giá điểm trừ tiebreak — mỗi Judge cho mỗi Team trong mỗi Round.
 *
 * <p>UNIQUE ({@code round_id}, {@code team_id}, {@code judge_id}).
 */
@Entity
@Table(
        name = "tiebreak_evaluations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_te_round_team_judge",
                        columnNames = {"round_id", "team_id", "judge_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TiebreakEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    @Builder.Default
    @Column(name = "penalty_score", nullable = false)
    private Float penaltyScore = 0f;

    // BỔ SUNG 2 TRƯỜNG THEO ĐÁP ÁN PENDING #1 (MF-04 v4.1)
    @Builder.Default
    @Column(name = "is_casting_vote", nullable = false)
    private Boolean isCastingVote = false;

    @Builder.Default
    @Column(name = "tiebreak_level", nullable = false)
    private Integer tiebreakLevel = 1;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt = LocalDateTime.now();
}