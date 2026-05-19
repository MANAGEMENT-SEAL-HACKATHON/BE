package com.sealhackathon.api.team_round_tracks.entity;

import com.sealhackathon.api.team_round_tracks.value_object.RegistrationType;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [BC-04] Bảng quan hệ giữa Team ↔ Track (mở rộng được cho nhiều Round/mùa).
 *
 * <p>UNIQUE ({@code team_id}, {@code track_id}). Trigger
 * {@code trg_check_team_track_same_hackathon} ({@code docs/db/schema-v3.0-mysql.md} §5.9)
 * chặn:
 * <ul>
 *   <li>Team và Track khác Hackathon.</li>
 *   <li>Track thuộc Round FINAL — TKĐD không gán team vào Track FINAL được.</li>
 * </ul>
 */
@Entity
@Table(
        name = "team_round_tracks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trt_team_track",
                        columnNames = {"team_id", "track_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRoundTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    /**
     * Bảng đấu của đội trong Track (vd: "Bảng A", "Group 3").
     * NULL khi Track không chia bảng.
     */
    @Column(name = "assigned_group", length = 50)
    private String assignedGroup;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false, length = 20)
    private RegistrationType registrationType = RegistrationType.ASSIGNED;

    @Builder.Default
    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;
}
