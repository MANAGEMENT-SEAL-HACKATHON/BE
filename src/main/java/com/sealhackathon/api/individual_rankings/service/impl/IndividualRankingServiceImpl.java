package com.sealhackathon.api.individual_rankings.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.query.FinalRankingQueryService;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.individual_rankings.dto.response.IndividualRankingItemResponse;
import com.sealhackathon.api.individual_rankings.entity.IndividualRanking;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
import com.sealhackathon.api.individual_rankings.service.IndividualRankingService;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IndividualRankingServiceImpl implements IndividualRankingService {

    private final IndividualRankingRepository individualRankingRepository;
    private final HackathonRepository hackathonRepository;
    private final FinalRankingQueryService finalRankingQueryService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<IndividualRankingItemResponse> listByHackathon(Integer hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        if (!Boolean.TRUE.equals(hackathon.getIndividualRankingEnabled())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Hackathon này không kích hoạt chế độ Xếp hạng Cá nhân.");
        }

        if (hackathon.getStatus() != HackathonStatus.FINISHED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Bảng xếp hạng Cá nhân chỉ được công bố khi Hackathon đã kết thúc.");
        }

        return individualRankingRepository.findByHackathon_IdOrderByRankAsc(hackathonId)
                .stream()
                .map(ir -> IndividualRankingItemResponse.builder()
                        .userId(ir.getUser().getId())
                        .fullName(ir.getUser().getFullName())
                        .scoreThisHackathon(ir.getScoreThisHackathon())
                        .cumulativeScore(ir.getCumulativeScore())
                        .rank(ir.getRank())
                        .build())
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calculateAsync(Integer hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        if (!Boolean.TRUE.equals(hackathon.getIndividualRankingEnabled())) {
            log.info("[IndividualRanking] hackathonId={} — cờ tắt, bỏ qua", hackathonId);
            return;
        }

        individualRankingRepository.deleteByHackathon_Id(hackathonId);

        List<FinalTeamRankingItemResponse> teamRankings =
                finalRankingQueryService.teamRankingsForHackathon(hackathonId);
        if (teamRankings.isEmpty()) {
            log.info("[IndividualRanking] hackathonId={} — không có team ranking CK, bỏ qua", hackathonId);
            return;
        }

        Map<Integer, Float> scoreByUser = new HashMap<>();
        for (FinalTeamRankingItemResponse item : teamRankings) {
            Team team = teamRepository.findById(item.getTeamId()).orElse(null);
            if (team == null) {
                continue;
            }
            float teamScore = item.getWeightedAvgScore() != null ? item.getWeightedAvgScore().floatValue() : 0f;
            List<TeamMember> members = teamMemberRepository.findByTeam_Id(team.getId()).stream()
                    .filter(tm -> tm.getStatus() == TeamMemberStatus.ACCEPTED)
                    .toList();
            if (members.isEmpty()) {
                continue;
            }
            float share = teamScore / members.size();
            for (TeamMember member : members) {
                User user = member.getUser();
                scoreByUser.merge(user.getId(), share, Float::sum);
            }
        }

        if (scoreByUser.isEmpty()) {
            return;
        }

        List<Map.Entry<Integer, Float>> sorted = scoreByUser.entrySet().stream()
                .sorted(Map.Entry.<Integer, Float>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        List<IndividualRanking> rows = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Integer, Float> entry : sorted) {
            User user = userRepository.findById(entry.getKey()).orElse(null);
            if (user == null) {
                continue;
            }
            float score = entry.getValue();
            rows.add(IndividualRanking.builder()
                    .hackathon(hackathon)
                    .user(user)
                    .scoreThisHackathon(score)
                    .cumulativeScore(score)
                    .rank(rank++)
                    .isEnabled(true)
                    .calculatedAt(now)
                    .build());
        }
        individualRankingRepository.saveAll(rows);
        log.info("[IndividualRanking] hackathonId={} — persisted {} rows", hackathonId, rows.size());
    }
}
