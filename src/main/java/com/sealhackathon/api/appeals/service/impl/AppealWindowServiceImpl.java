package com.sealhackathon.api.appeals.service.impl;

import com.sealhackathon.api.appeals.dto.request.AppealDelayRequest;
import com.sealhackathon.api.appeals.dto.request.PublishWithAppealWindowRequest;
import com.sealhackathon.api.appeals.dto.response.AppealDelayPreviewResponse;
import com.sealhackathon.api.appeals.dto.response.AppealWindowStatusResponse;
import com.sealhackathon.api.appeals.dto.response.PublishPreflightResponse;
import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.appeals.repository.AppealRepository;
import com.sealhackathon.api.appeals.service.AppealWindowService;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.appeals.value_object.AppealWindowMode;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.notifications.value_object.NotificationType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundScheduleShiftService;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AppealWindowServiceImpl implements AppealWindowService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final EnumSet<AppealStatus> OPEN_STATUSES =
            EnumSet.of(AppealStatus.PENDING, AppealStatus.UNDER_REVIEW);

    private final RoundAccessGuard roundAccessGuard;
    private final RoundRepository roundRepository;
    private final AppealRepository appealRepository;
    private final AuditService auditService;
    private final StakeholderBroadcastService stakeholderBroadcastService;
    private final NotificationService notificationService;
    private final TeamMemberRepository teamMemberRepository;
    private final RoundScheduleShiftService roundScheduleShiftService;

    @Override
    @Transactional(readOnly = true)
    public PublishPreflightResponse preflight(Integer roundId) {
        Round prelim = requirePrelimRound(roundId);
        LocalDateTime now = LocalDateTime.now();
        Hackathon hackathon = prelim.getHackathon();
        int configured = configuredWindowMinutes(hackathon);
        Optional<Round> finalOpt = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathon.getId());
        LocalDateTime finalExamAt = finalOpt.map(Round::getExamAt).orElse(null);

        Long remaining = null;
        boolean fits = true;
        if (configured <= 0 || finalExamAt == null) {
            fits = true; // window won't open; publish proceeds without mode
        } else {
            remaining = Math.max(0, Duration.between(now, finalExamAt).toMinutes());
            fits = remaining >= configured;
        }

        int applied = prelim.getAppealDelayMinutesApplied() != null
                ? prelim.getAppealDelayMinutesApplied() : 0;
        int delayBudget = Math.max(0, MAX_APPEAL_DELAY_MINUTES - applied);
        int shortfall = (!fits && remaining != null)
                ? (int) Math.max(0, configured - remaining) : 0;

        List<PublishPreflightResponse.ModeAvailability> modes = new ArrayList<>();
        if (!fits && configured > 0 && finalExamAt != null) {
            boolean delayOk = shortfall > 0 && shortfall <= delayBudget;
            String delayBlocked = null;
            if (shortfall <= 0) {
                delayBlocked = "Không cần dời lịch";
            } else if (shortfall > delayBudget) {
                delayBlocked = "Ngân sách dời lịch còn %d phút, cần %d".formatted(delayBudget, shortfall);
            }
            modes.add(PublishPreflightResponse.ModeAvailability.builder()
                    .mode(AppealWindowMode.DELAY_FINAL)
                    .available(delayOk)
                    .blockedReason(delayBlocked)
                    .suggestedDelayMinutes(shortfall > 0 ? shortfall : null)
                    .build());

            boolean shrinkOk = remaining != null && remaining >= MIN_APPEAL_WINDOW_MINUTES;
            modes.add(PublishPreflightResponse.ModeAvailability.builder()
                    .mode(AppealWindowMode.SHRINK)
                    .available(shrinkOk)
                    .blockedReason(shrinkOk ? null
                            : "Còn dưới %d phút — không rút ngắn được".formatted(MIN_APPEAL_WINDOW_MINUTES))
                    .build());

            modes.add(PublishPreflightResponse.ModeAvailability.builder()
                    .mode(AppealWindowMode.SKIP)
                    .available(true)
                    .blockedReason(null)
                    .build());
        }

        return PublishPreflightResponse.builder()
                .serverNow(now)
                .finalExamAt(finalExamAt)
                .configuredWindowMinutes(configured)
                .remainingMinutes(remaining)
                .fits(fits)
                .availableModes(modes)
                .build();
    }

    @Override
    public void openOnFirstPublish(Round prelimRound, PublishWithAppealWindowRequest modeRequest,
                                   LocalDateTime publishedAt) {
        // One-shot: never reset if already set (republish / re-entry guard)
        if (prelimRound.getAppealWindowEndsAt() != null) {
            return;
        }

        Hackathon hackathon = prelimRound.getHackathon();
        int configured = configuredWindowMinutes(hackathon);
        Optional<Round> finalOpt = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathon.getId());
        Round finalRound = finalOpt.orElse(null);

        if (configured <= 0 || finalRound == null || finalRound.getExamAt() == null) {
            return; // feature off or no final — GĐ4 as before
        }

        LocalDateTime now = publishedAt != null ? publishedAt : LocalDateTime.now();
        long remaining = Math.max(0, Duration.between(now, finalRound.getExamAt()).toMinutes());
        boolean fits = remaining >= configured;

        AppealWindowMode mode = modeRequest != null ? modeRequest.getAppealWindowMode() : null;

        if (!fits && mode == null) {
            throw new BusinessRuleException(ErrorCode.APPEAL_WINDOW_DOES_NOT_FIT,
                    "Thời gian còn lại tới Chung kết (%d phút) không đủ cửa sổ khiếu nại (%d phút). Chọn DELAY_FINAL, SHRINK hoặc SKIP."
                            .formatted(remaining, configured),
                    Map.of("remainingMinutes", remaining, "configuredWindowMinutes", configured));
        }

        if (fits || mode == null) {
            LocalDateTime endsAt = now.plusMinutes(configured);
            if (endsAt.isAfter(finalRound.getExamAt())) {
                endsAt = finalRound.getExamAt();
            }
            setWindowOpen(prelimRound, endsAt, configured, null);
            return;
        }

        switch (mode) {
            case DELAY_FINAL -> {
                int shortfall = (int) Math.max(0, configured - remaining);
                int delay = modeRequest.getDelayMinutes() != null ? modeRequest.getDelayMinutes() : shortfall;
                if (delay <= 0) {
                    delay = shortfall;
                }
                roundScheduleShiftService.delayFinalForAppeals(prelimRound, finalRound, delay);
                // reload final exam after delay
                finalRound = roundRepository.findById(finalRound.getId()).orElse(finalRound);
                LocalDateTime endsAt = now.plusMinutes(configured);
                if (finalRound.getExamAt() != null && endsAt.isAfter(finalRound.getExamAt())) {
                    endsAt = finalRound.getExamAt();
                }
                setWindowOpen(prelimRound, endsAt, configured, null);
            }
            case SHRINK -> {
                if (remaining < MIN_APPEAL_WINDOW_MINUTES) {
                    throw new BusinessRuleException(ErrorCode.APPEAL_WINDOW_BELOW_MINIMUM,
                            "Không thể rút ngắn cửa sổ khi còn dưới %d phút".formatted(MIN_APPEAL_WINDOW_MINUTES),
                            Map.of("remainingMinutes", remaining));
                }
                LocalDateTime endsAt = now.plusMinutes(remaining);
                if (endsAt.isAfter(finalRound.getExamAt())) {
                    endsAt = finalRound.getExamAt();
                }
                setWindowOpen(prelimRound, endsAt, (int) remaining, AuditAction.APPEAL_WINDOW_SHRUNK);
            }
            case SKIP -> {
                if (!StringUtils.hasText(modeRequest.getSkipReason())) {
                    throw new BusinessRuleException(ErrorCode.APPEAL_WINDOW_SKIP_REASON_REQUIRED,
                            "Phải ghi lý do khi bỏ qua cửa sổ khiếu nại");
                }
                auditService.log(AuditAction.APPEAL_WINDOW_SKIPPED, "rounds", prelimRound.getId(),
                        Map.of("skipReason", modeRequest.getSkipReason(),
                                "remainingMinutes", remaining,
                                "configuredWindowMinutes", configured));
                stakeholderBroadcastService.broadcast(
                        hackathon.getId(),
                        NotificationType.APPEAL_WINDOW_SKIPPED,
                        "Vòng «%s» không mở khiếu nại".formatted(prelimRound.getName()),
                        "BTC không mở cửa sổ khiếu nại cho vòng này. Lý do: %s"
                                .formatted(modeRequest.getSkipReason().trim()),
                        "rounds",
                        prelimRound.getId(),
                        true);
            }
        }
    }

    private void setWindowOpen(Round prelim, LocalDateTime endsAt, int windowMinutes, String shrinkAudit) {
        prelim.setAppealWindowEndsAt(endsAt);
        if (prelim.getPublishRevision() == null || prelim.getPublishRevision() < 1) {
            prelim.setPublishRevision(1);
        }
        roundRepository.save(prelim);

        auditService.log(AuditAction.APPEAL_WINDOW_OPEN, "rounds", prelim.getId(),
                Map.of("appealWindowEndsAt", endsAt.toString(),
                        "windowMinutes", windowMinutes));
        if (shrinkAudit != null) {
            auditService.log(shrinkAudit, "rounds", prelim.getId(),
                    Map.of("appealWindowEndsAt", endsAt.toString(),
                            "windowMinutes", windowMinutes));
        }

        Integer hackathonId = prelim.getHackathon() != null ? prelim.getHackathon().getId() : null;
        if (hackathonId != null) {
            stakeholderBroadcastService.broadcast(
                    hackathonId,
                    NotificationType.APPEAL_WINDOW_OPENED,
                    "Cửa sổ khiếu nại đã mở — %s".formatted(prelim.getName()),
                    "Đội bị loại kỷ luật có thể nộp đơn khiếu nại đến %s."
                            .formatted(endsAt.format(FMT)),
                    "rounds",
                    prelim.getId(),
                    true);
        }
    }

    @Override
    public AppealWindowStatusResponse closeEarly(Integer roundId) {
        Round prelim = requirePrelimRound(roundId);
        if (prelim.getAppealWindowEndsAt() == null) {
            throw new BusinessRuleException(ErrorCode.APPEAL_WINDOW_NOT_OPEN,
                    "Cửa sổ khiếu nại chưa được mở");
        }
        if (appealRepository.existsByRound_IdAndStatusIn(roundId, OPEN_STATUSES)) {
            throw new BusinessRuleException(ErrorCode.APPEAL_WINDOW_HAS_PENDING,
                    "Không đóng sớm khi còn đơn PENDING hoặc UNDER_REVIEW");
        }
        LocalDateTime now = LocalDateTime.now();
        if (prelim.getAppealWindowEndsAt().isAfter(now)) {
            prelim.setAppealWindowEndsAt(now);
            roundRepository.save(prelim);
            auditService.log(AuditAction.APPEAL_WINDOW_CLOSE_EARLY, "rounds", roundId,
                    Map.of("closedAt", now.toString()));
        }
        return getWindowStatus(roundId);
    }

    @Override
    public int expireOpenAppealsForRound(Integer roundId) {
        Round round = roundRepository.findById(roundId).orElse(null);
        if (round == null || round.getAppealWindowEndsAt() == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(round.getAppealWindowEndsAt())) {
            return 0;
        }
        List<Appeal> open = appealRepository.findByRoundIdAndStatusIn(roundId, OPEN_STATUSES);
        if (open.isEmpty()) {
            return 0;
        }
        for (Appeal appeal : open) {
            appeal.setStatus(AppealStatus.EXPIRED);
            appealRepository.save(appeal);
            auditService.log(AuditAction.APPEAL_EXPIRE, "appeals", appeal.getId(),
                    Map.of("roundId", roundId, "teamId", appeal.getTeam().getId()));
            notifyTeam(appeal, NotificationType.APPEAL_EXPIRED,
                    "Đơn khiếu nại đã hết hạn",
                    "Đơn khiếu nại của đội «%s» đã hết hạn vì cửa sổ khiếu nại đã đóng."
                            .formatted(appeal.getTeam().getTeamName()));
        }
        return open.size();
    }

    @Override
    public int expireAllDueAppeals() {
        LocalDateTime now = LocalDateTime.now();
        List<Integer> roundIds = appealRepository.findRoundIdsWithExpiredOpenAppeals(now, OPEN_STATUSES);
        int total = 0;
        for (Integer roundId : roundIds) {
            total += expireOpenAppealsForRound(roundId);
        }
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public AppealWindowStatusResponse getWindowStatus(Integer roundId) {
        Round prelim = requirePrelimRound(roundId);
        LocalDateTime now = LocalDateTime.now();
        Optional<Round> finalOpt = roundRepository.findByHackathon_IdAndIsFinalTrue(
                prelim.getHackathon().getId());
        LocalDateTime finalExamAt = finalOpt.map(Round::getExamAt).orElse(null);
        long pending = appealRepository.countByRound_IdAndStatus(roundId, AppealStatus.PENDING);
        long underReview = appealRepository.countByRound_IdAndStatus(roundId, AppealStatus.UNDER_REVIEW);
        int applied = prelim.getAppealDelayMinutesApplied() != null
                ? prelim.getAppealDelayMinutesApplied() : 0;

        return AppealWindowStatusResponse.builder()
                .serverNow(now)
                .appealWindowEndsAt(prelim.getAppealWindowEndsAt())
                .finalExamAt(finalExamAt)
                .pendingCount(pending)
                .underReviewCount(underReview)
                .delayMinutesRemaining(Math.max(0, MAX_APPEAL_DELAY_MINUTES - applied))
                .windowState(resolveWindowState(prelim, now))
                .publishRevision(prelim.getPublishRevision())
                .resultsRevisedAt(prelim.getResultsRevisedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AppealDelayPreviewResponse previewDelay(Integer prelimRoundId, AppealDelayRequest request) {
        Round prelim = requirePrelimRound(prelimRoundId);
        Round finalRound = requireFinalForDelay(prelim);
        int minutes = request.getMinutes();
        assertDelayApplicable(prelim, finalRound, minutes);

        LocalDateTime current = finalRound.getExamAt();
        LocalDateTime neu = current.plusMinutes(minutes);
        int applied = prelim.getAppealDelayMinutesApplied() != null
                ? prelim.getAppealDelayMinutesApplied() : 0;

        List<String> consequences = new ArrayList<>();
        consequences.add("Giờ Chung kết dời từ %s sang %s".formatted(current.format(FMT), neu.format(FMT)));
        consequences.add("Sự kiện AWARDS sẽ được dịch theo");
        consequences.add("Hàng đợi thuyết trình Chung kết sẽ được xếp lại");
        consequences.add("Thông báo gửi tới sinh viên, mentor, giám khảo và coordinator");

        return AppealDelayPreviewResponse.builder()
                .currentFinalExamAt(current)
                .newFinalExamAt(neu)
                .requestedMinutes(minutes)
                .delayMinutesRemaining(Math.max(0, MAX_APPEAL_DELAY_MINUTES - applied - minutes))
                .delayMinutesApplied(applied)
                .consequences(consequences)
                .build();
    }

    @Override
    public AppealDelayPreviewResponse applyDelay(Integer prelimRoundId, AppealDelayRequest request) {
        Round prelim = requirePrelimRound(prelimRoundId);
        Round finalRound = requireFinalForDelay(prelim);
        int minutes = request.getMinutes();
        assertDelayApplicable(prelim, finalRound, minutes);

        if (!appealRepository.existsByRound_IdAndStatusIn(prelimRoundId, OPEN_STATUSES)) {
            throw new BusinessRuleException(ErrorCode.APPEAL_DELAY_NOT_APPLICABLE,
                    "Chỉ dời giờ khi còn đơn khiếu nại chưa xử lý");
        }

        LocalDateTime newExam = roundScheduleShiftService.delayFinalForAppeals(prelim, finalRound, minutes);
        Round refreshed = roundRepository.findById(prelimRoundId).orElse(prelim);
        int applied = refreshed.getAppealDelayMinutesApplied() != null
                ? refreshed.getAppealDelayMinutesApplied() : 0;

        return AppealDelayPreviewResponse.builder()
                .currentFinalExamAt(finalRound.getExamAt())
                .newFinalExamAt(newExam)
                .requestedMinutes(minutes)
                .delayMinutesRemaining(Math.max(0, MAX_APPEAL_DELAY_MINUTES - applied))
                .delayMinutesApplied(applied)
                .consequences(List.of("Đã dời giờ Chung kết và cascade AWARDS/slots"))
                .build();
    }

    @Override
    public AppealWindowStatusResponse republish(Integer roundId) {
        Round prelim = requirePrelimRound(roundId);
        if (!Boolean.TRUE.equals(prelim.getIsPublished())) {
            throw new BusinessRuleException(ErrorCode.RESULT_NOT_PUBLISHED,
                    "Round chưa công bố — không thể công bố lại");
        }
        LocalDateTime now = LocalDateTime.now();
        int rev = prelim.getPublishRevision() != null ? prelim.getPublishRevision() : 1;
        prelim.setPublishRevision(rev + 1);
        prelim.setResultsRevisedAt(now);
        // MUST NOT reset appealWindowEndsAt
        roundRepository.save(prelim);

        auditService.log(AuditAction.ROUND_REPUBLISH, "rounds", roundId,
                Map.of("publishRevision", prelim.getPublishRevision(),
                        "resultsRevisedAt", now.toString()));

        Integer hackathonId = prelim.getHackathon().getId();
        stakeholderBroadcastService.broadcast(
                hackathonId,
                NotificationType.RESULTS_REVISED,
                "Kết quả đã cập nhật (bản #%d) — %s".formatted(prelim.getPublishRevision(), prelim.getName()),
                "Bảng xếp hạng vòng «%s» đã được công bố lại sau khi xử lý khiếu nại."
                        .formatted(prelim.getName()),
                "rounds",
                roundId,
                true);

        return getWindowStatus(roundId);
    }

    @Override
    public void validateAppealWindowMinutes(int minutes) {
        if (minutes < 0) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Thời gian khiếu nại phải >= 0");
        }
        if (minutes > 0 && minutes < MIN_APPEAL_WINDOW_MINUTES) {
            throw new BusinessRuleException(ErrorCode.APPEAL_WINDOW_BELOW_MINIMUM,
                    "Thời gian khiếu nại tối thiểu %d phút (hoặc 0 để tắt)"
                            .formatted(MIN_APPEAL_WINDOW_MINUTES),
                    Map.of("min", MIN_APPEAL_WINDOW_MINUTES, "requested", minutes));
        }
    }

    private void assertDelayApplicable(Round prelim, Round finalRound, int minutes) {
        if (!Boolean.TRUE.equals(prelim.getIsPublished())) {
            throw new BusinessRuleException(ErrorCode.APPEAL_DELAY_NOT_APPLICABLE,
                    "Chỉ dời giờ sau khi sơ loại đã công bố");
        }
        if (Boolean.TRUE.equals(finalRound.getIsActive())) {
            throw new BusinessRuleException(ErrorCode.APPEAL_DELAY_NOT_APPLICABLE,
                    "Chung kết đã kích hoạt");
        }
        int applied = prelim.getAppealDelayMinutesApplied() != null
                ? prelim.getAppealDelayMinutesApplied() : 0;
        if (applied + minutes > MAX_APPEAL_DELAY_MINUTES) {
            throw new BusinessRuleException(ErrorCode.APPEAL_DELAY_LIMIT_EXCEEDED,
                    "Ngân sách dời giờ kháng cáo tối đa %d phút".formatted(MAX_APPEAL_DELAY_MINUTES),
                    Map.of("applied", applied, "requested", minutes,
                            "remaining", Math.max(0, MAX_APPEAL_DELAY_MINUTES - applied)));
        }
        Hackathon h = finalRound.getHackathon();
        LocalDateTime neu = finalRound.getExamAt().plusMinutes(minutes);
        if (h != null && h.getEventEnd() != null && neu.toLocalDate().isAfter(h.getEventEnd())) {
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "Giờ Chung kết mới vượt eventEnd");
        }
    }

    private Round requireFinalForDelay(Round prelim) {
        return roundRepository.findByHackathon_IdAndIsFinalTrue(prelim.getHackathon().getId())
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.APPEAL_DELAY_NOT_APPLICABLE,
                        "Hackathon chưa có vòng Chung kết"));
    }

    private Round requirePrelimRound(Integer roundId) {
        Round round = roundAccessGuard.requireRound(roundId);
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Cửa sổ khiếu nại chỉ áp dụng vòng Sơ loại");
        }
        return round;
    }

    private static int configuredWindowMinutes(Hackathon hackathon) {
        if (hackathon == null || hackathon.getAppealWindowMinutes() == null) {
            return DEFAULT_APPEAL_WINDOW_MINUTES;
        }
        return hackathon.getAppealWindowMinutes();
    }

    private static String resolveWindowState(Round prelim, LocalDateTime now) {
        int configured = configuredWindowMinutes(prelim.getHackathon());
        if (configured <= 0) {
            return "NOT_CONFIGURED";
        }
        if (!Boolean.TRUE.equals(prelim.getIsPublished())) {
            return "NOT_CONFIGURED";
        }
        if (prelim.getAppealWindowEndsAt() == null) {
            return "SKIPPED";
        }
        if (!now.isBefore(prelim.getAppealWindowEndsAt())) {
            return "EXPIRED";
        }
        return "OPEN";
    }

    private void notifyTeam(Appeal appeal, String type, String title, String body) {
        Set<User> recipients = new LinkedHashSet<>();
        for (TeamMember tm : teamMemberRepository.findByTeam_Id(appeal.getTeam().getId())) {
            if (tm.getStatus() == TeamMemberStatus.ACCEPTED) {
                recipients.add(tm.getUser());
            }
        }
        if (!recipients.isEmpty()) {
            notificationService.sendBatch(new ArrayList<>(recipients), type, title, body,
                    "appeals", appeal.getId());
        }
    }
}
