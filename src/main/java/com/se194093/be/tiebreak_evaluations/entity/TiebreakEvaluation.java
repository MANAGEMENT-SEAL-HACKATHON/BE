package com.se194093.be.tiebreak_evaluations.entity;

import com.se194093.be.rounds.entity.Round;
import com.se194093.be.teams.entity.Team;
import com.se194093.be.users.entity.User;
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

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt = LocalDateTime.now();
}
