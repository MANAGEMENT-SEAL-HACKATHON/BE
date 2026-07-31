package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentHackathonBrowseItemResponse {

    private Integer id;
    private String name;
    private String status;
    private Boolean registered;
    /** Đã hủy đăng ký giải này — không được đăng ký lại. */
    private Boolean registrationWithdrawn;
    /** Đã đăng ký một giải ONGOING khác — không đăng ký thêm. */
    private Boolean registeredElsewhere;

    // Thông tin hiển thị để tăng chuyển đổi đăng ký
    private String description;
    private String bannerUrl;
    private String season;
    private Integer year;
    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private LocalDate eventStart;
    private LocalDate eventEnd;
    private Integer maxParticipants;
    /** Chưa tới registrationStart. */
    private Boolean registrationNotYetOpen;
    /** Cửa sổ đăng ký đang mở (start ≤ today ≤ end, chưa đóng sớm). */
    private Boolean registrationWindowOpen;
}
