package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Seed dev — ma trận trạng thái tài khoản (Module 5: xác thực email + duyệt tài khoản).
 *
 * <p>Phủ các case happy → bad mà seed hackathon chưa có:
 * <ul>
 *   <li>Chưa verify email (STUDENT PENDING) → {@code EMAIL_NOT_VERIFIED}.</li>
 *   <li>Chờ duyệt (JUDGE/MENTOR PENDING, đã verify) → hàng chờ "Duyệt tài khoản" của Coordinator.</li>
 *   <li>Bị từ chối (JUDGE REJECTED, có lý do) → {@code ACCOUNT_REJECTED_NOT_ALLOWED_LOGIN}.</li>
 * </ul>
 *
 * <p>Idempotent: nếu tài khoản đã tồn tại thì đồng bộ lại status/verified/reason mong muốn.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class AccountStatesDataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HackathonDevSeedHelper seedHelper;

    @Value("${app.seed.account-states.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[AccountStatesDataSeeder] Tắt");
            return;
        }

        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        Chapter ext = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_EXT);

        // 1) STUDENT — PENDING, email chưa verify → EMAIL_NOT_VERIFIED (gate xác thực + resend)
        upsertAccount(
                AccountStatesSeedConstants.EMAIL_UNVERIFIED_STUDENT,
                "Sinh viên Chưa xác thực",
                UserRole.STUDENT,
                UserType.INTERNAL,
                UserStatus.PENDING,
                false,
                hcm,
                "SV0000UNV",
                null,
                null);

        // 2) MENTOR — PENDING, đã verify → hàng chờ duyệt tài khoản
        upsertAccount(
                AccountStatesSeedConstants.EMAIL_PENDING_MENTOR,
                "Mentor Chờ duyệt",
                UserRole.MENTOR,
                UserType.INTERNAL,
                UserStatus.PENDING,
                true,
                hcm,
                null,
                "FPT University",
                null);

        // 3) JUDGE — PENDING, đã verify → hàng chờ duyệt tài khoản
        upsertAccount(
                AccountStatesSeedConstants.EMAIL_PENDING_JUDGE,
                "Giám khảo Chờ duyệt",
                UserRole.JUDGE,
                UserType.EXTERNAL,
                UserStatus.PENDING,
                true,
                ext,
                null,
                "Tập đoàn Công nghệ ABC",
                null);

        // 4) JUDGE — REJECTED (có lý do) → ACCOUNT_REJECTED_NOT_ALLOWED_LOGIN
        upsertAccount(
                AccountStatesSeedConstants.EMAIL_REJECTED_JUDGE,
                "Giám khảo Bị từ chối",
                UserRole.JUDGE,
                UserType.EXTERNAL,
                UserStatus.REJECTED,
                true,
                ext,
                null,
                "Công ty XYZ",
                AccountStatesSeedConstants.REJECTION_REASON);

        // 5) MENTOR — APPROVED nhưng chưa verify email → EMAIL_NOT_VERIFIED (duyệt ≠ verify)
        upsertAccount(
                AccountStatesSeedConstants.EMAIL_APPROVED_UNVERIFIED_MENTOR,
                "Mentor Đã duyệt Chưa verify",
                UserRole.MENTOR,
                UserType.INTERNAL,
                UserStatus.APPROVED,
                false,
                hcm,
                null,
                "FPT University",
                null);

        log.info("""
                [AccountStatesDataSeeder] Ma trận trạng thái tài khoản sẵn sàng (mật khẩu chung: {}):
                  - {} → STUDENT/PENDING/chưa verify (EMAIL_NOT_VERIFIED + resend)
                  - {} → MENTOR/PENDING/verified (hàng chờ duyệt)
                  - {} → JUDGE/PENDING/verified (hàng chờ duyệt)
                  - {} → JUDGE/REJECTED (ACCOUNT_REJECTED)
                  - {} → MENTOR/APPROVED/chưa verify (EMAIL_NOT_VERIFIED)
                """,
                AccountStatesSeedConstants.DEV_ACCOUNT_PASSWORD,
                AccountStatesSeedConstants.EMAIL_UNVERIFIED_STUDENT,
                AccountStatesSeedConstants.EMAIL_PENDING_MENTOR,
                AccountStatesSeedConstants.EMAIL_PENDING_JUDGE,
                AccountStatesSeedConstants.EMAIL_REJECTED_JUDGE,
                AccountStatesSeedConstants.EMAIL_APPROVED_UNVERIFIED_MENTOR);
    }

    @Transactional
    public void repairForFeTesting() {
        ensureSeed();
    }

    private void upsertAccount(String email, String fullName, UserRole role, UserType userType,
                               UserStatus status, boolean emailVerified, Chapter chapter,
                               String studentCode, String institution, String rejectionReason) {
        LocalDateTime now = LocalDateTime.now();
        String hash = passwordEncoder.encode(AccountStatesSeedConstants.DEV_ACCOUNT_PASSWORD);
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            user.setFullName(fullName);
            user.setRole(role);
            user.setUserType(userType);
            user.setStatus(status);
            user.setChapter(chapter);
            user.setStudentCode(studentCode);
            user.setInstitution(institution);
            user.setRejectionReason(rejectionReason);
            user.setEmailVerifiedAt(emailVerified ? now : null);
            user.setPasswordHash(hash);
            user.setMustChangePassword(false);
            user.setUpdatedAt(now);
            userRepository.save(user);
            return;
        }
        userRepository.save(User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(hash)
                .role(role)
                .userType(userType)
                .status(status)
                .chapter(chapter)
                .studentCode(studentCode)
                .institution(institution)
                .rejectionReason(rejectionReason)
                .isTempAccount(false)
                .isDeptHead(false)
                .mustChangePassword(false)
                .emailVerifiedAt(emailVerified ? now : null)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
