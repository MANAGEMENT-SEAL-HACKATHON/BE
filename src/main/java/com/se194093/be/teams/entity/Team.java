package com.se194093.be.teams.entity;

import com.se194093.be.chapters.entity.Chapter;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.teams.value_object.TeamStatus;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [BC-05] Team gắn thẳng vào Hackathon (bỏ {@code registration_track_id},
 * {@code assigned_track_id}, {@code assigned_group}).
 *
 * <p>Quan hệ với Track giờ qua bảng trung gian {@code team_round_tracks} (BC-04),
 * mở rộng được cho nhiều mùa và nhiều Round.
 */
@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @Column(name = "team_name", nullable = false, length = 200, unique = true)
    private String teamName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeamStatus status = TeamStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "eliminated_at")
    private LocalDateTime eliminatedAt;

    @Column(name = "elimination_reason", columnDefinition = "TEXT")
    private String eliminationReason;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
