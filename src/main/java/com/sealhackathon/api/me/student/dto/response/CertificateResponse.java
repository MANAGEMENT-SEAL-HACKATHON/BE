package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateResponse {

    private Integer id;
    private Integer hackathonId;
    private String hackathonName;
    private LocalDateTime issuedAt;
    private String downloadUrl;
}
