package com.sealhackathon.api.users.service.impl;

import com.sealhackathon.api.auth.util.TempPasswordGenerator;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.config.FrontendUrls;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.invitations.InvitationConstants;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.invitations.service.GuestJudgeLifecycleService;
import com.sealhackathon.api.users.dto.request.CreateTempJudgeRequest;
import com.sealhackathon.api.users.dto.response.TempJudgeResponse;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.mapper.UserMapper;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.service.TempJudgeService;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TempJudgeServiceImpl implements TempJudgeService {

    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final HackathonRepository hackathonRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final GuestJudgeLifecycleService guestJudgeLifecycleService;

    @Override
    public TempJudgeResponse createTempJudge(CreateTempJudgeRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException(ErrorCode.USER_EMAIL_TAKEN,
                    "Email đã được sử dụng: " + req.getEmail());
        }

        LocalDateTime now = LocalDateTime.now();
        String tempPassword = TempPasswordGenerator.generate();

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(UserRole.JUDGE)
                .userType(UserType.EXTERNAL)
                .isTempAccount(true)
                .isDeptHead(false)
                .mustChangePassword(true)
                .status(UserStatus.APPROVED)
                .institution(req.getInstitution())
                .phone(req.getPhone())
                .emailVerifiedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        User savedUser = userRepository.save(user);

        Integer invitedById = currentUserAccessor.currentUserId();
        Hackathon hackathonRef = hackathonRepository.findById(req.getHackathonId())
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", req.getHackathonId()));
        guestJudgeLifecycleService.assertHackathonNotEnded(hackathonRef);

        Invitation invitation = Invitation.builder()
                .email(savedUser.getEmail())
                .role(UserRole.JUDGE)
                .hackathon(hackathonRef)
                .invitedBy(invitedById == null ? null : User.builder().id(invitedById).build())
                .token(generateToken())
                .expiresAt(now.plusHours(InvitationConstants.INVITATION_EXPIRY_HOURS))
                .createdAt(now)
                .build();
        Invitation savedInv = invitationRepository.save(invitation);

        String loginUrl = FrontendUrls.loginUrl(appProperties);
        boolean tokenSent = true;
        try {
            emailService.sendGuestJudgeInvitation(savedUser.getEmail(), savedUser.getFullName(),
                    tempPassword, loginUrl, savedInv.getExpiresAt());
        } catch (RuntimeException ex) {
            log.warn("[TempJudge] sendGuestJudgeInvitation failed for {}: {}", savedUser.getEmail(), ex.getMessage());
            tokenSent = false;
        }

        auditService.log(AuditAction.TEMP_ACCOUNT_CREATE, "users", savedUser.getId(), Map.of(
                "invitationId", savedInv.getId(),
                "institution", req.getInstitution(),
                "tokenSent", tokenSent));
        return userMapper.toTempJudgeResponse(savedUser, savedInv, tokenSent);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> search(String institution, String q, Pageable pageable) {
        String inst = (institution == null || institution.isBlank()) ? null : institution.trim();
        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        Page<User> page = userRepository.searchTempJudges(inst, keyword, pageable);
        return PageResponse.from(page, page.getContent().stream().map(userMapper::toSummary).toList());
    }

    private String generateToken() {
        byte[] randomBytes = new byte[36];
        RNG.nextBytes(randomBytes);
        String base64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(randomBytes);
        String token = (UUID.randomUUID().toString().replace("-", "") + base64)
                .substring(0, 64);
        return new String(token.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
