package com.se194093.be.prizes.entity;

import com.se194093.be.prizes.value_object.PrizeRank;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.teams.entity.Team;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Giải thưởng cấp cho 1 Team trong 1 Round (và optional Track).
 */
@Entity
@Table(name = "prizes")
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
