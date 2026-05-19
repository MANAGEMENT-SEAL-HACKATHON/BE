package com.sealhackathon.api.chapter_rankings.entity;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Xếp hạng Chapter trong 1 Hackathon.
 *
 * <p>UNIQUE ({@code hackathon_id}, {@code chapter_id}).
 */
@Entity
@Table(
        name = "chapter_rankings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cr_hackathon_chapter",
                        columnNames = {"hackathon_id", "chapter_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Builder.Default
    @Column(name = "best_team_score", nullable = false)
    private Float bestTeamScore = 0f;

    @Builder.Default
    @Column(name = "total_score", nullable = false)
    private Float totalScore = 0f;

    @Column(name = "`rank`")
    private Integer rank;

    @Builder.Default
    @Column(name = "teams_participated", nullable = false)
    private Integer teamsParticipated = 0;

    @Builder.Default
    @Column(name = "prizes_won", nullable = false)
    private Integer prizesWon = 0;

    @Column(name = "formula_snapshot", columnDefinition = "TEXT")
    private String formulaSnapshot;

    @Builder.Default
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
