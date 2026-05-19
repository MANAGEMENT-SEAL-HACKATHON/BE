package com.sealhackathon.api.users.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-05a v3.1 [FIX-R9] — PATCH /api/v1/users/{userId} (Coordinator).
 * Coordinator phải explicit set {@code isDeptHead} trước khi phân công Trưởng khoa vào Chung kết.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchUserRequest {

    private Boolean isDeptHead;
}
