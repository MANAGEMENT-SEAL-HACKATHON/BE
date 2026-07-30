package com.sealhackathon.api.config.seed;

/**
 * Danh mục slug seed dev — một nguồn sự thật.
 *
 * <p>Doc: {@code docs/testing/dev-seed-guide.md} · {@code docs/testing/dev-seed-slugs-guide.md}
 *
 * <ul>
 *   <li>{@link #SLUG_E2E_ONGOING} — GĐ1–GĐ2 happy + continuous</li>
 *   <li>{@link #SLUG_ARCHIVE_FINISHED} — FINISHED archive duy nhất</li>
 *   <li>GĐ3–GĐ6: mỗi giai đoạn một happy slug</li>
 * </ul>
 */
public final class DevSeedCatalog {

    private DevSeedCatalog() {
    }

    public static final String DEV_STUDENT_PASSWORD = "Student@dev1";

    /** Số thành viên ACCEPTED tối thiểu mỗi đội seed. */
    public static final int MEMBERS_PER_TEAM = 3;

    public static final int ORPHAN_COUNT = 3;

    public static final String SLUG_E2E_ONGOING = Gd1SeedConstants.SLUG_ONGOING;

    public static final String SLUG_ARCHIVE_FINISHED = Gd1SeedConstants.SLUG_FINISHED;

    public static final String SLUG_GD3_PRELIM_OPEN = Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN;

    public static final String SLUG_GD4_ADVANCE_READY = Gd4SeedConstants.SLUG_GD4_ADVANCE_READY;

    public static final String SLUG_GD4_TIEBREAK_SUBMISSION_TIME =
            Gd4TiebreakSeedConstants.SLUG_GD4_TIEBREAK_SUBMISSION_TIME;

    public static final String SLUG_GD4_TIEBREAK_MANUAL =
            Gd4TiebreakSeedConstants.SLUG_GD4_TIEBREAK_MANUAL;

    public static final String SLUG_GD5_FINAL_ACTIVE = Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE;

    public static final String SLUG_GD6_PENDING_CONFIRM = Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM;

    /**
     * Slug seed cũ / bad / mid-stage — xóa khi start profile {@code dev}.
     */
    public static final String[] DEPRECATED_SLUGS = {
            // legacy spring / ready
            "seal-gd1-ready",
            "seal-spring-2026",
            "seal-spring-2026-gd3",
            "seal-spring-2026-gd4",
            "seal-spring-2026-gd5",
            "seal-spring-2026-gd6",
            // GĐ1 bad/gate
            Gd1SeedConstants.SLUG_INCOMPLETE,
            "seal-gd1-no-kickoff",
            "seal-gd1-no-awards",
            "seal-gd1-judge-final-early",
            "seal-gd1-event-order-bad",
            "seal-gd1-event-order-violation",
            "seal-gd1-prelim-only",
            // GĐ2 bad/gate + Fall ongoing
            "seal-gd2-teams-edge",
            "seal-gd2-registration-closed",
            "seal-gd2-lottery-not-locked",
            "seal-gd2-round-active",
            "seal-fall-ongoing-2026",
            // GĐ3 mid/bad
            "seal-gd3-late-review",
            "seal-gd3-scoring-live",
            "seal-gd3-scoring-gate",
            "seal-gd3-tiebreak-hybrid",
            "seal-gd3-edge-errors",
            "seal-gd3-judge-mentor-conflict",
            "seal-gd3-round-config-edge",
            "seal-gd3-no-lottery",
            "seal-gd3-mentor-portal",
            "seal-gd3-mentor-track-only",
            "seal-gd3-team-mentor-history",
            // GĐ4 mid/bad
            "seal-gd4-ck-unpublished",
            "seal-gd4-published",
            "seal-gd4-tiebreak-gate",
            "seal-gd4-ck-activate-ready",
            "seal-gd4-edge-errors",
            "seal-gd4-wildcard-resolved",
            "seal-gd4-tiebreak-manual",
            "seal-gd4-tiebreak-submission-time",
            "seal-gd4-wildcard-gap",
            "seal-gd4-tiebreak-resolved",
            "seal-gd4-wildcard-disabled",
            "seal-gd4-judge-assign-warnings",
            "seal-gd4-ck-no-criteria",
            // GĐ5 mid/bad
            "seal-gd5-submit-open",
            "seal-gd5-scoring-live",
            "seal-gd5-edge-errors",
            "seal-gd5-late-hardlock",
            "seal-gd5-judge-edge",
            "seal-gd5-late-pending",
            "seal-gd5-not-advanced",
            // GĐ6 mid/bad
            "seal-gd6-prizes-empty",
            "seal-gd6-confirm-ready",
            "seal-gd6-finished-export",
            "seal-gd6-edge-errors",
            "seal-gd6-prize-duplicate",
    };

    /** Happy-path hackathon slugs seed khi start {@code dev}. */
    public static final String[] ALL_DEV_HACKATHON_SLUGS = {
            SLUG_E2E_ONGOING,
            SLUG_ARCHIVE_FINISHED,
            SLUG_GD3_PRELIM_OPEN,
            SLUG_GD4_ADVANCE_READY,
            SLUG_GD5_FINAL_ACTIVE,
            SLUG_GD6_PENDING_CONFIRM,
    };

    public static final SnapshotProfile PROFILE_E2E = new SnapshotProfile(
            "e2e", "E2E-", 7, 3, 2);

    public static final String TEAM_MARKER = PROFILE_E2E.teamMarker();

    public static String orphanEmail(int index) {
        return "student.e2e.orphan%d@fpt.edu.vn".formatted(index);
    }

    public static String orphanDisplayName(int index) {
        return "E2E Orphan %d".formatted(index);
    }

    public static int[] distributeTeams(int totalTeams, int trackCount) {
        if (trackCount <= 0 || totalTeams < 0) {
            throw new IllegalArgumentException("trackCount > 0 và totalTeams >= 0");
        }
        int base = totalTeams / trackCount;
        int remainder = totalTeams % trackCount;
        int[] counts = new int[trackCount];
        for (int i = 0; i < trackCount; i++) {
            counts[i] = base + (i < remainder ? 1 : 0);
        }
        return counts;
    }

    public record SnapshotProfile(
            String studentKey,
            String teamPrefix,
            int teamCount,
            int trackCount,
            int topNAdvance) {

        public String teamMarker() {
            return teamPrefix + "T01";
        }

        public String teamName(int teamIndex) {
            return teamPrefix + String.format("T%02d", teamIndex);
        }

        public int[] teamsPerTrack() {
            return distributeTeams(teamCount, trackCount);
        }

        public String studentEmail(int teamIndex, int memberIndex) {
            if (memberIndex == 1) {
                return "student.%s.t%02d.leader@fpt.edu.vn".formatted(studentKey, teamIndex);
            }
            return "student.%s.t%02d.m%d@fpt.edu.vn".formatted(studentKey, teamIndex, memberIndex);
        }

        public String displayName(int teamIndex, int memberIndex) {
            String label = teamPrefix.replace("-", "");
            if (memberIndex == 1) {
                return "%s T%02d Leader".formatted(label, teamIndex);
            }
            return "%s T%02d M%d".formatted(label, teamIndex, memberIndex);
        }

        public String distributionLabel() {
            int[] counts = teamsPerTrack();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < counts.length; i++) {
                if (i > 0) {
                    sb.append('+');
                }
                sb.append(counts[i]);
            }
            return teamCount + " đội × " + trackCount + " track (" + sb + ")";
        }
    }
}
