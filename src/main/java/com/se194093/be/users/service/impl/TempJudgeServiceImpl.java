package com.se194093.be.users.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.ConflictException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.common.response.PageResponse;
import com.se194093.be.common.security.CurrentUserAccessor;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.repository.HackathonRepository;
import com.se194093.be.invitations.entity.Invitation;
import com.se194093.be.invitations.repository.InvitationRepository;
import com.se194093.be.invitations.service.EmailService;
import com.se194093.be.users.dto.request.CreateTempJudgeRequest;
import com.se194093.be.users.dto.response.TempJudgeResponse;
import com.se194093.be.users.dto.response.UserSummaryResponse;
import com.se194093.be.users.entity.User;
import com.se194093.be.users.mapper.UserMapper;
import com.se194093.be.users.repository.UserRepository;
import com.se194093.be.users.service.TempJudgeService;
import com.se194093.be.users.value_object.UserRole;
import com.se194093.be.users.value_object.UserStatus;
import com.se194093.be.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * FR-05a Temp Judge service. Tạo user APPROVED + Invitation token 64-char, gửi mail stub.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TempJudgeServiceImpl implements TempJudgeService {

    private static final int INVITATION_EXPIRY_HOURS = 48;
    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final HackathonRepository hackathonRepository;

    @Override
    public TempJudgeResponse createTempJudge(CreateTempJudgeRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException(ErrorCode.USER_EMAIL_TAKEN,
                    "Email đã được sử dụng: " + req.getEmail());
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .role(UserRole.JUDGE)
                .userType(UserType.EXTERNAL)
                .isTempAccount(true)
                .isDeptHead(false)
                .status(UserStatus.APPROVED)
                .institution(req.getInstitution())
                .phone(req.getPhone())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        User savedUser = userRepository.save(user);

        Integer invitedById = currentUserAccessor.currentUserId();
        Hackathon hackathonRef = null;
        if (req.getHackathonId() != null) {
            hackathonRef = hackathonRepository.findById(req.getHackathonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hackathon", req.getHackathonId()));
        }

        Invitation invitation = Invitation.builder()
                .email(savedUser.getEmail())
                .role(UserRole.JUDGE)
                .hackathon(hackathonRef)
                .invitedBy(invitedById == null ? null : User.builder().id(invitedById).build())
                .token(generateToken())
                .expiresAt(LocalDateTime.now().plusHours(INVITATION_EXPIRY_HOURS))
                .createdAt(LocalDateTime.now())
                .build();
        Invitation savedInv = invitationRepository.save(invitation);

        boolean tokenSent = true;
        try {
            emailService.sendInvitation(savedUser.getEmail(), savedUser.getFullName(),
                    savedInv.getToken(), savedInv.getExpiresAt());
        } catch (RuntimeException ex) {
            log.warn("[TempJudge] sendInvitation failed for {}: {}", savedUser.getEmail(), ex.getMessage());
            tokenSent = false;
        }

        auditService.log(AuditAction.TEMP_ACCOUNT_CREATE, "users", savedUser.getId(), Map.of(
                "invitationId", savedInv.getId(),
                "institution",  req.getInstitution(),
                "tokenSent",    tokenSent
        ));
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
