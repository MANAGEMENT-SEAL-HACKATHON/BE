package com.sealhackathon.api.hackathon_registrations.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathon_registrations.entity.HackathonRegistration;
import com.sealhackathon.api.hackathon_registrations.entity.HackathonRegistrationWithdrawal;
import com.sealhackathon.api.hackathon_registrations.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathon_registrations.repository.HackathonRegistrationWithdrawalRepository;
import com.sealhackathon.api.hackathon_registrations.service.HackathonRegistrationService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.hackathons.support.HackathonRegistrationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class HackathonRegistrationServiceImpl implements HackathonRegistrationService {

    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final HackathonRegistrationWithdrawalRepository hackathonRegistrationWithdrawalRepository;
    private final HackathonRepository hackathonRepository;
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    public void register(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Giải đấu hiện không mở đăng ký.");
        }

        if (hackathonRegistrationWithdrawalRepository.existsByHackathon_IdAndUser_Id(hackathonId, userId)) {
            throw new BusinessRuleException(
                    ErrorCode.REGISTRATION_WITHDRAWN,
                    "Bạn đã hủy đăng ký giải này trước đó và không thể đăng ký lại.");
        }

        if (hackathonRegistrationRepository.existsByUser_IdAndHackathon_StatusAndHackathon_IdNot(
                userId, HackathonStatus.ONGOING, hackathonId)) {
            throw new BusinessRuleException(
                    ErrorCode.REGISTRATION_ALREADY_ACTIVE_ELSEWHERE,
                    "Bạn đã đăng ký một giải đấu khác. Mỗi người chỉ được đăng ký một giải tại một thời điểm.");
        }

        if (hackathon.getMaxParticipants() != null) {
            long currentlyRegistered = hackathonRegistrationRepository.countByHackathon_Id(hackathonId);
            if (currentlyRegistered >= hackathon.getMaxParticipants()) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Đăng ký thất bại: Giải đấu đã đạt giới hạn tối đa số lượng người tham gia (%d/%d người)."
                                .formatted(currentlyRegistered, hackathon.getMaxParticipants()));
            }
        }

        if (HackathonRegistrationSupport.isRegistrationClosed(hackathon)) {
            throw new BusinessRuleException(ErrorCode.REGISTRATION_CLOSED, "Thời gian đăng ký đã kết thúc.");
        }

        LocalDate today = LocalDate.now();
        if (hackathon.getRegistrationStart() != null && today.isBefore(hackathon.getRegistrationStart())) {
            throw new BusinessRuleException(ErrorCode.REGISTRATION_CLOSED, "Chưa đến thời gian mở đăng ký.");
        }

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

        if (teamMemberRepository.isUserInAnyActiveTeamForHackathon(userId, hackathonId)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Bạn phải rời Đội thi hiện tại trước khi hủy đăng ký giải đấu.");
        }

        if (!hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathonId, userId)) {
            if (hackathonRegistrationWithdrawalRepository.existsByHackathon_IdAndUser_Id(hackathonId, userId)) {
                throw new BusinessRuleException(
                        ErrorCode.REGISTRATION_WITHDRAWN,
                        "Bạn đã hủy đăng ký giải này rồi.");
            }
            throw new ResourceNotFoundException("HackathonRegistration", hackathonId);
        }

        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
        User user = userRepository.findById(userId).orElseThrow();

        hackathonRegistrationWithdrawalRepository.save(HackathonRegistrationWithdrawal.builder()
                .hackathon(hackathon)
                .user(user)
                .build());

        hackathonRegistrationRepository.deleteByHackathon_IdAndUser_Id(hackathonId, userId);
    }
}
