package com.sealhackathon.api.submissions.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubRepoValidatorTest {

    private final GitHubRepoValidator validator = new GitHubRepoValidator(false);

    @Test
    void rejectsNonGitHubUrl() {
        assertThatThrownBy(() -> validator.validatePublicGitHubRepo("https://gitlab.com/o/r"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_REPO_PLATFORM);
    }

    @Test
    void rejectsGoogleDrive() {
        assertThatThrownBy(() -> validator.validatePublicGitHubRepo("https://drive.google.com/file/d/x"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_REPO_PLATFORM);
    }

    @Test
    void acceptsGitHubUrlWhenPublicCheckDisabled() {
        assertThatCode(() -> validator.validatePublicGitHubRepo("https://github.com/octocat/Hello-World"))
                .doesNotThrowAnyException();
    }

    @Test
    void requiresRepoUrl() {
        assertThatThrownBy(() -> validator.validatePublicGitHubRepo(" "))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }
}
