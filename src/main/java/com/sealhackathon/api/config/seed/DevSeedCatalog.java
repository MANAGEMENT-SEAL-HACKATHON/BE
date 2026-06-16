package com.sealhackathon.api.config.seed;

/**
 * Danh mục slug seed dev — một nguồn sự thật.
 *
 * <p>Doc: {@code docs/testing/dev-seed-guide.md}
 *
 * <ul>
 *   <li>{@link #SLUG_E2E_ONGOING} — GĐ1 sẵn sàng, 7 đội + 3 SV chưa có nhóm (test GĐ2→GĐ6)</li>
 *   <li>{@link #SLUG_ARCHIVE_FINISHED} — FINISHED archive duy nhất</li>
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

    /** Slug seed cũ — xóa khi start dev. */
    public static final String[] DEPRECATED_SLUGS = {
            "seal-gd1-incomplete",
            "seal-gd1-ready",
            "seal-spring-2026",
            "seal-spring-2026-gd3",
            "seal-spring-2026-gd4",
            "seal-spring-2026-gd5",
            "seal-spring-2026-gd6",
    };

    public static final String[] ALL_DEV_HACKATHON_SLUGS = {
            SLUG_E2E_ONGOING,
            SLUG_ARCHIVE_FINISHED,
            Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN,
            Gd4SeedConstants.SLUG_GD4_ADVANCE_READY,
            Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE,
            Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM,
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
