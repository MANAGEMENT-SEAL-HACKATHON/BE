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
}
