package com.sealhackathon.api.submissions.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

@Component
public class GitHubRepoValidator {

    private static final Pattern GITHUB_REPO =
            Pattern.compile("^https?://github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)/?$");

    private final HttpClient httpClient;
    private final boolean publicCheckEnabled;

    public GitHubRepoValidator(
            @Value("${app.submission.github-public-check-enabled:true}") boolean publicCheckEnabled) {
        this.publicCheckEnabled = publicCheckEnabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void validatePublicGitHubRepo(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "repoUrl bắt buộc");
        }
        if (repoUrl.toLowerCase().contains("drive.google.com")) {
            throw new BusinessRuleException(ErrorCode.INVALID_REPO_PLATFORM,
                    "Chỉ chấp nhận GitHub public repository");
        }
        if (!GITHUB_REPO.matcher(repoUrl.trim()).matches()) {
            throw new BusinessRuleException(ErrorCode.INVALID_REPO_PLATFORM,
                    "repoUrl phải là GitHub public: https://github.com/{owner}/{repo}");
        }
        if (!publicCheckEnabled) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(repoUrl.trim()))
                    .timeout(Duration.ofSeconds(8))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", "SealHackathon-BE")
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 404) {
                throw new BusinessRuleException(ErrorCode.REPO_NOT_PUBLIC,
                        "Repository không tồn tại hoặc không public");
            }
            if (response.statusCode() >= 400) {
                throw new BusinessRuleException(ErrorCode.REPO_NOT_PUBLIC,
                        "Không xác minh được repository GitHub (HTTP " + response.statusCode() + ")");
            }
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessRuleException(ErrorCode.REPO_NOT_PUBLIC,
                    "Không kiểm tra được repository GitHub: " + ex.getMessage());
        }
    }
}
