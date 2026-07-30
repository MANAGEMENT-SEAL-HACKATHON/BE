package com.sealhackathon.api.submissions.support;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse {@code https://github.com/{owner}/{repo}} → owner/repo.
 */
public final class GitHubRepoUrlParser {

    private static final Pattern GITHUB_REPO =
            Pattern.compile("^https?://github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)/?$",
                    Pattern.CASE_INSENSITIVE);

    private GitHubRepoUrlParser() {
    }

    public record RepoRef(String owner, String repo) {
        public String fullName() {
            return owner + "/" + repo;
        }
    }

    public static Optional<RepoRef> parse(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            return Optional.empty();
        }
        String trimmed = repoUrl.trim();
        // Strip trailing .git
        if (trimmed.endsWith(".git")) {
            trimmed = trimmed.substring(0, trimmed.length() - 4);
        }
        Matcher m = GITHUB_REPO.matcher(trimmed);
        if (!m.matches()) {
            return Optional.empty();
        }
        String owner = m.group(1);
        String repo = m.group(2);
        if (".".equals(owner) || "..".equals(owner) || ".".equals(repo) || "..".equals(repo)) {
            return Optional.empty();
        }
        return Optional.of(new RepoRef(owner, repo));
    }
}
