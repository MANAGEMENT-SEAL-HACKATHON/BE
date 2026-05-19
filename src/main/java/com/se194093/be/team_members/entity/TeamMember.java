package com.se194093.be.team_members.entity;

import com.se194093.be.team_members.value_object.TeamMemberRole;
import com.se194093.be.team_members.value_object.TeamMemberStatus;
import com.se194093.be.teams.entity.Team;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bảng quan hệ N-N giữa Team và User. Composite PK ({@code team_id}, {@code user_id}).
 *
 * <p>Trigger {@code trg_lock_member_insert/update} ({@code docs/db/schema-v3.0-mysql.md} §5.2)
 * chặn thay đổi khi đội đã {@code is_locked = TRUE}.
 */
@Entity
@Table(name = "team_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @EmbeddedId
    private TeamMemberId id;

    @MapsId("teamId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_team", nullable = false, length = 20)
    private TeamMemberRole roleInTeam = TeamMemberRole.MEMBER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeamMemberStatus status = TeamMemberStatus.PENDING;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;
}
