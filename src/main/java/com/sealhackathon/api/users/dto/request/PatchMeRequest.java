package com.sealhackathon.api.users.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatchMeRequest {

    @Size(max = 30)
    private String phone;

    @Size(max = 2000)
    private String avatarUrl;
}
