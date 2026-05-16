package com.se194093.be.users.service.impl;

import com.se194093.be.common.response.PageResponse;
import com.se194093.be.users.dto.request.CreateTempJudgeRequest;
import com.se194093.be.users.dto.response.TempJudgeResponse;
import com.se194093.be.users.dto.response.UserSummaryResponse;
import com.se194093.be.users.service.TempJudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-05-personnel.md} §FR-05a.
 *
 * <p>Inject: UserRepository, InvitationRepository, EmailService, UserMapper, AuditService,
 * CurrentUserAccessor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TempJudgeServiceImpl implements TempJudgeService {

    @Override
    public TempJudgeResponse createTempJudge(CreateTempJudgeRequest req) {
        // TODO Dev:
        //  - if userRepo.existsByEmail(req.email) → 409 USER_EMAIL_TAKEN
        //  - User user = User.builder()
        //                  .fullName(req.fullName).email(req.email)
        //                  .role(JUDGE).userType(EXTERNAL).isTempAccount(true)
        //                  .status(APPROVED).institution(req.institution).phone(req.phone)
        //                  .build()
        //  - userRepo.save(user)
        //  - String token = generateUuidTokenBase64(64)
        //  - Invitation inv = Invitation.builder()
        //                       .email(user.email).role(JUDGE).invitedBy(currentUserRef)
        //                       .token(token).expiresAt(now().plusHours(48))
        //                       .build()
        //  - invitationRepo.save(inv)
        //  - emailService.sendInvitation(user.email, user.fullName, token, inv.expiresAt)
        //  - audit.log(TEMP_ACCOUNT_CREATE, "users", user.id,
        //              Map.of("invitationId", inv.id, "institution", req.institution))
        //  - return mapper.toTempJudgeResponse(user, inv, true)
        throw new UnsupportedOperationException("FR-05a POST /users/temp-judges - to be implemented");
    }

    @Override
    public PageResponse<UserSummaryResponse> search(String institution, String q, Pageable pageable) {
        // TODO Dev:
        //  - page = userRepo.searchTempJudges(institution, q, pageable)
        //  - mapped = page.map(mapper::toSummary)
        //  - PageResponse.from(mapped)
        throw new UnsupportedOperationException("FR-05a GET /users/temp-judges - to be implemented");
    }
}
