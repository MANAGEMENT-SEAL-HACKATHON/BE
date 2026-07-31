package com.sealhackathon.api.hackathons.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Optional body for POST /api/v1/me/hackathons/{id}/register.
 * All fields optional — callers may omit the body entirely.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterHackathonRequest {

    @Size(max = 10)
    private String preferredShirtSize;

    @Size(max = 20)
    private String preferredShirtFit;
}
