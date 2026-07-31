package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.value_object.TeamStatus;

import java.time.LocalDate;
import java.util.List;

/** Kiểm tra cổng đăng ký / khóa đội theo deadline hoặc kết thúc sớm. */
public final class HackathonRegistrationSupport {

    private HackathonRegistrationSupport() {}

    /** Chưa tới ngày mở đăng ký ({@code registrationStart}). */
    public static boolean isRegistrationNotYetOpen(Hackathon hackathon) {
        if (hackathon == null) {
            return true;
        }
        LocalDate start = hackathon.getRegistrationStart();
        if (start == null) {
            return false;
        }
        return LocalDate.now().isBefore(start);
    }

    /**
     * Cửa sổ đăng ký đang mở: ONGOING, đã tới {@code registrationStart},
     * chưa hết hạn / chưa đóng sớm.
     */
    public static boolean isRegistrationWindowOpen(Hackathon hackathon) {
        if (hackathon == null) {
            return false;
        }
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            return false;
        }
        if (isRegistrationNotYetOpen(hackathon)) {
            return false;
        }
        return !isRegistrationClosed(hackathon);
    }

    public static boolean isRegistrationClosed(Hackathon hackathon) {
        if (hackathon == null) {
            return true;
        }
        if (hackathon.getRegistrationClosedEarlyAt() != null) {
            return true;
        }
        LocalDate today = LocalDate.now();
        return hackathon.getRegistrationEnd() != null && today.isAfter(hackathon.getRegistrationEnd());
    }

    public static boolean isRegistrationPeriodEnded(Hackathon hackathon) {
        return isRegistrationClosed(hackathon);
    }

    public static boolean allActiveTeamsLocked(List<Team> teams) {
        if (teams == null || teams.isEmpty()) {
            return true;
        }
        return teams.stream()
                .filter(t -> t.getStatus() == TeamStatus.ACTIVE)
                .allMatch(t -> Boolean.TRUE.equals(t.getIsLocked()));
    }

    /**
     * Bốc thăm được phép khi giai đoạn đăng ký đã kết thúc và mọi đội ACTIVE đã khóa.
     * Kết thúc sớm → ngay lập tức; hết hạn tự nhiên → từ ngày sau registrationEnd.
     */
    public static boolean canRunLottery(Hackathon hackathon, List<Team> teams) {
        if (hackathon == null) {
            return false;
        }
        if (!isRegistrationPeriodEnded(hackathon)) {
            return false;
        }
        return allActiveTeamsLocked(teams);
    }
}
