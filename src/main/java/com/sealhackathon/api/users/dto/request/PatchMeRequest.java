package com.sealhackathon.api.users.dto.request;

import com.sealhackathon.api.users.value_object.UserType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatchMeRequest {

    @Size(max = 200)
    private String fullName;

    @Size(max = 30)
    private String phone;

    @Size(max = 2000)
    private String avatarUrl;

    private UserType userType;

    @Size(max = 50)
    private String studentCode;

    private Integer chapterId;

    @Size(max = 300)
    private String institution;
}
