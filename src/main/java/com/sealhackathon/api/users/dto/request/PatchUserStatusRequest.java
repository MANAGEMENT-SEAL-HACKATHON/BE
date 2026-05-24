package com.sealhackathon.api.users.dto.request;

import com.sealhackathon.api.users.value_object.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatchUserStatusRequest {

    @NotNull
    private UserStatus status;

    /** Bắt buộc khi status = REJECTED. */
    private String rejectionReason;

    /** Bắt buộc khi REJECTED → PENDING (override). */
    private String overrideReason;
}
