package com.sealhackathon.api.wildcard_reviews.support;

import java.util.Locale;
import java.util.Set;

/** Plan C — category bắt buộc khi Override sau khi proposal LOCKED. */
public final class WildcardOverrideCategory {

    public static final String PROPOSED_TEAM_VIOLATION = "PROPOSED_TEAM_VIOLATION";
    public static final String TRACK_QUOTA_ADJUST = "TRACK_QUOTA_ADJUST";
    public static final String SCORE_CORRECTED = "SCORE_CORRECTED";
    public static final String OTHER = "OTHER";

    private static final Set<String> ALL = Set.of(
            PROPOSED_TEAM_VIOLATION,
            TRACK_QUOTA_ADJUST,
            SCORE_CORRECTED,
            OTHER);

    private WildcardOverrideCategory() {
    }

    public static boolean isValid(String category) {
        return category != null && ALL.contains(category.trim().toUpperCase(Locale.ROOT));
    }

    public static String normalize(String category) {
        return category == null ? null : category.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean requiresNote(String category) {
        return OTHER.equals(normalize(category));
    }
}
