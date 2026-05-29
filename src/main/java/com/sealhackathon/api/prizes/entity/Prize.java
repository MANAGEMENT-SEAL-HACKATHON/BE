package com.sealhackathon.api.prizes.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Giải thưởng cấp cho 1 Team trong 1 Round (và optional Track).
 */
@Entity
@Table(
        name = "prizes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_prizes_hackathon_team", columnNames = {"hackathon_id", "team_id"}),
                @UniqueConstraint(name = "uk_prizes_hackathon_rank", columnNames = {"hackathon_id", "prize_rank"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "prize_name", nullable = false, length = 200)
    private String prizeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "prize_rank", length = 50)
    private PrizeRank prizeRank;

    @Column(name = "prize_value", length = 300)
    private String prizeValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_by")
    private User awardedBy;
}
