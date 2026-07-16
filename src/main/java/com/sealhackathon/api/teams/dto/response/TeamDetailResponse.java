package com.sealhackathon.api.teams.dto.response;

import com.sealhackathon.api.teams.value_object.TeamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Chi tiết đội cho Leader / Coordinator / FE dashboard. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDetailResponse {

    private Integer id;
    private Integer hackathonId;
    private String hackathonName;
    private String teamName;
    private Integer leaderId;
    private String leaderName;
    private Integer chapterId;
    private TeamStatus status;
    private Boolean isLocked;
    private LocalDateTime lockedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private int acceptedMemberCount;
    private int pendingInviteCount;
    private LocalDateTime formationSubmittedAt;
    private LocalDateTime formationGraceDeadlineAt;
    private List<TeamMemberResponse> members;
    private Integer trackId;
    private String trackName;
    private String assignedGroup;
    /** TRT participationStatus (PARTICIPATING | ADVANCED | ELIMINATED) — alias lotteryStatus for FE. */
    private String lotteryStatus;
    private Integer minTeamSize;
    private Integer maxTeamSize;
    /** True when team has an active mentor assignment — FE gates disband on this. */
    private Boolean hasMentor;
}