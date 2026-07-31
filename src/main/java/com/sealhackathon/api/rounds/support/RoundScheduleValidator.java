package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Validate lịch Round cho Edit Schedule và Activate shift.
 * Path force-start ({@code allowEarlyExamAt}) nới gap ĐK/festival nhưng vẫn giữ eventEnd + cửa sổ coding.
 */
@Component
@RequiredArgsConstructor
public class RoundScheduleValidator {

    /** Gap tối thiểu ĐK → ngày thi Sơ loại khi tạo/cập nhật vòng (khác DAYS_REG_END_TO_EVENT_START=3 dùng cho RESCHEDULE). */
    public static final int MIN_DAYS_FROM_REG_END_TO_PRELIM_EXAM = 5;

    private final HackathonTimelineService hackathonTimelineService;
    private final RoundRepository roundRepository;

    public void validateWindowConsistency(LocalDateTime examAt, Integer codingDurationHours,
                                          LocalDateTime submissionOpen, LocalDateTime submissionDeadline) {
        if (examAt == null || codingDurationHours == null || codingDurationHours <= 0) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Thiếu examAt hoặc codingDurationHours hợp lệ để nén/dời lịch");
        }
        if (submissionOpen != null && !examAt.isBefore(submissionOpen)) {
            throw new BusinessRuleException(ErrorCode.ROUND_EXAM_BEFORE_SUBMISSION_OPEN,
                    "Ngày thi (%s) phải trước thời điểm mở nộp bài (%s)"
                            .formatted(examAt, submissionOpen),
                    Map.of("examAt", examAt, "submissionOpen", submissionOpen));
        }
        LocalDateTime expectedDeadline = RoundScheduleSeedUtil.submissionDeadline(examAt, codingDurationHours);
        LocalDateTime expectedOpen = RoundScheduleSeedUtil.submissionOpen(examAt, codingDurationHours);
        if (submissionDeadline == null || !submissionDeadline.equals(expectedDeadline)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "submissionDeadline phải bằng examAt + codingDurationHours (%s)"
                            .formatted(expectedDeadline),
                    Map.of("examAt", examAt, "expectedDeadline", expectedDeadline));
        }
        if (submissionOpen == null || !submissionOpen.equals(expectedOpen)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "submissionOpen phải bằng examAt + 2/3 duration (%s)".formatted(expectedOpen),
                    Map.of("examAt", examAt, "expectedSubmissionOpen", expectedOpen));
        }
    }

    /**
     * @param allowEarlyExamAt true khi Coord xác nhận RESCHEDULE (bỏ min ngày sau ĐK + eventStart/KICKOFF)
     */
    public void validateActivateShift(Round round, LocalDateTime examAt, LocalDateTime submissionOpen,
                                      LocalDateTime submissionDeadline, boolean allowEarlyExamAt) {
        validateActivateShift(round, examAt, submissionOpen, submissionDeadline, allowEarlyExamAt, false);
    }

    /**
     * @param skipSiblingExamOrder true khi sắp cascade sibling (vd. dời Sơ loại rồi kéo CK theo) — tránh reject tạm thời
     */
    public void validateActivateShift(Round round, LocalDateTime examAt, LocalDateTime submissionOpen,
                                      LocalDateTime submissionDeadline, boolean allowEarlyExamAt,
                                      boolean skipSiblingExamOrder) {
        Integer hours = round.getCodingDurationHours();
        validateWindowConsistency(examAt, hours, submissionOpen, submissionDeadline);

        Hackathon hackathon = round.getHackathon();
        if (hackathon == null || hackathon.getId() == null) {
            return;
        }
        Integer hackathonId = hackathon.getId();

        if (!allowEarlyExamAt) {
            validatePreliminaryEarliestExamDate(hackathon, round.getIsFinal(), examAt);
            hackathonTimelineService.validateRoundExamAt(hackathonId, Boolean.TRUE.equals(round.getIsFinal()), examAt);
        } else {
            // Vẫn không vượt eventEnd (RESCHEDULE có thể bump eventEnd sau cascade — cho phép vượt tạm thời bỏ check nếu cascade)
            LocalDate eventEnd = hackathon.getEventEnd();
            if (eventEnd != null && examAt.toLocalDate().isAfter(eventEnd) && !skipSiblingExamOrder) {
                throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                        "Ngày thi (%s) sau eventEnd (%s)".formatted(examAt.toLocalDate(), eventEnd),
                        Map.of("hackathonId", hackathonId, "examAt", examAt, "eventEnd", eventEnd));
            }
        }

        if (!skipSiblingExamOrder) {
            validateSiblingExamOrder(hackathonId, round, examAt);
        }
    }

    public void requireNewExamAtNotInPast(LocalDateTime newExamAt, LocalDateTime now) {
        if (newExamAt == null) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "RESCHEDULE yêu cầu newExamAt");
        }
        if (!newExamAt.isAfter(now)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "newExamAt phải lớn hơn thời điểm hiện tại",
                    Map.of("newExamAt", newExamAt, "now", now));
        }
    }

    /**
     * RESCHEDULE Sơ loại: ngày thi ≥ registrationEnd + {@link RoundScheduleSeedUtil#DAYS_REG_END_TO_EVENT_START}
     * để còn chỗ WORKSHOP + KICKOFF trước ngày thi.
     */
    public void requireReschedulePrelimWorkshopKickoffGap(Round round, LocalDateTime examAt) {
        if (round == null || examAt == null || Boolean.TRUE.equals(round.getIsFinal())) {
            return;
        }
        Hackathon hackathon = round.getHackathon();
        if (hackathon == null || hackathon.getRegistrationEnd() == null) {
            return;
        }
        LocalDate minExamDate = hackathon.getRegistrationEnd().toLocalDate()
                .plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
        if (examAt.toLocalDate().isBefore(minExamDate)) {
            throw new BusinessRuleException(ErrorCode.ROUND_PRELIM_EXAM_ORDER,
                    "Dời lịch Sơ loại: ngày thi phải từ %s trở đi (registrationEnd %s + %d ngày) để còn Workshop và Khai mạc"
                            .formatted(minExamDate, hackathon.getRegistrationEnd(),
                                    RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START),
                    Map.of("hackathonId", hackathon.getId(),
                            "registrationEnd", hackathon.getRegistrationEnd(),
                            "minPreliminaryExamDate", minExamDate,
                            "examAt", examAt));
        }
    }

    private void validatePreliminaryEarliestExamDate(Hackathon hackathon, Boolean isFinal,
                                                     LocalDateTime examAt) {
        if (hackathon == null || examAt == null || Boolean.TRUE.equals(isFinal)) {
            return;
        }
        if (hackathon.getRegistrationEnd() == null) {
            return;
        }
        LocalDate minPrelimExamDate = hackathon.getRegistrationEnd().toLocalDate()
                .plusDays(MIN_DAYS_FROM_REG_END_TO_PRELIM_EXAM);
        if (examAt.toLocalDate().isBefore(minPrelimExamDate)) {
            throw new BusinessRuleException(ErrorCode.ROUND_PRELIM_EXAM_ORDER,
                    "Vòng Sơ loại/Bán kết: ngày thi phải từ %s (registrationEnd %s + %d ngày)"
                            .formatted(minPrelimExamDate, hackathon.getRegistrationEnd(),
                                    MIN_DAYS_FROM_REG_END_TO_PRELIM_EXAM),
                    Map.of("hackathonId", hackathon.getId(),
                            "registrationEnd", hackathon.getRegistrationEnd(),
                            "minPreliminaryExamDate", minPrelimExamDate,
                            "examAt", examAt));
        }
    }

    private void validateSiblingExamOrder(Integer hackathonId, Round round, LocalDateTime examAt) {
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            roundRepository.findPreliminaryLikeByHackathonId(hackathonId).stream()
                    .filter(pr -> pr.getExamAt() != null)
                    .max(java.util.Comparator.comparing(Round::getExamAt))
                    .ifPresent(latestPrelim -> {
                        if (!examAt.isAfter(latestPrelim.getExamAt())) {
                            throw new BusinessRuleException(ErrorCode.ROUND_FINAL_EXAM_ORDER,
                                    "Round Chung kết: ngày thi phải sau vòng Sơ loại (%s)"
                                            .formatted(latestPrelim.getExamAt()),
                                    Map.of("examAt", examAt, "latestPreliminaryExamAt", latestPrelim.getExamAt()));
                        }
                    });
            return;
        }
        roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId)
                .filter(fr -> fr.getExamAt() != null)
                .ifPresent(finalRound -> {
                    if (!examAt.isBefore(finalRound.getExamAt())) {
                        throw new BusinessRuleException(ErrorCode.ROUND_PRELIM_EXAM_ORDER,
                                "Vòng Sơ loại/Bán kết: ngày thi phải trước Chung kết (%s)"
                                        .formatted(finalRound.getExamAt()),
                                Map.of("examAt", examAt, "finalExamAt", finalRound.getExamAt()));
                    }
                });
    }
}
