package com.sealhackathon.api.rounds.job;

import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Khi một vòng đang mở (active, chưa khóa chấm) sắp đến hạn nộp bài trong cửa sổ lead-hours:
 * <ul>
 *   <li>Nhắc <b>sinh viên</b> (thành viên các đội ACTIVE) hoàn tất và nộp bài đúng hạn.</li>
 *   <li>Nhắc <b>giám khảo</b> được phân công chuẩn bị chấm điểm ngay khi hết hạn nộp.</li>
 * </ul>
 * Idempotent qua {@code Round.deadlineReminderSentAt} — mỗi vòng chỉ nhắc một lần.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.round-deadline-reminder.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class RoundDeadlineReminderScheduler {

    private static final DateTimeFormatter DEADLINE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final TrackRepository trackRepository;
    private final NotificationService notificationService;

    @Value("${app.round-deadline-reminder.lead-hours:24}")
    private int leadHours;

    @Scheduled(cron = "${app.round-deadline-reminder.scheduler.cron:0 0 * * * *}")
    @Transactional
    public void runRoundDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(leadHours);
        List<Round> due = roundRepository.findActiveWithUpcomingDeadlineWithoutReminder(now, deadline);
        if (due.isEmpty()) {
            return;
        }

        for (Round round : due) {
            String when = round.getSubmissionDeadline().format(DEADLINE_FMT);
            notifyStudents(round, when);
            notifyJudges(round, when);
            round.setDeadlineReminderSentAt(now);
            roundRepository.save(round);
        }
    }

    private void notifyStudents(Round round, String when) {
        Integer hackathonId = round.getHackathon().getId();
        Set<User> students = new LinkedHashSet<>();
        for (Team team : teamRepository.findByHackathon_IdAndStatus(hackathonId, TeamStatus.ACTIVE)) {
            for (TeamMember tm : teamMemberRepository.findByTeam_Id(team.getId())) {
                if (tm.getStatus() == TeamMemberStatus.ACCEPTED && tm.getUser() != null) {
                    students.add(tm.getUser());
                }
            }
        }
        if (students.isEmpty()) {
            return;
        }
        notificationService.sendBatch(
                new ArrayList<>(students),
                "SUBMISSION_DEADLINE_REMINDER",
                "Sắp đến hạn nộp bài — %s".formatted(round.getName()),
                "Vòng \"%s\" sẽ đóng nộp bài lúc %s. Hãy hoàn tất và nộp bài trước thời hạn để tránh bị trễ."
                        .formatted(round.getName(), when),
                "rounds",
                round.getId());
        log.info("Sent SUBMISSION_DEADLINE_REMINDER for round #{} ({}) to {} students",
                round.getId(), round.getName(), students.size());
    }

    private void notifyJudges(Round round, String when) {
        Set<User> judges = new LinkedHashSet<>();
        for (JudgeAssignment ja : judgeAssignmentRepository.findByRoundId(round.getId())) {
            if (ja.getJudge() != null) {
                judges.add(ja.getJudge());
            }
        }
        for (Track track : trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId())) {
            for (JudgeAssignment ja : judgeAssignmentRepository.findByTrackId(track.getId())) {
                if (ja.getJudge() != null) {
                    judges.add(ja.getJudge());
                }
            }
        }
        if (judges.isEmpty()) {
            return;
        }
        notificationService.sendBatch(
                new ArrayList<>(judges),
                "JUDGE_SCORING_REMINDER",
                "Sắp tới hạn chấm bài — %s".formatted(round.getName()),
                "Vòng \"%s\" đóng nộp bài lúc %s. Hãy sẵn sàng chấm điểm ngay khi vòng mở chấm để kịp tiến độ."
                        .formatted(round.getName(), when),
                "rounds",
                round.getId());
        log.info("Sent JUDGE_SCORING_REMINDER for round #{} ({}) to {} judges",
                round.getId(), round.getName(), judges.size());
    }
}
