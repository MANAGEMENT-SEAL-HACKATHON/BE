package com.se194093.be.users.service;

import com.se194093.be.common.response.PageResponse;
import com.se194093.be.users.dto.request.CreateTempJudgeRequest;
import com.se194093.be.users.dto.response.TempJudgeResponse;
import com.se194093.be.users.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Pageable;

/**
 * FR-05a — Tạo & quản lý Judge khách mời (Temp Judge).
 *
 * <p>Business rules:
 * <ul>
 *   <li>Email phải chưa tồn tại trong {@code users} → 409 {@code USER_EMAIL_TAKEN}.</li>
 *   <li>Tạo User với role=JUDGE, userType=EXTERNAL, isTempAccount=TRUE, status=APPROVED.</li>
 *   <li>Tạo {@code invitations} kèm token random 64 char, expires_at = NOW + 48h.</li>
 *   <li>Gọi {@code EmailService.sendInvitation(...)} async — KHÔNG trả token cho Coordinator.</li>
 * </ul>
 *
 * <p>Audit: {@code TEMP_ACCOUNT_CREATE}.
 */
public interface TempJudgeService {

    TempJudgeResponse createTempJudge(CreateTempJudgeRequest req);

    PageResponse<UserSummaryResponse> search(String institution, String q, Pageable pageable);
}
