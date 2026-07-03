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
            Gd1SeedConstants.SLUG_INCOMPLETE,
            Gd1NoKickoffSeedConstants.SLUG_GD1_NO_KICKOFF,
            Gd1NoAwardsSeedConstants.SLUG_GD1_NO_AWARDS,
            Gd1JudgeFinalEarlySeedConstants.SLUG_GD1_JUDGE_FINAL_EARLY,
            Gd1EventOrderBadSeedConstants.SLUG_GD1_EVENT_ORDER_BAD,
            Gd1EventOrderViolationSeedConstants.SLUG_GD1_EVENT_ORDER_VIOLATION,
            Gd1PrelimOnlySeedConstants.SLUG_GD1_PRELIM_ONLY,
            Gd2TeamsEdgeSeedConstants.SLUG_GD2_TEAMS_EDGE,
            Gd2RegistrationClosedSeedConstants.SLUG_GD2_REGISTRATION_CLOSED,
            Gd2LotteryNotLockedSeedConstants.SLUG_GD2_LOTTERY_NOT_LOCKED,
            Gd2RoundActiveSeedConstants.SLUG_GD2_ROUND_ACTIVE,
            FallOngoingSeedConstants.SLUG_FALL_ONGOING,
            Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN,
            Gd3LateReviewSeedConstants.SLUG_GD3_LATE_REVIEW,
            Gd3ScoringLiveSeedConstants.SLUG_GD3_SCORING_LIVE,
            Gd3ScoringGateSeedConstants.SLUG_GD3_SCORING_GATE,
            Gd3TiebreakHybridSeedConstants.SLUG_GD3_TIEBREAK_HYBRID,
            Gd3EdgeErrorsSeedConstants.SLUG_GD3_EDGE_ERRORS,
            Gd3CalibrationTimerSeedConstants.SLUG_GD3_CALIBRATION_TIMER,
            Gd3JudgeMentorConflictSeedConstants.SLUG_GD3_JUDGE_MENTOR_CONFLICT,
            Gd3RoundConfigEdgeSeedConstants.SLUG_GD3_ROUND_CONFIG_EDGE,
            Gd3NoLotterySeedConstants.SLUG_GD3_NO_LOTTERY,
            Gd3MentorPortalSeedConstants.SLUG_GD3_MENTOR_PORTAL,
            Gd3MentorTrackOnlySeedConstants.SLUG_GD3_MENTOR_TRACK_ONLY,
            Gd3TeamMentorHistorySeedConstants.SLUG_GD3_TEAM_MENTOR_HISTORY,
            Gd4SeedConstants.SLUG_GD4_ADVANCE_READY,
            Gd4CkUnpublishedSeedConstants.SLUG_GD4_CK_UNPUBLISHED,
            Gd4PublishedSeedConstants.SLUG_GD4_PUBLISHED,
            Gd4TiebreakGateSeedConstants.SLUG_GD4_TIEBREAK_GATE,
            Gd4CkActivateReadySeedConstants.SLUG_GD4_CK_ACTIVATE_READY,
            Gd4EdgeErrorsSeedConstants.SLUG_GD4_EDGE_ERRORS,
            Gd4WildcardResolvedSeedConstants.SLUG_GD4_WILDCARD_RESOLVED,
            Gd4TiebreakResolvedSeedConstants.SLUG_GD4_TIEBREAK_RESOLVED,
            Gd4WildcardDisabledSeedConstants.SLUG_GD4_WILDCARD_DISABLED,
            Gd4JudgeAssignWarningsSeedConstants.SLUG_GD4_JUDGE_ASSIGN_WARNINGS,
            Gd4CkNoCriteriaSeedConstants.SLUG_GD4_CK_NO_CRITERIA,
            Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE,
            Gd5SubmitOpenSeedConstants.SLUG_GD5_SUBMIT_OPEN,
            Gd5ScoringLiveSeedConstants.SLUG_GD5_SCORING_LIVE,
            Gd5CalibrationTimerSeedConstants.SLUG_GD5_CALIBRATION_TIMER,
            Gd5EdgeErrorsSeedConstants.SLUG_GD5_EDGE_ERRORS,
            Gd5LateHardlockSeedConstants.SLUG_GD5_LATE_HARDLOCK,
            Gd5JudgeEdgeSeedConstants.SLUG_GD5_JUDGE_EDGE,
            Gd5LatePendingSeedConstants.SLUG_GD5_LATE_PENDING,
            Gd5NotAdvancedSeedConstants.SLUG_GD5_NOT_ADVANCED,
            Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM,
            Gd6PrizesEmptySeedConstants.SLUG_GD6_PRIZES_EMPTY,
            Gd6ConfirmReadySeedConstants.SLUG_GD6_CONFIRM_READY,
            Gd6FinishedExportSeedConstants.SLUG_GD6_FINISHED_EXPORT,
            Gd6EdgeErrorsSeedConstants.SLUG_GD6_EDGE_ERRORS,
            Gd6PrizeDuplicateSeedConstants.SLUG_GD6_PRIZE_DUPLICATE,
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
