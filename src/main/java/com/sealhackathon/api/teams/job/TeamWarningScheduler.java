package com.sealhackathon.api.teams.job;

import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.support.HackathonTeamSizeResolver;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cron cảnh báo sinh viên lẻ và đội thiếu người trước giờ chốt sổ.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.team-warning.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class TeamWarningScheduler {

    private final HackathonRepository hackathonRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;
    private final HackathonTeamSizeResolver teamSizeResolver;

    /** Chạy định kỳ lúc 08:00 sáng mỗi ngày. */
    @Scheduled(cron = "${app.team-warning.scheduler.cron:0 0 8 * * *}")
    public void runWarningJob() {
        LocalDate today = LocalDate.now();

        // 1. Quét các Hackathon đang mở đăng ký
        List<Hackathon> ongoingHackathons = hackathonRepository.findAll().stream()
                .filter(h -> h.getStatus() == HackathonStatus.ONGOING)
                .filter(h -> h.getRegistrationEnd() != null
                        && !h.getRegistrationEnd().toLocalDate().isBefore(today))
                .toList();

        for (Hackathon h : ongoingHackathons) {
            long daysLeft = ChronoUnit.DAYS.between(today, h.getRegistrationEnd().toLocalDate());

            // Chỉ bắn cảnh báo nếu còn đúng 2 ngày (48h) hoặc 1 ngày (24h)
            if (daysLeft == 2 || daysLeft == 1) {
                log.info("Bắt đầu quét và cảnh báo đội thi cho Hackathon ID: {}. Còn {} ngày", h.getId(), daysLeft);
                warnOrphanUsers(h, daysLeft);
                warnIncompleteTeams(h, daysLeft);
            }
        }
    }

    private void warnOrphanUsers(Hackathon h, long daysLeft) {
        // Lấy toàn bộ user đã đăng ký
        List<User> registeredUsers = hackathonRegistrationRepository.findAll().stream()
                .filter(r -> r.getHackathon().getId().equals(h.getId()))
                .map(HackathonRegistration::getUser)
                .toList();

        List<User> orphans = new ArrayList<>();
        for (User u : registeredUsers) {
            // Kiểm tra xem User có đang ở trong Đội nào không
            boolean hasTeam = teamMemberRepository.isUserInAnyActiveTeamForHackathon(u.getId(), h.getId());
            if (!hasTeam) {
                orphans.add(u);
            }
        }

        if (!orphans.isEmpty()) {
            notificationService.sendBatch(
                    orphans,
                    "TEAM_WARNING",
                    "⚠️ Khẩn cấp: Bạn chưa có Đội thi đấu!",
                    "Chỉ còn " + daysLeft + " ngày nữa là đóng cổng đăng ký. Hãy nhanh chóng tìm đội hoặc liên hệ Ban Tổ Chức để được hỗ trợ ghép cặp.",
                    "hackathons",
                    h.getId()
            );
            log.info("Đã gửi cảnh báo cho {} sinh viên chưa có đội.", orphans.size());
        }
    }

    private void warnIncompleteTeams(Hackathon h, long daysLeft) {
        // Lấy toàn bộ đội đang PENDING
        List<Team> incompleteTeams = teamRepository.findByHackathon_IdAndStatus(h.getId(), TeamStatus.PENDING);

        List<User> leadersToWarn = new ArrayList<>();
        HackathonTeamSizeResolver.TeamSizeLimits limits = teamSizeResolver.forHackathon(h.getId());
        for (Team t : incompleteTeams) {
            long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(t.getId(), com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED);
            if (acceptedCount < limits.minTeamSize()) {
                leadersToWarn.add(t.getLeader());
            }
        }

        if (!leadersToWarn.isEmpty()) {
            notificationService.sendBatch(
                    leadersToWarn,
                    "TEAM_WARNING",
                    "⚠️ Nguy cơ bị loại: Đội của bạn chưa đủ người!",
                    "Chỉ còn " + daysLeft + " ngày nữa là kết thúc, đội của bạn vẫn đang có dưới "
                            + limits.minTeamSize() + " thành viên. Hãy nhanh chóng tuyển thêm người để Đội được kích hoạt (ACTIVE).",
                    "teams",
                    h.getId()
            );
            log.info("Đã gửi cảnh báo cho {} nhóm trưởng của các đội chưa đủ thành viên.", leadersToWarn.size());
        }
    }
}