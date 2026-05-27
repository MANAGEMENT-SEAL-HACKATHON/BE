package com.sealhackathon.api.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.oauth")
public class OAuthProperties {

    private String googleClientId = "";
    private String googleTokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo";

    private String githubApiBaseUrl = "https://api.github.com";
    private String githubClientId = "";
    private String githubClientSecret = "";
    private String githubTokenUrl = "https://github.com/login/oauth/access_token";

    /**
     * true: social login nếu chưa linked nhưng trùng email sẽ tự link vào account đó.
     */
    private boolean autoLinkByEmail = true;

    /**
     * true: nếu email social chưa tồn tại trong hệ thống thì tự tạo user mới để login ngay.
     */
    private boolean autoCreateUserOnLogin = true;

    /**
     * true: khi auto-link theo email bắt buộc gửi mật khẩu account hiện có để xác nhận.
     */
    private boolean requirePasswordForAutoLink = false;

    /**
     * true: endpoint link khi đang login cho phép email social khác email account local.
     */
    private boolean allowLinkDifferentEmail = false;
}
