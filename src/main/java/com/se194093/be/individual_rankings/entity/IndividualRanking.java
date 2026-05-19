package com.se194093.be.individual_rankings.entity;

import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Xếp hạng cá nhân trong 1 Hackathon (đối với mùa cá nhân).
 *
 * <p>UNIQUE ({@code hackathon_id}, {@code user_id}).
 */
@Entity
@Table(
        name = "individual_rankings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ir_hackathon_user",
                        columnNames = {"hackathon_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndividualRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "score_this_hackathon", nullable = false)
    private Float scoreThisHackathon = 0f;

    @Builder.Default
    @Column(name = "cumulative_score", nullable = false)
    private Float cumulativeScore = 0f;

    @Column(name = "`rank`")
    private Integer rank;

    @Builder.Default
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Builder.Default
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
