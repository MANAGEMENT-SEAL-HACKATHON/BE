package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.ActivateScheduleMode;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Nén / dời lịch Round khi Activate (START_NOW / RESCHEDULE).
 * Round + reminder + presentation slots + audit trong cùng TX; notify fan-out afterCommit.
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
    private final NotificationService notificationService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public static final int DEFAULT_SETUP_LEAD_MINUTES = 5;

    /**
     * @return true nếu đã đổi lịch
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean applyOnActivate(Round round, ActivateScheduleMode mode, LocalDateTime newExamAt) {
        return applyOnActivate(round, mode, newExamAt, null);
    }

    /**
     * @param setupLeadMinutes phút chuẩn bị khi START_NOW; null → {@link #DEFAULT_SETUP_LEAD_MINUTES}
     * @return true nếu đã đổi lịch
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean applyOnActivate(Round round, ActivateScheduleMode mode, LocalDateTime newExamAt,
                                   Integer setupLeadMinutes) {
        ActivateScheduleMode effective = mode != null ? mode : ActivateScheduleMode.KEEP;
        if (effective == ActivateScheduleMode.KEEP) {
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
        LocalDateTime examAt;
        int leadApplied = DEFAULT_SETUP_LEAD_MINUTES;
        if (effective == ActivateScheduleMode.START_NOW) {
            leadApplied = resolveSetupLeadMinutes(setupLeadMinutes);
            // Tech Lead: đúng N phút từ now — không ceil (countdown ~N:00)
            examAt = now.plusMinutes(leadApplied);
        } else if (effective == ActivateScheduleMode.RESCHEDULE) {
            scheduleValidator.requireNewExamAtNotInPast(newExamAt, now);
            examAt = RoundScheduleClocks.ceilToNextMinute(newExamAt);
            if (!examAt.isAfter(now)) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "newExamAt phải lớn hơn thời điểm hiện tại sau khi làm tròn phút");
            }
        } else {
            return false;
        }

        LocalDateTime open = RoundScheduleSeedUtil.submissionOpen(examAt, hours);
        LocalDateTime deadline = RoundScheduleSeedUtil.submissionDeadline(examAt, hours);
        scheduleValidator.validateActivateShift(round, examAt, open, deadline, true);

        round.setExamAt(examAt);
        round.setSubmissionOpen(open);
        round.setSubmissionDeadline(deadline);
        round.setDeadlineReminderSentAt(null);
        Round saved = roundRepository.save(round);

        presentationSlotCascadeService.rescheduleForRound(saved.getId());

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("scheduleMode", effective.name());
        if (effective == ActivateScheduleMode.START_NOW) {
            audit.put("setupLeadMinutes", leadApplied);
        }
        audit.put("oldExamAt", String.valueOf(oldExamAt));
        audit.put("oldSubmissionOpen", String.valueOf(oldOpen));
        audit.put("oldSubmissionDeadline", String.valueOf(oldDeadline));
        audit.put("newExamAt", String.valueOf(saved.getExamAt()));
        audit.put("newSubmissionOpen", String.valueOf(saved.getSubmissionOpen()));
        audit.put("newSubmissionDeadline", String.valueOf(saved.getSubmissionDeadline()));
        audit.put("hackathonId", saved.getHackathon() != null ? saved.getHackathon().getId() : null);
        auditService.log(AuditAction.ROUND_SCHEDULE_SHIFTED, "rounds", saved.getId(), audit);

        scheduleNotifyAfterCommit(saved);
        return true;
    }

    static int resolveSetupLeadMinutes(Integer setupLeadMinutes) {
        if (setupLeadMinutes == null) {
            return DEFAULT_SETUP_LEAD_MINUTES;
        }
        if (setupLeadMinutes < 1) {
            return 1;
        }
        return Math.min(30, setupLeadMinutes);
    }

    private void scheduleNotifyAfterCommit(Round round) {
        Integer roundId = round.getId();
        Integer hackathonId = round.getHackathon() != null ? round.getHackathon().getId() : null;
        String roundName = round.getName();
        LocalDateTime examAt = round.getExamAt();
        LocalDateTime deadline = round.getSubmissionDeadline();

        Runnable notifyTask = () -> notifyStudentsScheduleUpdated(hackathonId, roundId, roundName, examAt, deadline);

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

    private void notifyStudentsScheduleUpdated(Integer hackathonId, Integer roundId, String roundName,
                                               LocalDateTime examAt, LocalDateTime deadline) {
        if (hackathonId == null) {
            return;
        }
        Set<User> students = new LinkedHashSet<>();
        for (Team team : teamRepository.findByHackathon_IdAndStatus(hackathonId, TeamStatus.ACTIVE)) {
            for (TeamMember member : teamMemberRepository.findByTeam_Id(team.getId())) {
                if (member.getStatus() == TeamMemberStatus.ACCEPTED && member.getUser() != null) {
                    students.add(member.getUser());
                }
            }
        }
        if (students.isEmpty()) {
            return;
        }
        String when = examAt != null ? examAt.format(FMT) : "—";
        String due = deadline != null ? deadline.format(FMT) : "—";
        notificationService.sendBatch(
                new ArrayList<>(students),
                "ROUND_SCHEDULE_UPDATED",
                "Thời gian vòng thi '%s' đã được cập nhật".formatted(roundName),
                "Thời gian vòng %s đã được BTC cập nhật. Vòng thi bắt đầu lúc %s. Hạn nộp: %s."
                        .formatted(roundName, when, due),
                "rounds",
                roundId);
    }
}
