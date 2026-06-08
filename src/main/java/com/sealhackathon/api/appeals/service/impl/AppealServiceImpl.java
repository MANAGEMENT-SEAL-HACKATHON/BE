package com.sealhackathon.api.appeals.service.impl;

import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.appeals.repository.AppealRepository;
import com.sealhackathon.api.appeals.service.AppealService;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;
import com.sealhackathon.api.me.support.StudentAccessGuard;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AppealServiceImpl implements AppealService {

    private final AppealRepository appealRepository;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final StudentAccessGuard studentAccessGuard;
    private final CurrentUserAccessor currentUserAccessor;
    private final UserRepository userRepository;

    @Override
    public AppealResponse create(CreateAppealRequest request) {
        // RÀO CHẮN 1: CHỈ CÓ NHÓM TRƯỞNG MỚI ĐƯỢC GỬI KHIẾU NẠI
        studentAccessGuard.assertTeamLeader(request.getTeamId());

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new com.sealhackathon.api.common.exception.ResourceNotFoundException("Team", request.getTeamId()));

        // RÀO CHẮN 2: Chỉ cho phép khiếu nại khi đội ở trạng thái ELIMINATED (Bị loại thủ công)
        if (team.getStatus() != com.sealhackathon.api.teams.value_object.TeamStatus.ELIMINATED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Đội của bạn không nằm trong diện bị loại thủ công để có thể khiếu nại.");
        }

        // RÀO CHẮN 3: Window 24h - Quá 24h kể từ lúc bị loại là chặn đứng
        if (team.getEliminatedAt() == null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Không tìm thấy thời gian đội bị loại.");
        }
        if (LocalDateTime.now().isAfter(team.getEliminatedAt().plusHours(24))) {
            throw new BusinessRuleException("APPEAL_DEADLINE_EXPIRED", "Đã quá thời hạn 24h để gửi đơn khiếu nại.");
        }

        // RÀO CHẮN 4: Mỗi Vòng chỉ được khiếu nại 1 lần
        if (appealRepository.existsByTeam_IdAndRound_Id(team.getId(), request.getRoundId())) {
            throw new ConflictException(ErrorCode.INVALID_STATE, "Đội của bạn đã gửi đơn khiếu nại cho Vòng thi này rồi.");
        }

        var round = roundRepository.findById(request.getRoundId()).orElseThrow();
        var submitter = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();

        Appeal saved = appealRepository.save(Appeal.builder()
                .team(team)
                .round(round)
                .submittedBy(submitter)
                .reason(request.getReason())
                .evidenceUrl(request.getEvidenceUrl())
                .status(AppealStatus.PENDING)
                .build());

        return AppealResponse.builder()
                .id(saved.getId())
                .teamId(saved.getTeam().getId())
                .roundId(saved.getRound().getId())
                .reason(saved.getReason())
                .evidenceUrl(saved.getEvidenceUrl())
                .status(saved.getStatus())
                .build();
    }
}