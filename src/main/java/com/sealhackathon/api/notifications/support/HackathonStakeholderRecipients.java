package com.sealhackathon.api.notifications.support;

import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.entity.MentorTeamAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects unique stakeholder {@link User}s for a hackathon (students, mentors, judges, coordinators).
 */
@Component
@RequiredArgsConstructor
public class HackathonStakeholderRecipients {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final UserRepository userRepository;

    public List<User> collect(Integer hackathonId) {
        if (hackathonId == null) {
            return List.of();
        }
        Map<Integer, User> byId = new LinkedHashMap<>();

        List<Team> teams = teamRepository.findByHackathon_IdAndStatus(hackathonId, TeamStatus.ACTIVE);
        if (!teams.isEmpty()) {
            List<Integer> teamIds = teams.stream().map(Team::getId).toList();
            for (TeamMember m : teamMemberRepository.findByTeam_IdIn(teamIds)) {
                if (m.getStatus() == TeamMemberStatus.ACCEPTED && m.getUser() != null) {
                    byId.putIfAbsent(m.getUser().getId(), m.getUser());
                }
            }
        }

        for (MentorTeamAssignment mta : mentorTeamAssignmentRepository.findByHackathon_Id(hackathonId)) {
            if (mta.getMentor() != null) {
                byId.putIfAbsent(mta.getMentor().getId(), mta.getMentor());
            }
        }
        for (MentorAssignment ma : mentorAssignmentRepository.findByTrack_Round_Hackathon_Id(hackathonId)) {
            if (ma.getMentor() != null) {
                byId.putIfAbsent(ma.getMentor().getId(), ma.getMentor());
            }
        }

        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId);
        List<Integer> roundIds = rounds.stream().map(Round::getId).toList();
        if (!roundIds.isEmpty()) {
            for (JudgeAssignment ja : judgeAssignmentRepository.findByRound_IdIn(roundIds)) {
                if (ja.getJudge() != null) {
                    byId.putIfAbsent(ja.getJudge().getId(), ja.getJudge());
                }
            }
        }
        List<Track> tracks = trackRepository.findByHackathonIdOrderById(hackathonId);
        List<Integer> trackIds = tracks.stream().map(Track::getId).toList();
        if (!trackIds.isEmpty()) {
            for (JudgeAssignment ja : judgeAssignmentRepository.findByTrack_IdIn(trackIds)) {
                if (ja.getJudge() != null) {
                    byId.putIfAbsent(ja.getJudge().getId(), ja.getJudge());
                }
            }
        }

        try {
            for (User coord : userRepository
                    .findByRoleAndStatus(UserRole.COORDINATOR, UserStatus.APPROVED, Pageable.unpaged())
                    .getContent()) {
                if (coord != null && coord.getId() != null) {
                    byId.putIfAbsent(coord.getId(), coord);
                }
            }
        } catch (Exception ignored) {
            // optional fan-out
        }

        return new ArrayList<>(byId.values());
    }
}
