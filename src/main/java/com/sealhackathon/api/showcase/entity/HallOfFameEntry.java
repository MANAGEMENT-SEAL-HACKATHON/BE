package com.sealhackathon.api.showcase.entity;

import com.sealhackathon.api.hackathons.value_object.Season;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Snapshot quán quân (giải FIRST) khi hackathon FINISHED.
 * Ghi trực tiếp — không đi qua {@code HackathonArchiveGuard}.
 */
@Entity
@Table(
        name = "hall_of_fame_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_hof_hackathon", columnNames = "hackathon_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallOfFameEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "hackathon_id", nullable = false)
    private Integer hackathonId;

    @Column(name = "hackathon_name", nullable = false, length = 300)
    private String hackathonName;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "season", nullable = false, length = 20)
    private Season season;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "team_name", nullable = false, length = 200)
    private String teamName;

    @Column(name = "member_names", columnDefinition = "TEXT")
    private String memberNames;

    @Column(name = "track_name", length = 200)
    private String trackName;

    @Column(name = "prize_name", length = 200)
    private String prizeName;

    @Column(name = "prize_value", length = 300)
    private String prizeValue;

    @Column(name = "awarded_at")
    private LocalDateTime awardedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
