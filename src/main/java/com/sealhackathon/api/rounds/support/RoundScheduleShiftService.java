package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.service.MilestoneEventRescheduleService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonRoundTimelineSyncService;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.notifications.value_object.NotificationType;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.ActivateScheduleMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dời lịch Round khi RESCHEDULE (START_NOW đã bị gỡ — phase 2).
 * Sơ loại: cascade CK + sync hackathon dates + WS/KO + AWARDS + slots.
 * Chung kết: shift + AWARDS + sync + slots.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoundScheduleShiftService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RoundRepository roundRepository;
    private final RoundScheduleValidator scheduleValidator;
    private final PresentationSlotCascadeService presentationSlotCascadeService;
    private final AuditService auditService;
    private final StakeholderBroadcastService stakeholderBroadcastService;
    private final HackathonRoundTimelineSyncService hackathonRoundTimelineSyncService;
    private final MilestoneEventRescheduleService milestoneEventRescheduleService;
    private final HackathonRepository hackathonRepository;

    /**
     * @return true nếu đã đổi lịch
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean applyOnActivate(Round round, ActivateScheduleMode mode, LocalDateTime newExamAt) {
        ActivateScheduleMode effective = mode != null ? mode : ActivateScheduleMode.KEEP;
        if (effective == ActivateScheduleMode.KEEP) {
            return false;
        }
        if (effective != ActivateScheduleMode.RESCHEDULE) {
            return false;
        }

        Integer hours = round.getCodingDurationHours();
        if (hours == null || hours <= 0) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Round chưa có codingDurationHours — không thể nén/dời lịch thi");
        }

        LocalDateTime oldExamAt = round.getExamAt();
        LocalDateTime oldOpen = round.getSubmissionOpen();
        LocalDateTime oldDeadline = round.getSubmissionDeadline();

        LocalDateTime now = LocalDateTime.now();
        scheduleValidator.requireNewExamAtNotInPast(newExamAt, now);
        LocalDateTime examAt = RoundScheduleClocks.ceilToNextMinute(newExamAt);
        if (!examAt.isAfter(now)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "newExamAt phải lớn hơn thời điểm hiện tại sau khi làm tròn phút");
        }
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            scheduleValidator.requireReschedulePrelimWorkshopKickoffGap(round, examAt);
        }

        LocalDateTime open = RoundScheduleSeedUtil.submissionOpen(examAt, hours);
        LocalDateTime deadline = RoundScheduleSeedUtil.submissionDeadline(examAt, hours);
        // skip sibling khi dời Sơ loại — cascade CK ngay sau; hoặc dời CK rồi sync eventEnd
        boolean skipSibling = true;
        scheduleValidator.validateActivateShift(round, examAt, open, deadline, true, skipSibling);

        round.setExamAt(examAt);
        round.setSubmissionOpen(open);
        round.setSubmissionDeadline(deadline);
        round.setDeadlineReminderSentAt(null);
        // Re-open submission window: clear early-close flag so phase gates stay consistent
        boolean clearedClosedEarly = round.getSubmissionClosedEarlyAt() != null;
        if (clearedClosedEarly) {
            round.setSubmissionClosedEarlyAt(null);
        }
        Round saved = roundRepository.save(round);

        Map<String, Object> cascadeMeta = new LinkedHashMap<>();
        if (Boolean.TRUE.equals(saved.getIsFinal())) {
            cascadeAfterFinalShift(saved, cascadeMeta);
        } else {
            cascadeAfterPrelimShift(saved, cascadeMeta);
        }

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("scheduleMode", effective.name());
        audit.put("oldExamAt", String.valueOf(oldExamAt));
        audit.put("oldSubmissionOpen", String.valueOf(oldOpen));
        audit.put("oldSubmissionDeadline", String.valueOf(oldDeadline));
        audit.put("newExamAt", String.valueOf(saved.getExamAt()));
        audit.put("newSubmissionOpen", String.valueOf(saved.getSubmissionOpen()));
        audit.put("newSubmissionDeadline", String.valueOf(saved.getSubmissionDeadline()));
        audit.put("clearedSubmissionClosedEarlyAt", clearedClosedEarly);
        audit.put("hackathonId", saved.getHackathon() != null ? saved.getHackathon().getId() : null);
        audit.putAll(cascadeMeta);
        auditService.log(AuditAction.ROUND_SCHEDULE_SHIFTED, "rounds", saved.getId(), audit);

        scheduleNotifyAfterCommit(saved);
        return true;
    }

    /**
     * Sơ loại đã shift → kéo CK vào [end+1h, end+2h], sync dates, WS/KO, AWARDS, slots.
     */
    private void cascadeAfterPrelimShift(Round prelim, Map<String, Object> meta) {
        Integer hackathonId = prelim.getHackathon() != null ? prelim.getHackathon().getId() : null;
        if (hackathonId == null) {
            presentationSlotCascadeService.rescheduleForRound(prelim.getId());
            return;
        }

        Integer prelimHours = prelim.getCodingDurationHours();
        Optional<Round> finalOpt = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId);
        Round cascadedFinal = null;
        if (finalOpt.isPresent() && prelim.getExamAt() != null && prelimHours != null && prelimHours > 0) {
            Round finalRound = finalOpt.get();
            Integer finalHours = finalRound.getCodingDurationHours();
            if (finalHours == null || finalHours <= 0) {
                finalHours = RoundScheduleSeedUtil.DEFAULT_FINAL_CODING_HOURS;
            }
            LocalDateTime finalExam = RoundScheduleSeedUtil.maxFinalExamAt(prelim.getExamAt(), prelimHours);
            LocalDateTime finalOpen = RoundScheduleSeedUtil.submissionOpen(finalExam, finalHours);
            LocalDateTime finalDeadline = RoundScheduleSeedUtil.submissionDeadline(finalExam, finalHours);
            finalRound.setExamAt(finalExam);
            finalRound.setSubmissionOpen(finalOpen);
            finalRound.setSubmissionDeadline(finalDeadline);
            finalRound.setDeadlineReminderSentAt(null);
            if (finalRound.getSubmissionClosedEarlyAt() != null) {
                finalRound.setSubmissionClosedEarlyAt(null);
            }
            if (finalRound.getCodingDurationHours() == null || finalRound.getCodingDurationHours() <= 0) {
                finalRound.setCodingDurationHours(finalHours);
            }
            cascadedFinal = roundRepository.save(finalRound);
            meta.put("cascadedFinalRoundId", cascadedFinal.getId());
            meta.put("cascadedFinalExamAt", String.valueOf(cascadedFinal.getExamAt()));
        }

        hackathonRoundTimelineSyncService.syncFromRounds(hackathonId);
        Hackathon h = hackathonRepository.findById(hackathonId).orElse(null);
        if (h == null) {
            presentationSlotCascadeService.rescheduleForRound(prelim.getId());
            if (cascadedFinal != null) {
                presentationSlotCascadeService.rescheduleForRound(cascadedFinal.getId());
            }
            return;
        }

        int milestones = milestoneEventRescheduleService.repositionWorkshopKickoff(h);
        Round finalForAwards = cascadedFinal != null ? cascadedFinal : finalOpt.orElse(null);
        if (finalForAwards != null) {
            milestones += milestoneEventRescheduleService.repositionAwardsAfterFinal(h, finalForAwards);
        }
        meta.put("milestonesUpdated", milestones);
        if (h.getEventStart() != null) {
            meta.put("eventStart", h.getEventStart().toString());
        }
        if (h.getEventEnd() != null) {
            meta.put("eventEnd", h.getEventEnd().toString());
        }

        presentationSlotCascadeService.rescheduleForRound(prelim.getId());
        if (cascadedFinal != null) {
            presentationSlotCascadeService.rescheduleForRound(cascadedFinal.getId());
        }
    }

    private void cascadeAfterFinalShift(Round finalRound, Map<String, Object> meta) {
        Integer hackathonId = finalRound.getHackathon() != null ? finalRound.getHackathon().getId() : null;
        if (hackathonId != null) {
            hackathonRoundTimelineSyncService.syncFromRounds(hackathonId);
            Hackathon h = hackathonRepository.findById(hackathonId).orElse(null);
            if (h != null) {
                int awards = milestoneEventRescheduleService.repositionAwardsAfterFinal(h, finalRound);
                meta.put("milestonesUpdated", awards);
                if (h.getEventEnd() != null) {
                    meta.put("eventEnd", h.getEventEnd().toString());
                }
            }
        }
        presentationSlotCascadeService.rescheduleForRound(finalRound.getId());
    }

    private void scheduleNotifyAfterCommit(Round round) {
        Integer roundId = round.getId();
        Integer hackathonId = round.getHackathon() != null ? round.getHackathon().getId() : null;
        String roundName = round.getName();
        LocalDateTime examAt = round.getExamAt();
        LocalDateTime deadline = round.getSubmissionDeadline();

        Runnable notifyTask = () -> notifyStakeholdersScheduleUpdated(
                hackathonId, roundId, roundName, examAt, deadline);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notifyTask.run();
                    } catch (Exception ex) {
                        log.warn("[RoundScheduleShift] afterCommit notify failed roundId={}: {}",
                                roundId, ex.getMessage());
                    }
                }
            });
        } else {
            notifyTask.run();
        }
    }

    private void notifyStakeholdersScheduleUpdated(Integer hackathonId, Integer roundId, String roundName,
                                                   LocalDateTime examAt, LocalDateTime deadline) {
        if (hackathonId == null) {
            return;
        }
        String when = examAt != null ? examAt.format(FMT) : "—";
        String due = deadline != null ? deadline.format(FMT) : "—";
        stakeholderBroadcastService.broadcast(
                hackathonId,
                NotificationType.ROUND_SCHEDULE_UPDATED,
                "Thời gian vòng thi '%s' đã được cập nhật".formatted(roundName),
                "Thời gian vòng %s đã được BTC cập nhật. Vòng thi bắt đầu lúc %s. Hạn nộp: %s."
                        .formatted(roundName, when, due),
                "rounds",
                roundId,
                true);
    }
}
