package com.sealhackathon.api.teams.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.Team;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [MF-02 v3.5] Đội tham gia một Round — tạo TRƯỚC {@code team_round_tracks} khi bốc thăm.
 */
@Entity
@Table(
        name = "team_round_participation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trp_team_round",
                columnNames = {"team_id", "round_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRoundParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
