package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
