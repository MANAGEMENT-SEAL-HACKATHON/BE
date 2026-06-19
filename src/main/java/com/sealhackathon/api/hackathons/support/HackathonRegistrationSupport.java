package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.hackathons.entity.Hackathon;

import java.time.LocalDate;

/** Kiểm tra cổng đăng ký / khóa đội theo deadline hoặc kết thúc sớm. */
public final class HackathonRegistrationSupport {

    private HackathonRegistrationSupport() {}

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

    /**
     * Bốc thăm được phép khi giai đoạn đăng ký đã kết thúc và đội ACTIVE đã khóa.
     * Kết thúc sớm → ngay lập tức; hết hạn tự nhiên → từ ngày sau registrationEnd.
     */
    public static boolean canRunLottery(Hackathon hackathon) {
        if (hackathon == null) {
            return false;
        }
        if (hackathon.getRegistrationClosedEarlyAt() != null) {
            return true;
        }
        LocalDate today = LocalDate.now();
        return hackathon.getRegistrationEnd() != null && today.isAfter(hackathon.getRegistrationEnd());
    }
}
