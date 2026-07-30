package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.config.HackathonProperties;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.dto.request.RegistrationExtensionRequest;
import com.sealhackathon.api.hackathons.dto.response.RegistrationExtensionPreviewResponse;
import com.sealhackathon.api.hackathons.dto.response.RegistrationExtensionPreviewResponse.MilestoneItem;
import com.sealhackathon.api.hackathons.dto.response.RegistrationExtensionPreviewResponse.TeamStats;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.CompetitionScheduleAdjustService;
import com.sealhackathon.api.hackathons.service.HackathonRegistrationExtensionService;
import com.sealhackathon.api.hackathons.support.HackathonRegistrationSupport;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.notifications.value_object.NotificationType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundScheduleValidator;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HackathonRegistrationExtensionServiceImpl implements HackathonRegistrationExtensionService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HackathonRepository hackathonRepository;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final EventRepository eventRepository;
    private final HackathonProperties hackathonProperties;
    private final CompetitionScheduleAdjustService competitionScheduleAdjustService;
    private final StakeholderBroadcastService stakeholderBroadcastService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public RegistrationExtensionPreviewResponse preview(Integer hackathonId, LocalDate newRegistrationEnd) {
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
        return buildPreview(h, newRegistrationEnd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationExtensionPreviewResponse extend(Integer hackathonId, RegistrationExtensionRequest request) {
        if (request == null || request.getNewRegistrationEnd() == null) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Cần chọn ngày hết hạn đăng ký mới.");
        }

        Hackathon h = hackathonRepository.findByIdForUpdate(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        LocalDate newEnd = request.getNewRegistrationEnd();
        int max = Math.max(0, hackathonProperties.getMaxRegistrationExtensions());
        int count = extensionCountOf(h);
        String hardBlock = hardBlockReason(h, newEnd, count, max);
        if (hardBlock != null) {
            throw toExtendException(RegistrationExtensionPreviewResponse.builder()
                    .currentEnd(h.getRegistrationEnd())
                    .newEnd(newEnd)
                    .extensionCount(count)
                    .maxExtensions(max)
                    .canExtend(false)
                    .blockReason(hardBlock)
                    .milestones(List.of())
                    .suggestedAdjustments(List.of())
                    .build());
        }

        RegistrationExtensionPreviewResponse preview = buildPreview(h, newEnd);
        boolean adjust = request.isAdjustCompetitionSchedule();
        boolean hasViolation = preview.getMilestones() != null
                && preview.getMilestones().stream().anyMatch(m -> "VIOLATION".equals(m.getStatus()));

        if (hasViolation && !adjust) {
            throw new BusinessRuleException(ErrorCode.REGISTRATION_EXTENSION_TIMELINE_CONFLICT,
                    preview.getBlockReason() != null
                            ? preview.getBlockReason()
                            : "Dời hạn đăng ký xung đột với lịch Workshop / Khai mạc / Sơ loại — cần điều chỉnh lịch.");
        }

        if (adjust) {
            if (h.getScheduleAdjustedAt() != null) {
                throw new BusinessRuleException(ErrorCode.SCHEDULE_ALREADY_ADJUSTED,
                        "Lịch thi đã được dời 1 lần — chỉ có thể dời hạn đăng ký nếu không còn xung đột mốc lịch, "
                                + "hoặc không kèm điều chỉnh lịch.");
            }
            if (request.getNewPrelimExamAt() == null) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "Cần chọn giờ thi Sơ loại (newPrelimExamAt) khi điều chỉnh lịch kèm dời hạn đăng ký.");
            }
        }

        LocalDate oldEnd = h.getRegistrationEnd();
        LocalDateTime now = LocalDateTime.now();

        h.setRegistrationEnd(newEnd);
        h.setRegistrationExtensionCount(count + 1);
        h.setRegistrationExtendedAt(now);
        hackathonRepository.save(h);

        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("oldRegistrationEnd", oldEnd != null ? oldEnd.toString() : null);
        auditPayload.put("newRegistrationEnd", newEnd.toString());
        auditPayload.put("extensionCount", h.getRegistrationExtensionCount());
        auditPayload.put("adjustCompetitionSchedule", adjust);

        if (adjust) {
            Map<String, Object> scheduleMeta = competitionScheduleAdjustService.apply(
                    h, request.getNewPrelimExamAt(), true, request.getOverrides());
            auditPayload.putAll(scheduleMeta);
        }

        auditService.log(AuditAction.HACKATHON_REGISTRATION_EXTENDED, "hackathons", hackathonId, auditPayload);
        broadcastExtension(h, oldEnd, newEnd);

        return buildPreview(hackathonRepository.findById(hackathonId).orElse(h), newEnd);
    }

    private RegistrationExtensionPreviewResponse buildPreview(Hackathon h, LocalDate newEnd) {
        int max = Math.max(0, hackathonProperties.getMaxRegistrationExtensions());
        int count = extensionCountOf(h);
        LocalDate currentEnd = h.getRegistrationEnd();
        TeamStats teamStats = loadTeamStats(h.getId());

        String hardBlock = hardBlockReason(h, newEnd, count, max);
        List<MilestoneItem> milestones = hardBlock == null
                ? buildMilestones(h, newEnd)
                : List.of();
        List<String> suggestions = new ArrayList<>();
        boolean hasViolation = milestones.stream().anyMatch(m -> "VIOLATION".equals(m.getStatus()));
        if (hasViolation) {
            LocalDate minPrelim = newEnd.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
            LocalDate recommended = newEnd.plusDays(RoundScheduleValidator.MIN_DAYS_FROM_REG_END_TO_PRELIM_EXAM);
            suggestions.add("Đặt Workshop vào ngày %s (registrationEnd + 1)."
                    .formatted(newEnd.plusDays(1).format(DATE_FMT)));
            suggestions.add("Đặt Khai mạc vào ngày %s (registrationEnd + 2)."
                    .formatted(newEnd.plusDays(2).format(DATE_FMT)));
            suggestions.add("Dời thi Sơ loại / eventStart từ %s trở đi (tối thiểu +%d ngày; khuyến nghị +%d → %s)."
                    .formatted(minPrelim.format(DATE_FMT),
                            RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START,
                            RoundScheduleValidator.MIN_DAYS_FROM_REG_END_TO_PRELIM_EXAM,
                            recommended.format(DATE_FMT)));
            if (h.getScheduleAdjustedAt() == null) {
                suggestions.add("Bật «Điều chỉnh lịch thi» khi xác nhận để cascade WS/KO/SL/CK/Awards.");
            } else {
                suggestions.add("Lịch đã dời 1 lần — chọn hạn đăng ký sớm hơn để không vượt mốc WS/KO/SL hiện tại, "
                        + "hoặc không dời hạn nếu còn xung đột.");
            }
        }

        boolean canExtend;
        String blockReason = hardBlock;
        if (hardBlock != null) {
            canExtend = false;
        } else if (hasViolation) {
            canExtend = false;
            boolean canClearViaAdjust = h.getScheduleAdjustedAt() == null;
            blockReason = canClearViaAdjust
                    ? "Hạn đăng ký mới xung đột với lịch hiện tại — cần điều chỉnh lịch thi kèm theo."
                    : "Hạn đăng ký mới xung đột với lịch hiện tại và lịch thi đã được dời 1 lần — không thể cascade.";
        } else {
            canExtend = true;
        }

        return RegistrationExtensionPreviewResponse.builder()
                .currentEnd(currentEnd)
                .newEnd(newEnd)
                .extensionCount(count)
                .maxExtensions(max)
                .teamStats(teamStats)
                .milestones(milestones)
                .canExtend(canExtend)
                .blockReason(blockReason)
                .suggestedAdjustments(suggestions)
                .build();
    }

    private String hardBlockReason(Hackathon h, LocalDate newEnd, int count, int max) {
        if (h.getStatus() != HackathonStatus.ONGOING) {
            return "Chỉ dời hạn đăng ký khi Hackathon đang ONGOING.";
        }
        if (HackathonRegistrationSupport.isRegistrationClosed(h)) {
            return "Đăng ký đã kết thúc — không thể dời hạn.";
        }
        if (newEnd == null) {
            return "Cần chọn ngày hết hạn đăng ký mới.";
        }
        LocalDate today = LocalDate.now();
        LocalDate currentEnd = h.getRegistrationEnd();
        if (currentEnd != null && !newEnd.isAfter(currentEnd)) {
            return "Ngày hết hạn mới phải sau hạn hiện tại (%s)."
                    .formatted(currentEnd.format(DATE_FMT));
        }
        if (!newEnd.isAfter(today)) {
            return "Ngày hết hạn mới phải sau hôm nay (%s)."
                    .formatted(today.format(DATE_FMT));
        }
        if (count >= max) {
            return "Đã dùng hết %d/%d lần dời hạn đăng ký.".formatted(count, max);
        }
        return null;
    }

    private List<MilestoneItem> buildMilestones(Hackathon h, LocalDate newRegEnd) {
        List<MilestoneItem> items = new ArrayList<>();

        Optional<Event> ws = firstEvent(h.getId(), EventType.WORKSHOP);
        LocalDate wsDate = ws.map(Event::getStartsAt).map(LocalDateTime::toLocalDate).orElse(null);
        items.add(milestone("WORKSHOP", "Workshop", wsDate, newRegEnd, gapStatusForWorkshopKickoff(wsDate, newRegEnd, h.getEventStart())));

        Optional<Event> ko = firstEvent(h.getId(), EventType.KICKOFF);
        LocalDate koDate = ko.map(Event::getStartsAt).map(LocalDateTime::toLocalDate).orElse(null);
        items.add(milestone("KICKOFF", "Khai mạc (Kickoff)", koDate, newRegEnd,
                gapStatusForWorkshopKickoff(koDate, newRegEnd, h.getEventStart())));

        Round prelim = roundRepository.findPreliminaryLikeByHackathonId(h.getId()).stream()
                .findFirst()
                .orElse(null);
        LocalDate prelimDate = prelim != null && prelim.getExamAt() != null
                ? prelim.getExamAt().toLocalDate()
                : null;
        items.add(milestone("PRELIM", "Thi Sơ loại", prelimDate, newRegEnd, gapStatusForPrelim(prelimDate, newRegEnd)));

        LocalDate eventStart = h.getEventStart();
        items.add(milestone("EVENT_START", "Ngày bắt đầu sự kiện", eventStart, newRegEnd,
                gapStatusForPrelim(eventStart, newRegEnd)));

        return items;
    }

    /**
     * WS/KO: phải sau registrationEnd và trước eventStart.
     * VIOLATION nếu ≤ regEnd hoặc ≥ eventStart; OK nếu trong cửa sổ.
     */
    static String gapStatusForWorkshopKickoff(LocalDate milestoneDate, LocalDate newRegEnd, LocalDate eventStart) {
        if (milestoneDate == null || newRegEnd == null) {
            return "OK";
        }
        if (!milestoneDate.isAfter(newRegEnd)) {
            return "VIOLATION";
        }
        if (eventStart != null && !milestoneDate.isBefore(eventStart)) {
            return "VIOLATION";
        }
        long days = ChronoUnit.DAYS.between(newRegEnd, milestoneDate);
        if (days == 1 || days == 2) {
            return "OK";
        }
        return "TIGHT";
    }

    /**
     * Prelim / eventStart: ≥ DAYS_REG_END_TO_EVENT_START (3) bắt buộc;
     * &lt; MIN_DAYS (5) → TIGHT; ≥ 5 → OK.
     */
    static String gapStatusForPrelim(LocalDate milestoneDate, LocalDate newRegEnd) {
        if (milestoneDate == null || newRegEnd == null) {
            return "OK";
        }
        long days = ChronoUnit.DAYS.between(newRegEnd, milestoneDate);
        if (days < RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START) {
            return "VIOLATION";
        }
        if (days < RoundScheduleValidator.MIN_DAYS_FROM_REG_END_TO_PRELIM_EXAM) {
            return "TIGHT";
        }
        return "OK";
    }

    private static MilestoneItem milestone(String key, String label, LocalDate date,
                                           LocalDate newRegEnd, String status) {
        Long days = (date != null && newRegEnd != null)
                ? ChronoUnit.DAYS.between(newRegEnd, date)
                : null;
        return MilestoneItem.builder()
                .key(key)
                .label(label)
                .date(date)
                .daysFromNewRegEnd(days)
                .status(status)
                .build();
    }

    private TeamStats loadTeamStats(Integer hackathonId) {
        List<Team> teams = teamRepository.findByHackathon_Id(hackathonId);
        long active = teams.stream().filter(t -> t.getStatus() == TeamStatus.ACTIVE).count();
        long locked = teams.stream()
                .filter(t -> t.getStatus() == TeamStatus.ACTIVE && Boolean.TRUE.equals(t.getIsLocked()))
                .count();
        long pending = teams.stream().filter(t -> t.getStatus() == TeamStatus.PENDING).count();
        return TeamStats.builder()
                .activeCount(active)
                .lockedCount(locked)
                .pendingCount(pending)
                .build();
    }

    private Optional<Event> firstEvent(Integer hackathonId, EventType type) {
        return eventRepository.findByHackathonIdAndType(hackathonId, type).stream().findFirst();
    }

    private static int extensionCountOf(Hackathon h) {
        return h.getRegistrationExtensionCount() == null ? 0 : h.getRegistrationExtensionCount();
    }

    private BusinessRuleException toExtendException(RegistrationExtensionPreviewResponse preview) {
        String reason = preview.getBlockReason() != null
                ? preview.getBlockReason()
                : "Không thể dời hạn đăng ký.";
        int count = preview.getExtensionCount();
        int max = preview.getMaxExtensions();
        if (count >= max) {
            return new BusinessRuleException(ErrorCode.REGISTRATION_EXTENSION_LIMIT_REACHED, reason,
                    Map.of("extensionCount", count, "maxExtensions", max));
        }
        if (reason.contains("sau hạn hiện tại") || reason.contains("sau hôm nay") || reason.contains("ngày hết hạn")) {
            return new BusinessRuleException(ErrorCode.REGISTRATION_EXTENSION_INVALID_DATE, reason);
        }
        if (reason.contains("xung đột") || reason.contains("điều chỉnh lịch")) {
            return new BusinessRuleException(ErrorCode.REGISTRATION_EXTENSION_TIMELINE_CONFLICT, reason);
        }
        if (reason.contains("ONGOING")) {
            return new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, reason);
        }
        if (reason.contains("kết thúc")) {
            return new BusinessRuleException(ErrorCode.REGISTRATION_ALREADY_CLOSED, reason);
        }
        return new BusinessRuleException(ErrorCode.VALIDATION_FAILED, reason);
    }

    private void broadcastExtension(Hackathon h, LocalDate oldEnd, LocalDate newEnd) {
        String oldStr = oldEnd != null ? oldEnd.format(DATE_FMT) : "—";
        String newStr = newEnd != null ? newEnd.format(DATE_FMT) : "—";
        String title = "Đã dời hạn đăng ký hackathon";
        String body = "BTC đã dời hạn đăng ký sự kiện \"" + h.getName() + "\".\n"
                + "• Hạn cũ: " + oldStr + "\n"
                + "• Hạn mới: " + newStr + "\n"
                + "Vui lòng kiểm tra lịch trên hệ thống.";
        stakeholderBroadcastService.broadcast(
                h.getId(),
                NotificationType.REGISTRATION_EXTENDED,
                title,
                body,
                "hackathons",
                h.getId(),
                true);
    }
}
