package com.se194093.be.tracks.entity;

import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.tracks.value_object.TrackStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tracks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_teams")
    private Integer maxTeams;

    /**
     * [FIX-02]
     * Số đội tối đa mỗi bảng đấu
     * VD:
     * - Fall 2025 = 6
     * - Spring 2026 = 8
     */
    @Column(name = "max_teams_per_group")
    private Integer maxTeamsPerGroup;

    @Column(name = "min_team_size", nullable = false)
    private Integer minTeamSize = 3;

    @Column(name = "max_team_size", nullable = false)
    private Integer maxTeamSize = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TrackStatus status = TrackStatus.OPEN;

}
