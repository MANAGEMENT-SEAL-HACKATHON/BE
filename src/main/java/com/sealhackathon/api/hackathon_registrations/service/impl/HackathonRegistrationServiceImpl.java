package com.sealhackathon.api.hackathon_registrations.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathon_registrations.entity.HackathonRegistration;
import com.sealhackathon.api.hackathon_registrations.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathon_registrations.service.HackathonRegistrationService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class HackathonRegistrationServiceImpl implements HackathonRegistrationService {

    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final HackathonRepository hackathonRepository;
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    public void register(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        // RÀO CHẮN 1: Hackathon phải đang ở trạng thái ONGOING
        if (hackathon.getStatus() != com.sealhackathon.api.hackathons.value_object.HackathonStatus.ONGOING) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Giải đấu hiện không mở đăng ký.");
        }

        // RÀO CHẮN: CHẶN KHI VƯỢT QUÁ CHỈ TIÊU ĐĂNG KÝ
        if (hackathon.getMaxParticipants() != null) {
            long currentlyRegistered = hackathonRegistrationRepository.countByHackathon_Id(hackathonId);
            if (currentlyRegistered >= hackathon.getMaxParticipants()) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Đăng ký thất bại: Giải đấu đã đạt giới hạn tối đa số lượng người tham gia (%d/%d người)."
                                .formatted(currentlyRegistered, hackathon.getMaxParticipants()));
            }
        }

        // RÀO CHẮN 2: Kiểm tra thời gian đăng ký (Window Validation)
        // Dùng LocalDate thay vì LocalDateTime để đồng bộ với kiểu dữ liệu trong DB
        LocalDate today = java.time.LocalDate.now();

        if (hackathon.getRegistrationStart() != null && today.isBefore(hackathon.getRegistrationStart())) {
            throw new BusinessRuleException(ErrorCode.REGISTRATION_CLOSED, "Chưa đến thời gian mở đăng ký.");
        }
        if (hackathon.getRegistrationEnd() != null && today.isAfter(hackathon.getRegistrationEnd())) {
            throw new BusinessRuleException(ErrorCode.REGISTRATION_CLOSED, "Thời gian đăng ký đã kết thúc.");
        }

        // RÀO CHẮN 3: Chống đăng ký trùng lặp
        if (hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathonId, userId)) {
            throw new ConflictException(ErrorCode.INVALID_STATE, "Bạn đã đăng ký giải đấu này rồi.");
        }

        User user = userRepository.findById(userId).orElseThrow();

        hackathonRegistrationRepository.save(HackathonRegistration.builder()
                .hackathon(hackathon)
                .user(user)
                .build());
    }

    @Override
    public void unregister(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();

        // RÀO CHẮN 1: Nếu sinh viên đang ở trong một Đội (Team) thì KHÔNG được hủy đăng ký giải
        if (teamMemberRepository.isUserInAnyActiveTeamForHackathon(userId, hackathonId)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Bạn phải rời Đội thi hiện tại trước khi hủy đăng ký giải đấu.");
        }

        hackathonRegistrationRepository.deleteByHackathon_IdAndUser_Id(hackathonId, userId);
    }
}