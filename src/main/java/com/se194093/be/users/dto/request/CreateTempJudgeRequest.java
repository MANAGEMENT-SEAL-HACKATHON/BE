package com.se194093.be.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-05a POST /api/v1/users/temp-judges — tạo Judge khách mời.
 *
 * <p>Service set cố định:
 * <ul>
 *   <li>{@code role = JUDGE}</li>
 *   <li>{@code userType = EXTERNAL}</li>
 *   <li>{@code isTempAccount = TRUE}</li>
 *   <li>{@code status = APPROVED} (Coordinator approve trực tiếp)</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTempJudgeRequest {

    @NotBlank
    @Size(max = 200)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 320)
    private String email;

    @NotBlank
    @Size(max = 300)
    private String institution;

    @Size(max = 30)
    private String phone;
}
