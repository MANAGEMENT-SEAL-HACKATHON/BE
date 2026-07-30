package com.sealhackathon.api.submissions.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubRepoUrlParserTest {

    @Test
    void parsesStandardGithubUrl() {
        var ref = GitHubRepoUrlParser.parse("https://github.com/octocat/Hello-World");
        assertThat(ref).isPresent();
        assertThat(ref.get().owner()).isEqualTo("octocat");
        assertThat(ref.get().repo()).isEqualTo("Hello-World");
        assertThat(ref.get().fullName()).isEqualTo("octocat/Hello-World");
    }

    @Test
    void parsesWithTrailingSlashAndGitSuffix() {
        var ref = GitHubRepoUrlParser.parse("https://github.com/octocat/Hello-World.git");
        assertThat(ref).isPresent();
        assertThat(ref.get().repo()).isEqualTo("Hello-World");
    }

    @Test
    void rejectsNonGithub() {
        assertThat(GitHubRepoUrlParser.parse("https://gitlab.com/o/r")).isEmpty();
        assertThat(GitHubRepoUrlParser.parse("")).isEmpty();
        assertThat(GitHubRepoUrlParser.parse(null)).isEmpty();
    }
}
