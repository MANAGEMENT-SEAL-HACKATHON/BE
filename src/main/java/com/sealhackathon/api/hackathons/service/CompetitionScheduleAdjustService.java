package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.MilestoneEventRescheduleService;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.dto.request.CompetitionScheduleOverrides;
import com.sealhackathon.api.hackathons.dto.response.CompetitionSchedulePreviewResponse;
import com.sealhackathon.api.hackathons.dto.response.CompetitionSchedulePreviewResponse.ScheduleChangeItem;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonRegistrationSupport;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.notifications.value_object.NotificationType;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dời lịch thi Sơ loại + cascade WS/KO/CK/Awards — đúng 1 lần / hackathon.
 * Dùng khi đóng ĐK sớm (kèm chọn ngày) hoặc nút «Dời lịch» riêng (trước Kickoff ≥ 4 ngày).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionScheduleAdjustService {

    public static final int DAYS_BEFORE_KICKOFF_REQUIRED = 4;
    private static final LocalTime DEFAULT_PRELIM_TIME = LocalTime.of(8, 0);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final EventRepository eventRepository;
    private final HackathonRoundTimelineSyncService hackathonRoundTimelineSyncService;
    private final MilestoneEventRescheduleService milestoneEventRescheduleService;
    private final PresentationSlotCascadeService presentationSlotCascadeService;
    private final StakeholderBroadcastService stakeholderBroadcastService;

    @Transactional(readOnly = true)
    public CompetitionSchedulePreviewResponse preview(Integer hackathonId, LocalDateTime newPrelimExamAt) {
        return preview(hackathonId, newPrelimExamAt, false);
    }

    @Transactional(readOnly = true)
    public CompetitionSchedulePreviewResponse preview(Integer hackathonId, LocalDateTime newPrelimExamAt,
                                                      boolean assumeCloseRegToday) {
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
        LocalDate effectiveRegEnd = assumeCloseRegToday ? LocalDate.now() : resolveRegEndForGap(h);
        String block = gateReason(h, newPrelimExamAt, assumeCloseRegToday, effectiveRegEnd);
        List<ScheduleChangeItem> changes = block == null
                ? buildChanges(h, ceilExam(newPrelimExamAt), effectiveRegEnd)
                : List.of();
        return CompetitionSchedulePreviewResponse.builder()
                .newPrelimExamAt(newPrelimExamAt != null ? ceilExam(newPrelimExamAt) : null)
                .alreadyAdjusted(h.getScheduleAdjustedAt() != null)
                .canAdjust(block == null)
                .blockReason(block)
                .changes(changes)
                .build();
    }

    /**
     * Dời lịch từ API: preview + pessimistic lock + apply trong một transaction.
     * Self-invocation: {@code @Transactional} phải nằm trên method này (không dựa preview/apply).
     */
    @Transactional(rollbackFor = Exception.class)
    public CompetitionSchedulePreviewResponse adjust(Integer hackathonId, LocalDateTime newPrelimExamAt,
                                                     CompetitionScheduleOverrides overrides) {
        CompetitionSchedulePreviewResponse preview = preview(hackathonId, newPrelimExamAt);
        Hackathon h = hackathonRepository.findByIdForUpdate(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
        apply(h, newPrelimExamAt, false, overrides);
        return preview;
    }

    /**
     * Áp dụng lịch + đánh dấu đã dời 1 lần + notify.
     *
     * @param skipKickoffWindowGate true khi gọi từ close-reg-early (chưa chắc có KO đúng ngày)
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apply(Hackathon hackathon, LocalDateTime newPrelimExamAt,
                                     boolean skipKickoffWindowGate) {
        return apply(hackathon, newPrelimExamAt, skipKickoffWindowGate, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apply(Hackathon hackathon, LocalDateTime newPrelimExamAt,
                                     boolean skipKickoffWindowGate,
                                     CompetitionScheduleOverrides overrides) {
        if (hackathon == null || hackathon.getId() == null) {
            return Map.of();
        }
        LocalDateTime examAt = ceilExam(newPrelimExamAt);
        String block = gateReason(hackathon, examAt, skipKickoffWindowGate, resolveRegEndForGap(hackathon));
        if (block != null) {
            throw gateException(hackathon, examAt, skipKickoffWindowGate, block);
        }

        Integer hackathonId = hackathon.getId();
        LocalDate regEnd = resolveRegEndForGap(hackathon);
        LocalDate eventStart = examAt.toLocalDate();
        requirePrelimGap(regEnd, eventStart, examAt);

        ResolvedSchedule resolved = resolveSchedule(examAt, regEnd, overrides, prelimHoursOf(hackathonId));
        validateGd1Overrides(regEnd, eventStart, examAt, resolved, prelimHoursOf(hackathonId));

        List<ScheduleChangeItem> beforeNotify = buildChangesFromResolved(hackathon, examAt, regEnd, resolved);

        hackathon.setEventStart(eventStart);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timelineCompressed", true);
        meta.put("scheduleAdjusted", true);
        meta.put("registrationEnd", regEnd != null ? regEnd.toString() : null);
        meta.put("eventStart", eventStart.toString());
        meta.put("prelimExamAt", examAt.toString());

        Round shiftedPrelim = shiftPrelims(hackathonId, examAt, meta);
        Round cascadedFinal = cascadeFinal(hackathonId, shiftedPrelim, resolved.finalExamAt(), meta);

        hackathonRoundTimelineSyncService.syncFromRounds(hackathonId);
        Hackathon refreshed = hackathonRepository.findById(hackathonId).orElse(hackathon);

        int milestones = milestoneEventRescheduleService.setWorkshopKickoffTimes(
                refreshed, resolved.wsStart(), resolved.wsEnd(), resolved.koStart(), resolved.koEnd());
        Round awardsAnchor = cascadedFinal;
        if (awardsAnchor == null) {
            awardsAnchor = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId).orElse(null);
        }
        if (awardsAnchor != null) {
            milestones += milestoneEventRescheduleService.setAwardsTimes(
                    refreshed, resolved.awardsStart(), resolved.awardsEnd());
        }
        meta.put("milestonesUpdated", milestones);
        if (refreshed.getEventEnd() != null) {
            meta.put("eventEnd", refreshed.getEventEnd().toString());
        }

        LocalDateTime now = LocalDateTime.now();
        refreshed.setScheduleAdjustedAt(now);
        hackathonRepository.save(refreshed);
        hackathon.setScheduleAdjustedAt(now);

        notifyStakeholders(refreshed, beforeNotify);

        log.info("[CompetitionSchedule] adjusted hackathonId={} prelimExamAt={}", hackathonId, examAt);
        return meta;
    }

    private record ResolvedSchedule(
            LocalDateTime wsStart, LocalDateTime wsEnd,
            LocalDateTime koStart, LocalDateTime koEnd,
            LocalDateTime finalExamAt,
            LocalDateTime awardsStart, LocalDateTime awardsEnd) {}

    private int prelimHoursOf(Integer hackathonId) {
        return roundRepository.findPreliminaryLikeByHackathonId(hackathonId).stream()
                .map(Round::getCodingDurationHours)
                .filter(h -> h != null && h > 0)
                .findFirst()
                .orElse(RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS);
    }

    private ResolvedSchedule resolveSchedule(LocalDateTime prelimExam, LocalDate regEnd,
                                             CompetitionScheduleOverrides o, int prelimHours) {
        LocalDate wsDay = regEnd.plusDays(1);
        LocalDate koDay = regEnd.plusDays(2);
        LocalDateTime wsStart = o != null && o.getWorkshopStartsAt() != null
                ? ceilExam(o.getWorkshopStartsAt()) : wsDay.atTime(20, 0);
        LocalDateTime wsEnd = o != null && o.getWorkshopEndsAt() != null
                ? ceilExam(o.getWorkshopEndsAt()) : wsDay.atTime(21, 30);
        LocalDateTime koStart = o != null && o.getKickoffStartsAt() != null
                ? ceilExam(o.getKickoffStartsAt()) : koDay.atTime(14, 0);
        LocalDateTime koEnd = o != null && o.getKickoffEndsAt() != null
                ? ceilExam(o.getKickoffEndsAt()) : koDay.atTime(17, 0);

        LocalDateTime defaultFinal = RoundScheduleSeedUtil.maxFinalExamAt(prelimExam, prelimHours);
        LocalDateTime finalExam = o != null && o.getFinalExamAt() != null
                ? ceilExam(o.getFinalExamAt()) : defaultFinal;
        int finalHours = RoundScheduleSeedUtil.DEFAULT_FINAL_CODING_HOURS;
        LocalDateTime finalDeadline = RoundScheduleSeedUtil.submissionDeadline(finalExam, finalHours);
        LocalDateTime awardsStart = o != null && o.getAwardsStartsAt() != null
                ? ceilExam(o.getAwardsStartsAt()) : finalDeadline.plusMinutes(30);
        LocalDateTime awardsEnd = o != null && o.getAwardsEndsAt() != null
                ? ceilExam(o.getAwardsEndsAt()) : awardsStart.plusMinutes(90);
        return new ResolvedSchedule(wsStart, wsEnd, koStart, koEnd, finalExam, awardsStart, awardsEnd);
    }

    private void validateGd1Overrides(LocalDate regEnd, LocalDate eventStart, LocalDateTime prelimExam,
                                      ResolvedSchedule r, int prelimHours) {
        LocalDate wsDay = regEnd.plusDays(1);
        LocalDate koDay = regEnd.plusDays(2);
        if (!r.wsStart().toLocalDate().equals(wsDay) || !r.wsEnd().toLocalDate().equals(wsDay)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Workshop phải đúng ngày registrationEnd+1 (%s) — ràng buộc GĐ1".formatted(wsDay));
        }
        if (!r.koStart().toLocalDate().equals(koDay) || !r.koEnd().toLocalDate().equals(koDay)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Khai mạc phải đúng ngày registrationEnd+2 (%s) — ràng buộc GĐ1".formatted(koDay));
        }
        if (!r.wsStart().toLocalDate().isBefore(r.koStart().toLocalDate())) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Workshop và Khai mạc phải khác ngày lịch (GĐ1)");
        }
        if (!r.wsEnd().isBefore(r.koStart())) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Workshop phải kết thúc trước Khai mạc");
        }
        if (!r.wsStart().toLocalDate().isBefore(eventStart) || !r.koStart().toLocalDate().isBefore(eventStart)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Workshop/Khai mạc phải trước ngày thi Sơ loại (eventStart)");
        }
        LocalDateTime minFinal = RoundScheduleSeedUtil.minFinalExamAt(prelimExam, prelimHours);
        LocalDateTime maxFinal = RoundScheduleSeedUtil.maxFinalExamAt(prelimExam, prelimHours);
        if (r.finalExamAt().isBefore(minFinal) || r.finalExamAt().isAfter(maxFinal)) {
            throw new BusinessRuleException(ErrorCode.ROUND_FINAL_EXAM_ORDER,
                    "Chung kết phải trong [%s ; %s] (sau khi Sơ loại kết thúc +1–2h)"
                            .formatted(minFinal.format(FMT), maxFinal.format(FMT)));
        }
        LocalDateTime finalDeadline = RoundScheduleSeedUtil.submissionDeadline(
                r.finalExamAt(), RoundScheduleSeedUtil.DEFAULT_FINAL_CODING_HOURS);
        if (!r.awardsStart().isAfter(finalDeadline)) {
            throw new BusinessRuleException(ErrorCode.AWARDS_BEFORE_FINAL_DEADLINE,
                    "Lễ trao giải phải sau hạn nộp Chung kết (%s)".formatted(finalDeadline.format(FMT)));
        }
    }

    /** @deprecated dùng {@link #apply} với newPrelimExamAt — giữ overload cho test cũ nếu cần */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> compressAfterRegistrationClosed(Hackathon hackathon) {
        LocalDate regEnd = hackathon.getRegistrationEnd() != null
                ? hackathon.getRegistrationEnd().toLocalDate()
                : null;
        if (regEnd == null) {
            return Map.of();
        }
        LocalDateTime defaultExam = LocalDateTime.of(
                regEnd.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START),
                DEFAULT_PRELIM_TIME);
        return apply(hackathon, defaultExam, true);
    }

    public String gateReason(Hackathon h, LocalDateTime newPrelimExamAt, boolean skipKickoffWindowGate) {
        return gateReason(h, newPrelimExamAt, skipKickoffWindowGate, resolveRegEndForGap(h));
    }

    public String gateReason(Hackathon h, LocalDateTime newPrelimExamAt, boolean skipKickoffWindowGate,
                             LocalDate regEnd) {
        if (h.getScheduleAdjustedAt() != null) {
            return "Lịch thi đã được dời 1 lần — không thể dời lại.";
        }
        if (newPrelimExamAt == null) {
            return "Cần chọn giờ thi Sơ loại.";
        }
        LocalDateTime examAt = ceilExam(newPrelimExamAt);
        if (!examAt.isAfter(LocalDateTime.now())) {
            return "Giờ thi Sơ loại phải sau thời điểm hiện tại.";
        }
        if (regEnd != null) {
            LocalDate minDay = regEnd.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
            if (examAt.toLocalDate().isBefore(minDay)) {
                return "Ngày thi phải từ %s (registrationEnd + %d ngày) để còn Workshop và Khai mạc."
                        .formatted(minDay, RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
            }
        }
        List<Round> prelims = roundRepository.findPreliminaryLikeByHackathonId(h.getId());
        for (Round p : prelims) {
            if (Boolean.TRUE.equals(p.getIsActive()) || p.getProblemReleasedAt() != null) {
                return "Vòng Sơ loại đã kích hoạt / đã phát đề — không thể dời lịch.";
            }
        }
        if (!skipKickoffWindowGate) {
            Optional<Event> ko = earliestKickoff(h.getId());
            if (ko.isPresent() && ko.get().getStartsAt() != null) {
                LocalDateTime deadline = ko.get().getStartsAt().minusDays(DAYS_BEFORE_KICKOFF_REQUIRED);
                if (!LocalDateTime.now().isBefore(deadline)) {
                    return "Chỉ được dời lịch khi còn ít nhất %d ngày trước Khai mạc (%s)."
                            .formatted(DAYS_BEFORE_KICKOFF_REQUIRED, ko.get().getStartsAt().format(FMT));
                }
            }
        }
        return null;
    }

    private BusinessRuleException gateException(Hackathon h, LocalDateTime examAt,
                                                boolean skipKickoff, String message) {
        if (h.getScheduleAdjustedAt() != null) {
            return new BusinessRuleException(ErrorCode.SCHEDULE_ALREADY_ADJUSTED, message);
        }
        if (message != null && message.contains("Khai mạc")) {
            return new BusinessRuleException(ErrorCode.SCHEDULE_ADJUST_TOO_LATE, message);
        }
        if (message != null && message.contains("Workshop")) {
            return new BusinessRuleException(ErrorCode.SCHEDULE_ADJUST_PRELIM_TOO_SOON, message,
                    Map.of("examAt", examAt));
        }
        return new BusinessRuleException(ErrorCode.VALIDATION_FAILED, message);
    }

    private void requirePrelimGap(LocalDate regEnd, LocalDate eventStart, LocalDateTime examAt) {
        if (regEnd == null) {
            return;
        }
        LocalDate minDay = regEnd.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
        if (eventStart.isBefore(minDay)) {
            throw new BusinessRuleException(ErrorCode.SCHEDULE_ADJUST_PRELIM_TOO_SOON,
                    "Ngày thi phải từ %s để còn Workshop và Khai mạc".formatted(minDay),
                    Map.of("examAt", examAt, "minPreliminaryExamDate", minDay));
        }
    }

    private LocalDate resolveRegEndForGap(Hackathon h) {
        if (h.getRegistrationEnd() != null) {
            return h.getRegistrationEnd().toLocalDate();
        }
        return LocalDate.now();
    }

    private List<ScheduleChangeItem> buildChangesFromResolved(Hackathon h, LocalDateTime newPrelimExamAt,
                                                              LocalDate regEnd, ResolvedSchedule r) {
        List<ScheduleChangeItem> items = new ArrayList<>();
        items.add(item("EVENT_START", "Ngày bắt đầu sự kiện (eventStart)",
                str(h.getEventStart()), newPrelimExamAt.toLocalDate().toString()));
        Optional<Event> ws = firstEvent(h.getId(), EventType.WORKSHOP);
        items.add(item("WORKSHOP", "Workshop", fmt(ws.map(Event::getStartsAt).orElse(null)), r.wsStart().format(FMT)));
        Optional<Event> ko = firstEvent(h.getId(), EventType.KICKOFF);
        items.add(item("KICKOFF", "Khai mạc (Kickoff)", fmt(ko.map(Event::getStartsAt).orElse(null)), r.koStart().format(FMT)));
        Round prelim = roundRepository.findPreliminaryLikeByHackathonId(h.getId()).stream().findFirst().orElse(null);
        items.add(item("PRELIM", "Vòng Sơ loại — giờ thi",
                fmt(prelim != null ? prelim.getExamAt() : null), newPrelimExamAt.format(FMT)));
        Round finals = roundRepository.findByHackathon_IdAndIsFinalTrue(h.getId()).orElse(null);
        items.add(item("FINAL", "Vòng Chung kết — giờ thi",
                fmt(finals != null ? finals.getExamAt() : null), r.finalExamAt().format(FMT)));
        Optional<Event> awards = firstEvent(h.getId(), EventType.AWARDS);
        items.add(item("AWARDS", "Lễ trao giải",
                fmt(awards.map(Event::getStartsAt).orElse(null)), r.awardsStart().format(FMT)));
        items.add(item("TRACK_SLOTS", "Slot thuyết trình theo bảng (tracks)",
                "Giữ phân bảng hiện tại",
                "Cập nhật giờ slot theo lịch thi mới (không xóa phân bảng)"));
        items.add(item("EVENT_END", "Ngày kết thúc sự kiện (eventEnd)",
                str(h.getEventEnd()), r.awardsStart().toLocalDate().toString()));
        return items;
    }

    private List<ScheduleChangeItem> buildChanges(Hackathon h, LocalDateTime newPrelimExamAt,
                                                  LocalDate regEnd) {
        return buildChangesFromResolved(h, newPrelimExamAt, regEnd,
                resolveSchedule(newPrelimExamAt, regEnd, null, RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS));
    }

    private Round shiftPrelims(Integer hackathonId, LocalDateTime examAt, Map<String, Object> meta) {
        Round shifted = null;
        for (Round prelim : roundRepository.findPreliminaryLikeByHackathonId(hackathonId)) {
            if (Boolean.TRUE.equals(prelim.getIsActive()) || prelim.getProblemReleasedAt() != null) {
                meta.put("skippedActivePrelimId", prelim.getId());
                continue;
            }
            Integer hours = prelim.getCodingDurationHours();
            if (hours == null || hours <= 0) {
                continue;
            }
            prelim.setExamAt(examAt);
            prelim.setSubmissionOpen(RoundScheduleSeedUtil.submissionOpen(examAt, hours));
            prelim.setSubmissionDeadline(RoundScheduleSeedUtil.submissionDeadline(examAt, hours));
            prelim.setDeadlineReminderSentAt(null);
            shifted = roundRepository.save(prelim);
            meta.put("prelimRoundId", shifted.getId());
            presentationSlotCascadeService.rescheduleForRound(shifted.getId());
        }
        return shifted;
    }

    private Round cascadeFinal(Integer hackathonId, Round anchor, LocalDateTime finalExamOverride,
                               Map<String, Object> meta) {
        Optional<Round> finalOpt = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId);
        if (finalOpt.isEmpty()) {
            return null;
        }
        Round finalRound = finalOpt.get();
        if (Boolean.TRUE.equals(finalRound.getIsActive()) || finalRound.getProblemReleasedAt() != null) {
            return null;
        }
        if (anchor == null || anchor.getExamAt() == null
                || anchor.getCodingDurationHours() == null || anchor.getCodingDurationHours() <= 0) {
            return null;
        }
        Integer finalHours = finalRound.getCodingDurationHours();
        if (finalHours == null || finalHours <= 0) {
            finalHours = RoundScheduleSeedUtil.DEFAULT_FINAL_CODING_HOURS;
            finalRound.setCodingDurationHours(finalHours);
        }
        LocalDateTime minFinal = RoundScheduleSeedUtil.minFinalExamAt(
                anchor.getExamAt(), anchor.getCodingDurationHours());
        LocalDateTime maxFinal = RoundScheduleSeedUtil.maxFinalExamAt(
                anchor.getExamAt(), anchor.getCodingDurationHours());
        LocalDateTime finalExam = finalExamOverride != null ? finalExamOverride : maxFinal;
        if (finalExam.isBefore(minFinal) || finalExam.isAfter(maxFinal)) {
            throw new BusinessRuleException(ErrorCode.ROUND_FINAL_EXAM_ORDER,
                    "Chung kết phải trong [%s ; %s]".formatted(minFinal.format(FMT), maxFinal.format(FMT)));
        }
        finalRound.setExamAt(finalExam);
        finalRound.setSubmissionOpen(RoundScheduleSeedUtil.submissionOpen(finalExam, finalHours));
        finalRound.setSubmissionDeadline(RoundScheduleSeedUtil.submissionDeadline(finalExam, finalHours));
        finalRound.setDeadlineReminderSentAt(null);
        Round saved = roundRepository.save(finalRound);
        meta.put("cascadedFinalRoundId", saved.getId());
        meta.put("cascadedFinalExamAt", String.valueOf(saved.getExamAt()));
        presentationSlotCascadeService.rescheduleForRound(saved.getId());
        return saved;
    }

    private void notifyStakeholders(Hackathon h, List<ScheduleChangeItem> changes) {
        StringBuilder body = new StringBuilder("BTC đã cập nhật lịch sự kiện \"")
                .append(h.getName()).append("\". Chi tiết:\n");
        for (ScheduleChangeItem c : changes) {
            if ("TRACK_SLOTS".equals(c.getKey())) {
                continue;
            }
            body.append("• ").append(c.getLabel()).append(": ")
                    .append(c.getOldValue()).append(" → ").append(c.getNewValue()).append("\n");
        }
        body.append("Vui lòng kiểm tra lịch trên hệ thống.");
        stakeholderBroadcastService.broadcast(
                h.getId(),
                NotificationType.COMPETITION_SCHEDULE_UPDATED,
                "Lịch thi / sự kiện đã được cập nhật",
                body.toString(),
                "hackathons",
                h.getId(),
                true);
    }

    private Optional<Event> earliestKickoff(Integer hackathonId) {
        return eventRepository.findByHackathonIdAndType(hackathonId, EventType.KICKOFF).stream()
                .filter(e -> e.getStartsAt() != null)
                .min((a, b) -> a.getStartsAt().compareTo(b.getStartsAt()));
    }

    private Optional<Event> firstEvent(Integer hackathonId, EventType type) {
        return eventRepository.findByHackathonIdAndType(hackathonId, type).stream().findFirst();
    }

    private static LocalDateTime ceilExam(LocalDateTime at) {
        if (at == null) {
            return null;
        }
        if (at.getSecond() == 0 && at.getNano() == 0) {
            return at;
        }
        return at.plusMinutes(1).withSecond(0).withNano(0);
    }

    private static ScheduleChangeItem item(String key, String label, String oldV, String newV) {
        return ScheduleChangeItem.builder().key(key).label(label).oldValue(oldV).newValue(newV).build();
    }

    private static String fmt(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(FMT);
    }

    private static String str(LocalDate d) {
        return d == null ? "—" : d.toString();
    }

    /** FE helper: registration đã đóng? */
    public boolean isRegistrationClosed(Hackathon h) {
        return HackathonRegistrationSupport.isRegistrationClosed(h);
    }
}
