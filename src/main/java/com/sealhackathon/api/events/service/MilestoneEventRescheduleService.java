package com.sealhackathon.api.events.service;

import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.rounds.entity.Round;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cập nhật giờ WORKSHOP / KICKOFF / AWARDS khi dời lịch round (không tạo event mới).
 * Công thức khớp seed: WS {@code regEnd+1} 20:00–21:30, KO {@code regEnd+2} 14:00–17:00,
 * AWARDS ngày {@code eventEnd} sau hạn nộp CK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneEventRescheduleService {

    private static final int AWARDS_BUFFER_MINUTES_AFTER_FINAL_DEADLINE = 30;
    private static final int AWARDS_DURATION_MINUTES = 90;

    private final EventRepository eventRepository;
    private final HackathonRepository hackathonRepository;

    /**
     * Đặt WS + KO theo giờ Coord chọn (đã validate GĐ1).
     */
    @Transactional(rollbackFor = Exception.class)
    public int setWorkshopKickoffTimes(Hackathon hackathon,
                                       LocalDateTime wsStart, LocalDateTime wsEnd,
                                       LocalDateTime koStart, LocalDateTime koEnd) {
        if (hackathon == null || hackathon.getId() == null) {
            return 0;
        }
        int count = 0;
        count += updateExistingTimes(hackathon.getId(), EventType.WORKSHOP, wsStart, wsEnd);
        count += updateExistingTimes(hackathon.getId(), EventType.KICKOFF, koStart, koEnd);
        return count;
    }

    /**
     * Đặt AWARDS theo giờ Coord chọn; bump eventEnd nếu cần.
     */
    @Transactional(rollbackFor = Exception.class)
    public int setAwardsTimes(Hackathon hackathon, LocalDateTime awardsStart, LocalDateTime awardsEnd) {
        if (hackathon == null || hackathon.getId() == null || awardsStart == null) {
            return 0;
        }
        LocalDateTime end = awardsEnd != null ? awardsEnd : awardsStart.plusMinutes(AWARDS_DURATION_MINUTES);
        LocalDate awardsDay = awardsStart.toLocalDate();
        if (hackathon.getEventEnd() == null || awardsDay.isAfter(hackathon.getEventEnd())) {
            hackathon.setEventEnd(awardsDay);
            hackathonRepository.save(hackathon);
        }
        return updateExistingTimes(hackathon.getId(), EventType.AWARDS, awardsStart, end);
    }

    /**
     * Đặt lại WS + KO theo {@code registrationEnd} khi còn đủ gap trước {@code eventStart}.
     *
     * @return số event đã đổi giờ
     */
    @Transactional(rollbackFor = Exception.class)
    public int repositionWorkshopKickoff(Hackathon hackathon) {
        if (hackathon == null || hackathon.getId() == null) {
            return 0;
        }
        LocalDate regEnd = hackathon.getRegistrationEnd();
        LocalDate eventStart = hackathon.getEventStart();
        if (regEnd == null || eventStart == null) {
            return 0;
        }
        LocalDate wsDay = regEnd.plusDays(1);
        LocalDate koDay = regEnd.plusDays(2);
        if (!koDay.isBefore(eventStart)) {
            log.warn(
                    "[MilestoneReschedule] hackathonId={} gap regEnd={}→eventStart={} quá hẹp cho WS+KO (cần ≥{} ngày)",
                    hackathon.getId(), regEnd, eventStart, RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
            return 0;
        }
        LocalDateTime wsStart = wsDay.atTime(20, 0);
        LocalDateTime wsEnd = wsDay.atTime(21, 30);
        LocalDateTime koStart = koDay.atTime(14, 0);
        LocalDateTime koEnd = koDay.atTime(17, 0);

        int count = 0;
        count += updateExistingTimes(hackathon.getId(), EventType.WORKSHOP, wsStart, wsEnd);
        count += updateExistingTimes(hackathon.getId(), EventType.KICKOFF, koStart, koEnd);
        return count;
    }

    /**
     * Đặt AWARDS sau hạn nộp Chung kết; bump {@code eventEnd} nếu cần.
     *
     * @return số event AWARDS đã đổi giờ
     */
    @Transactional(rollbackFor = Exception.class)
    public int repositionAwardsAfterFinal(Hackathon hackathon, Round finalRound) {
        if (hackathon == null || hackathon.getId() == null || finalRound == null) {
            return 0;
        }
        LocalDateTime finalDeadline = finalRound.getSubmissionDeadline();
        if (finalDeadline == null && finalRound.getExamAt() != null) {
            Integer hours = finalRound.getCodingDurationHours();
            if (hours != null && hours > 0) {
                finalDeadline = RoundScheduleSeedUtil.submissionDeadline(finalRound.getExamAt(), hours);
            }
        }
        if (finalDeadline == null) {
            return 0;
        }

        LocalDate eventEnd = hackathon.getEventEnd() != null
                ? hackathon.getEventEnd()
                : finalDeadline.toLocalDate();

        LocalDateTime awardsStart = eventEnd.atTime(17, 30);
        LocalDateTime awardsEnd = eventEnd.atTime(19, 0);
        if (!awardsStart.isAfter(finalDeadline)) {
            awardsStart = finalDeadline.plusMinutes(AWARDS_BUFFER_MINUTES_AFTER_FINAL_DEADLINE);
            awardsEnd = awardsStart.plusMinutes(AWARDS_DURATION_MINUTES);
            LocalDate awardsDay = awardsStart.toLocalDate();
            if (hackathon.getEventEnd() == null || awardsDay.isAfter(hackathon.getEventEnd())) {
                hackathon.setEventEnd(awardsDay);
                hackathonRepository.save(hackathon);
            }
        }

        return updateExistingTimes(hackathon.getId(), EventType.AWARDS, awardsStart, awardsEnd);
    }

    private int updateExistingTimes(Integer hackathonId, EventType type,
                                    LocalDateTime startsAt, LocalDateTime endsAt) {
        List<Event> existing = eventRepository.findByHackathonIdAndType(hackathonId, type);
        if (existing.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Event event : existing) {
            boolean changed = false;
            if (!startsAt.equals(event.getStartsAt())) {
                event.setStartsAt(startsAt);
                event.setReminderSentAt(null);
                changed = true;
            }
            if (endsAt != null && !endsAt.equals(event.getEndsAt())) {
                event.setEndsAt(endsAt);
                changed = true;
            }
            if (changed) {
                eventRepository.save(event);
                count++;
            }
        }
        return count;
    }
}
