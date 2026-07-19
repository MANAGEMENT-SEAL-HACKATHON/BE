package com.sealhackathon.api.auth.dto.request;

import com.sealhackathon.api.users.value_object.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @Size(max = 200)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 320)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(min = 8, max = 100)
    private String confirmPassword;

    @NotNull
    private UserType userType;

    @Size(max = 50)
    private String studentCode;

    private Integer chapterId;

    @Size(max = 300)
    private String institution;
}
