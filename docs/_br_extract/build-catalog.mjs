/**
 * Build BE/docs/business-rules-catalog.md from TSV extracts (B1 grain).
 * Usage: node build-catalog.mjs
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const docsRoot = path.resolve(__dirname, "..");
const outPath = path.join(docsRoot, "business-rules-catalog.md");
const errorCodePath = path.resolve(
  docsRoot,
  "../src/main/java/com/sealhackathon/api/common/exception/ErrorCode.java"
);

const MODULE_PREFIX = {
  Hackathon: "HACK",
  HackathonStatus: "HSTAT",
  Readiness: "READY",
  Registration: "REG",
  RegistrationClose: "REGC",
  Round: "ROUND",
  RoundActivate: "RACT",
  RoundProgression: "RPROG",
  Track: "TRACK",
  TrackRoundRules: "TRR",
  Criteria: "CRIT",
  CriteriaTemplate: "CRITT",
  JudgeAssign: "JUDGE",
  MentorAssign: "MENT",
  Event: "EVENT",
  TempJudge: "TJUDGE",
  Invitation: "INV",
  Auth: "AUTH",
  UserAdmin: "UADMIN",
  Team: "TEAM",
  Lottery: "LOT",
  FormationGrace: "FGRACE",
  TeamLock: "TLOCK",
  DQ: "DQ",
  Release: "REL",
  Submission: "SUB",
  Score: "SCORE",
  Presentation: "PRES",
  Appeal: "APPEAL",
  Calibration: "CALIB",
  Closure: "CLOSE",
  Prize: "PRIZE",
  Ranking: "RANK",
  ChapterRanking: "CHRANK",
  IndividualRanking: "IRANK",
  TeamRanking: "TRANK",
  Export: "EXPORT",
  Rbl: "RBL",
  Announcement: "ANN",
  MentorPortal: "MPORT",
  LiveScoring: "LIVE",
  Archive: "ARCH",
  Certificate: "CERT",
  JudgePortal: "JPORT",
  StudentPortal: "SPORT",
  Common: "COMMON",
};

function escCell(s) {
  if (s == null || s === "") return "";
  return String(s)
    .replace(/\r?\n/g, " ")
    .replace(/\|/g, "\\|")
    .trim();
}

function parseTsv(text) {
  const lines = text.replace(/^\uFEFF/, "").split(/\r?\n/).filter((l) => l.length > 0);
  if (lines.length < 2) return [];
  const header = lines[0].split("\t");
  return lines.slice(1).map((line) => {
    const cols = line.split("\t");
    const row = {};
    header.forEach((h, i) => {
      row[h] = cols[i] ?? "";
    });
    return row;
  });
}

function normalizeModule(m) {
  if (!m) return "Common";
  // Auth.Login → Auth; Closure.Confirm → Closure; Prize.Award → Prize
  const base = m.split(".")[0];
  const map = {
    "Readiness-G1": "Readiness",
    "Readiness-G2": "Readiness",
    "Readiness-G3": "Readiness",
    "Readiness-G4": "Readiness",
    "Readiness-G5": "Readiness",
    Reg: "Registration",
    "Auth": "Auth",
    SubmissionAuth: "Submission",
    SubmissionTeamMember: "Submission",
    SubmissionTeamActive: "Submission",
    SubmissionHackathonFinished: "Submission",
    SubmissionHackathonPendingConfirm: "Submission",
    SubmissionHackathonDraft: "Submission",
    SubmissionHackathonOngoing: "Submission",
    SubmissionCrossHackathon: "Submission",
    SubmissionRoundActive: "Submission",
    SubmissionPrelimRouting: "Submission",
    SubmissionTeamInTrack: "Submission",
    SubmissionPrelimMutable: "Submission",
    SubmissionTeamInFinal: "Submission",
    SubmissionGitHubMultipart: "Submission",
    SubmissionRepoPublicCheck: "Submission",
    SubmissionRepoJsonMode: "Submission",
    SubmissionSlideUrlJson: "Submission",
    SubmissionLateReason: "Submission",
    SubmissionAfterDeadlineFlag: "Submission",
    SubmissionStatusOnTime: "Submission",
    SubmissionStatusHardLockLate: "Submission",
    SubmissionStatusLatePending: "Submission",
    SubmissionPreserveApproved: "Submission",
    SubmissionSubmittedAfterClose: "Submission",
    SubmissionRejectedBlock: "Submission",
    SubmissionSlideRequired: "Submission",
    SubmissionSlidePersistFail: "Submission",
    SubmissionUpsertOnePerScope: "Submission",
    SubmissionNotifyEnqueueMeta: "Submission",
    SubmissionMetadataEnqueue: "Submission",
    SubmissionGradablePolicy: "Submission",
    SubmissionSlideAccess: "Submission",
    SubmissionListAnonJudge: "Submission",
    LateReviewStatusGate: "Submission",
    LateReviewFinalHardLock: "Submission",
    LateReviewRejectNote: "Submission",
    LateReviewApproveReject: "Submission",
    LateReviewQueueAppend: "Submission",
    ScoreGradable: "Score",
    ScoreEliminatedTeam: "Score",
    ScoreMaxCriterion: "Score",
    ScoreJudgeAssigned: "Score",
    ScoreMentorConflict: "Score",
    ScoreLockedRound: "Score",
    ScoreWindowPhase: "Score",
    ScoreWindowPresenting: "Score",
    ScoreWindowTimer: "Score",
    ScoreCriterionScope: "Score",
    ScoreUpsertClearConfirm: "Score",
    ReleaseProblemOnce: "RoundProgression",
    ReleaseProblemExamAt: "RoundProgression",
    ReleaseProblemPrelimTracks: "RoundProgression",
    ReleaseProblemFinalNoUpload: "RoundProgression",
    ReleaseProblemSideEffect: "RoundProgression",
    CloseSubmissionEarlyLocked: "RoundProgression",
    CloseSubmissionEarlyIdempotent: "RoundProgression",
    CloseSubmissionEarlyReleased: "RoundProgression",
    CloseSubmissionEarlyExam: "RoundProgression",
    CloseSubmissionEarlyClamp: "RoundProgression",
    LockScoringAlready: "RoundProgression",
    LockScoringDeadlineGate: "RoundProgression",
    LockScoringShuffledGate: "RoundProgression",
    LockScoringPresentationsComplete: "RoundProgression",
    LockScoringIncompleteForce: "RoundProgression",
    LockScoringForceReasonAlways: "RoundProgression",
    LockScoringSideEffects: "RoundProgression",
    UnlockScoringReason: "RoundProgression",
    UnlockScoringSideEffect: "RoundProgression",
    PublishPrelimOnly: "RoundProgression",
    PublishNeedsLock: "RoundProgression",
    PublishIdempotent: "RoundProgression",
    PublishSideEffect: "RoundProgression",
    RankingNeedsLock: "RoundProgression",
    ScoreboardNeedsPublish: "RoundProgression",
    AdvanceRosterNeedsPublish: "RoundProgression",
    TiebreakDetectPrelim: "RoundProgression",
    TiebreakDetectFinal: "RoundProgression",
    TiebreakEffectiveScore: "RoundProgression",
    TiebreakRuleOrdering: "RoundProgression",
    TiebreakAutoOnLock: "RoundProgression",
    TiebreakResolveNeedsLock: "RoundProgression",
    TiebreakResolveUniqueIds: "RoundProgression",
    TiebreakResolveMatchGroup: "RoundProgression",
    TiebreakResolveRace: "RoundProgression",
    WildcardCandidatesRemoved: "RoundProgression",
    WildcardConfirmNeedsLock: "RoundProgression",
    WildcardConfirmOnce: "RoundProgression",
    WildcardConfirmEnabled: "RoundProgression",
    WildcardConfirmPool: "RoundProgression",
    WildcardConfirmSideEffect: "RoundProgression",
    WildcardOverrideAfterLock: "RoundProgression",
    WildcardOverrideCategory: "RoundProgression",
    WildcardOverrideHistory: "RoundProgression",
    WildcardLegacyDecide: "RoundProgression",
    AdvancePrelimOnly: "RoundProgression",
    AdvanceNeedsLockPublish: "RoundProgression",
    AdvanceTiebreakGate: "RoundProgression",
    AdvanceNeedsFinalRound: "RoundProgression",
    AdvanceNoOverlap: "RoundProgression",
    AdvanceSideEffect: "RoundProgression",
    AdvanceWildcardNoBlock: "RoundProgression",
    AssignFinalJudges: "RoundProgression",
    QueueJudgingPhase: "Presentation",
    QueueShuffleLocked: "Presentation",
    QueueShuffleIdempotent: "Presentation",
    QueueShuffleStarted: "Presentation",
    QueueShuffleGradableOnly: "Presentation",
    QueueControllerAuth: "Presentation",
    QueueViewStudentReg: "Presentation",
    QueueNextEndedOnly: "Presentation",
    QueueNextScoringComplete: "Presentation",
    QueueForceAckAuth: "Presentation",
    QueueNextCompletionRule: "Presentation",
    QueueNextSideEffect: "Presentation",
    QueueSkipNoShow: "Presentation",
    QueueLateAppend: "Presentation",
    QueueHardLockInvariantWarn: "Presentation",
    TimerNeedsJudging: "Presentation",
    TimerNeedsPresentingSlot: "Presentation",
    TimerStartOnce: "Presentation",
    TimerPauseResumeQa: "Presentation",
    TimerEndAutoTimeout: "Presentation",
    DurationFinalNoTrack: "Presentation",
    DurationMutableBeforeStart: "Presentation",
    DurationCascade: "Presentation",
    ControllerGrantTrackRound: "Presentation",
    ControllerRoundFinalOnly: "Presentation",
    ControllerDefaultEarliest: "Presentation",
    AppealLeaderOnly: "Appeal",
    AppealTeamEliminated: "Appeal",
    Appeal24hWindow: "Appeal",
    AppealOncePerRound: "Appeal",
    StudentScoreBreakdownPublish: "StudentPortal",
    JudgeListSubmissionsAssign: "JudgePortal",
    JudgeConfirmScoring: "JudgePortal",
    JudgeScoreCommentLocked: "JudgePortal",
    JudgeTiebreakVotePreLock: "JudgePortal",
    JudgeCompletionOwnOnly: "JudgePortal",
    CalibrationPromptScope: "Calibration",
    CalibrationClosed: "Calibration",
    CalibrationJudgeAssigned: "Calibration",
    CalibrationScoreMax: "Calibration",
    RoundPhaseResolver: "Presentation",
    LateSubmissionPolicyEnum: "Round",
  };
  return map[m] || map[base] || base;
}

function prefixFor(mod) {
  if (MODULE_PREFIX[mod]) return MODULE_PREFIX[mod];
  const up = mod.replace(/[^A-Za-z]/g, "").toUpperCase().slice(0, 6);
  return up || "MISC";
}

function loadAllRows() {
  const files = ["gd1.tsv", "gd2.tsv", "gd35.tsv", "gd6.tsv", "gap-codes.tsv"];
  const rows = [];
  for (const f of files) {
    const p = path.join(__dirname, f);
    if (!fs.existsSync(p)) {
      console.error("Missing", p);
      continue;
    }
    const parsed = parseTsv(fs.readFileSync(p, "utf8"));
    for (const r of parsed) {
      r.Module = normalizeModule(r.Module);
      rows.push(r);
    }
  }
  return rows;
}

function assignIds(rows) {
  const counters = {};
  return rows.map((r) => {
    const mod = r.Module;
    const pref = prefixFor(mod);
    counters[pref] = (counters[pref] || 0) + 1;
    const n = String(counters[pref]).padStart(3, "0");
    return { ...r, RuleID: `BR-${pref}-${n}` };
  });
}

function extractErrorCodesFromJava(src) {
  const re = /public static final String\s+(\w+)\s*=\s*"([^"]+)"/g;
  const codes = [];
  let m;
  while ((m = re.exec(src))) {
    codes.push({ constName: m[1], value: m[2] });
  }
  return codes;
}

function collectReferencedCodes(rows) {
  const set = new Set();
  for (const r of rows) {
    const raw = r.ExceptionErrorCode || "";
    if (!raw || raw === "N/A") continue;
    // split on ; | / , and spaces around
    for (const part of raw.split(/[;|,/]+/)) {
      const c = part.trim().replace(/\(.*?\)/g, "").trim();
      if (/^[A-Z][A-Z0-9_]+$/.test(c)) set.add(c);
    }
  }
  return set;
}

function sectionOrder(rows) {
  const order = [
    "Auth",
    "UserAdmin",
    "Hackathon",
    "HackathonStatus",
    "Readiness",
    "Registration",
    "RegistrationClose",
    "Round",
    "RoundActivate",
    "Track",
    "TrackRoundRules",
    "Criteria",
    "CriteriaTemplate",
    "JudgeAssign",
    "MentorAssign",
    "Event",
    "TempJudge",
    "Invitation",
    "Team",
    "TeamLock",
    "FormationGrace",
    "Lottery",
    "DQ",
    "Release",
    "Submission",
    "Score",
    "RoundProgression",
    "Presentation",
    "Appeal",
    "Calibration",
    "JudgePortal",
    "StudentPortal",
    "MentorPortal",
    "Closure",
    "Prize",
    "TeamRanking",
    "ChapterRanking",
    "IndividualRanking",
    "Ranking",
    "Export",
    "Rbl",
    "Announcement",
    "LiveScoring",
    "Archive",
    "Certificate",
    "Common",
  ];
  const byMod = new Map();
  for (const r of rows) {
    if (!byMod.has(r.Module)) byMod.set(r.Module, []);
    byMod.get(r.Module).push(r);
  }
  const result = [];
  const seen = new Set();
  for (const m of order) {
    if (byMod.has(m)) {
      result.push([m, byMod.get(m)]);
      seen.add(m);
    }
  }
  for (const [m, list] of [...byMod.entries()].sort((a, b) => a[0].localeCompare(b[0]))) {
    if (!seen.has(m)) result.push([m, list]);
  }
  return result;
}

function buildMarkdown(rows, allCodes, usedInCatalog, usedInSrc) {
  const today = "2026-07-21";
  const sections = sectionOrder(rows);
  const lines = [];

  lines.push("# Business Rules Catalog — SEAL Hackathon Backend");
  lines.push("");
  lines.push(`**Generated:** ${today} · **Grain:** B1 (1 rule ≈ 1 ErrorCode / guard / invariant) · **Source of truth:** Java BE enforce`);
  lines.push("");
  lines.push("## Mục đích");
  lines.push("");
  lines.push("Catalog thống nhất các business rules đang được enforce trong backend, dạng bảng 11 cột để BA/QA/Dev truy xuất. Tài liệu MF theo giai đoạn (`mf01/01-business-rules.md`, `mf02/01-business-rules-gd2.md`, `mf03/01-business-rules-gd3.md`, `mf03/01-business-rules-gd6.md`) **vẫn giữ nguyên** — file này bổ sung, không thay thế.");
  lines.push("");
  lines.push("## Nguồn sự thật & ưu tiên khi mâu thuẫn");
  lines.push("");
  lines.push("1. `docs/db/schema-v3.0-mysql.md` (constraint / trigger DB)");
  lines.push("2. **Code enforce** (`*ServiceImpl`, `*Rules`, `*Policy`, `*Validator`, `ErrorCode`)");
  lines.push("3. MF business-rules docs theo giai đoạn");
  lines.push("4. `docs/system/workflow.md`");
  lines.push("");
  lines.push("**Related Req ID** map tới `FR-*` trong [`docs/FUNCTIONAL-REQUIREMENTS-BACKLOG.md`](../../docs/FUNCTIONAL-REQUIREMENTS-BACKLOG.md). Rule chỉ có trong code → `N/A`.");
  lines.push("");
  lines.push("## Quy ước");
  lines.push("");
  lines.push("| Trường | Ý nghĩa |");
  lines.push("|--------|---------|");
  lines.push("| Rule ID | `BR-{MODULE}-{NNN}` — ổn định trong file này |");
  lines.push("| Related Req ID | `FR-*` hoặc `N/A` |");
  lines.push("| Module | Nhóm nghiệp vụ (Auth, Round, Team, …) |");
  lines.push("| Rule Type | `Validation` · `Authorization` · `StateTransition` · `Invariant` · `SideEffect` · `Gate` · `Policy` · `Scheduler` · `Lifecycle` · `Access` · `Audit` · `Calculation` · `Design` · `Gap` |");
  lines.push("| Status | `Implemented` · `Partial` · `Spec-only` · `Deprecated` · `Gap` · `Removed/Disabled` — theo **code** |");
  lines.push("| Evidence | Path Java tương đối dưới `src/main/java/com/sealhackathon/api/` |");
  lines.push("");
  lines.push("## Thống kê");
  lines.push("");
  lines.push(`| Metric | Giá trị |`);
  lines.push(`|--------|--------:|`);
  lines.push(`| Tổng rules | ${rows.length} |`);
  lines.push(`| Modules | ${sections.length} |`);
  lines.push(`| ErrorCode constants | ${allCodes.length} |`);
  lines.push(`| ErrorCode xuất hiện trong catalog | ${usedInCatalog.size} |`);
  lines.push(`| ErrorCode được reference trong src | ${usedInSrc.size} |`);
  lines.push("");
  lines.push("## Mục lục theo Module");
  lines.push("");
  for (const [mod, list] of sections) {
    const anchor = mod.toLowerCase().replace(/[^a-z0-9]+/g, "-");
    lines.push(`- [${mod}](#${anchor}) (${list.length})`);
  }
  lines.push("- [Appendix A — ErrorCode orphan / unused](#appendix-a--errorcode-orphan--unused)");
  lines.push("- [Appendix B — FR collisions & doc drift](#appendix-b--fr-collisions--doc-drift)");
  lines.push("- [Appendix C — Module → Java package](#appendix-c--module--java-package)");
  lines.push("");

  const header =
    "| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |";
  const sep =
    "|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|";

  for (const [mod, list] of sections) {
    lines.push(`## ${mod}`);
    lines.push("");
    lines.push(header);
    lines.push(sep);
    for (const r of list) {
      const cells = [
        r.RuleID,
        r.RelatedReqID || "N/A",
        r.Module,
        r.RuleType || "",
        r.Statement || "",
        r.ConditionTrigger || "",
        r.SystemAction || "",
        r.ExceptionErrorCode || "N/A",
        r.TestHint || "",
        r.Status || "",
        r.EvidencePath || "",
      ].map(escCell);
      lines.push(`| ${cells.join(" | ")} |`);
    }
    lines.push("");
  }

  // Appendix A
  const allConstValues = new Set(allCodes.map((c) => c.value));
  const orphanDeclared = [...allConstValues].filter((c) => !usedInSrc.has(c)).sort();
  const inSrcNotCatalog = [...usedInSrc].filter((c) => !usedInCatalog.has(c) && allConstValues.has(c)).sort();
  const inCatalogNotDeclared = [...usedInCatalog].filter((c) => !allConstValues.has(c)).sort();

  lines.push("## Appendix A — ErrorCode orphan / unused");
  lines.push("");
  lines.push(`### A.1 Declared in \`ErrorCode.java\` nhưng không thấy reference \`ErrorCode.*\` trong \`src/main/java\` (${orphanDeclared.length})`);
  lines.push("");
  if (orphanDeclared.length === 0) {
    lines.push("_Không có._");
  } else {
    lines.push("| ErrorCode | Ghi chú |");
    lines.push("|-----------|---------|");
    for (const c of orphanDeclared) {
      let note = "Có thể chỉ dùng string literal / chưa wire / reserved";
      if (c === "EXPORT_JOB_NOT_READY") note = "Download dùng INVALID_STATE thay thế";
      if (c === "ROUND_HAS_SCORES") note = "Docs FR-13C yêu cầu khi removeMentor — code chưa enforce (Gap)";
      if (c === "WILDCARD_PENDING") note = "Advance không còn block WC (WC-MIG)";
      if (c === "NOT_IMPLEMENTED") note = "Reserved / legacy stub";
      if (c === "PRIZE_CATALOG_LOCKED") note = "Reserved — chưa thấy throw";
      if (c === "DEPT_HEAD_NOT_CONFIRMED") note = "PATCH isDeptHead đã ngừng (INVALID_ASSIGNMENT_TYPE)";
      if (c === "TEAM_LEADER_NOT_APPROVED") note = "createTeam không check APPROVED (Gap vs docs)";
      lines.push(`| \`${c}\` | ${note} |`);
    }
  }
  lines.push("");
  lines.push(`### A.2 Có reference trong src nhưng chưa gắn rule riêng trong catalog (${inSrcNotCatalog.length})`);
  lines.push("");
  if (inSrcNotCatalog.length === 0) {
    lines.push("_Không có (hoặc đã cover qua rule khác)._");
  } else {
    lines.push(inSrcNotCatalog.map((c) => `\`${c}\``).join(", "));
  }
  lines.push("");
  if (inCatalogNotDeclared.length) {
    lines.push(`### A.3 Mã trong catalog không phải constant \`ErrorCode\` (${inCatalogNotDeclared.length})`);
    lines.push("");
    lines.push(inCatalogNotDeclared.map((c) => `\`${c}\``).join(", "));
    lines.push("");
    lines.push("_Thường là string literal runtime (vd. `APPEAL_DEADLINE_EXPIRED`, `RESULT_NOT_AVAILABLE`, warning codes)._");
    lines.push("");
  }

  lines.push("## Appendix B — FR collisions & doc drift");
  lines.push("");
  lines.push("| Issue | Chi tiết |");
  lines.push("|-------|----------|");
  lines.push("| FR-07 collision | Backlog dùng `FR-07` cho **Hackathon status** (GĐ1) và **Auth register/login** (GĐ2). Catalog gắn theo ngữ cảnh module. |");
  lines.push("| FR-05A vs FR-05a | Guest judge / temp account — dùng `FR-05A`. |");
  lines.push("| mf02 BR doc stale | `mf02/01-business-rules-gd2.md` vẫn ghi TODO/501 — **code đã implement** (Status=Implemented). |");
  lines.push("| Student PENDING login | Code cho phép STUDENT PENDING login sau verify email; docs auth cũ có thể nói chỉ APPROVED. |");
  lines.push("| createTeam gaps | Student create không bắt buộc đã register / APPROVED / prelim inactive (admin path có `ROUND_ALREADY_ACTIVE`). |");
  lines.push("| removeMentor | Docs yêu cầu `ROUND_HAS_SCORES` — code chưa check → Status Gap trên rule liên quan. |");
  lines.push("| Team rankings status gate | Docs: chỉ PENDING_CONFIRM+; **code** `FinalRankingQueryServiceImpl` không gate status. |");
  lines.push("| EXPORT_JOB_NOT_READY | Constant tồn tại; download dùng `INVALID_STATE`. |");
  lines.push("| Wildcard candidates | `wildcardCandidates` luôn empty (product disabled); confirm/override còn code. |");
  lines.push("| Mentor chapter AVG vs official SUM | Mentor portal AVG; `ChapterRankingServiceImpl` SUM. |");
  lines.push("| RESULT_PUBLISHED batch notify | TODO trong `HackathonFinishedEventListener` (announcement STOMP đã có). |");
  lines.push("");

  lines.push("## Appendix C — Module → Java package");
  lines.push("");
  lines.push("| Module | Package / entry |");
  lines.push("|--------|-----------------|");
  lines.push("| Auth | `auth.service.*` |");
  lines.push("| UserAdmin | `users.service.impl.UserAdminServiceImpl` |");
  lines.push("| Hackathon / Status / Readiness / Registration | `hackathons.service.impl.*` |");
  lines.push("| Round / Activate / Progression | `rounds.service.impl.*` |");
  lines.push("| Track | `tracks.service.impl` + `tracks.support.TrackRoundRules` |");
  lines.push("| Criteria | `criteria.service.impl.*` |");
  lines.push("| JudgeAssign / MentorAssign | `judge_assignments` / `mentors` + `PersonnelAssignmentRules` |");
  lines.push("| Event | `events.service.impl.*` |");
  lines.push("| Team / Lottery / Lock | `teams.service.impl.*` / `HackathonLotteryServiceImpl` |");
  lines.push("| Submission / Score | `submissions.*` / `scores.*` |");
  lines.push("| Presentation | `presentation.service.impl.*` |");
  lines.push("| Closure / Prize / Ranking / Export | `HackathonClosureServiceImpl` / `prizes` / rankings / `export_jobs` |");
  lines.push("| RBL / Calibration | `rbl.service` / `rbl.calibration` |");
  lines.push("| Portals | `me.student` / `me.judge` / `me.mentor` |");
  lines.push("");
  lines.push("---");
  lines.push("");
  lines.push("_Regenerate extracts: cập nhật TSV trong `docs/_br_extract/` rồi chạy `node docs/_br_extract/build-catalog.mjs`._");
  lines.push("");

  return lines.join("\n");
}

function walkJavaFiles(dir, acc = []) {
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) walkJavaFiles(p, acc);
    else if (ent.name.endsWith(".java")) acc.push(p);
  }
  return acc;
}

function collectUsedInSrc(constToValue) {
  const srcRoot = path.resolve(docsRoot, "../src/main/java");
  const used = new Set();
  const re = /ErrorCode\.([A-Z][A-Z0-9_]+)/g;
  for (const file of walkJavaFiles(srcRoot)) {
    const text = fs.readFileSync(file, "utf8");
    let m;
    while ((m = re.exec(text))) {
      used.add(constToValue.get(m[1]) || m[1]);
    }
  }
  return used;
}

/** Normalize Exception column: constant name → runtime string value when known. */
function normalizeExceptionCodes(rows, constToValue) {
  return rows.map((r) => {
    const raw = r.ExceptionErrorCode || "";
    if (!raw || raw === "N/A") return r;
    const parts = raw.split(/([;|,/]+)/).map((part) => {
      if (/^[;|,/]+$/.test(part) || !part.trim()) return part;
      const token = part.trim().replace(/\(.*?\)/g, "").trim();
      if (constToValue.has(token)) return part.replace(token, constToValue.get(token));
      return part;
    });
    return { ...r, ExceptionErrorCode: parts.join("") };
  });
}

function main() {
  let rows = loadAllRows();
  const javaSrc = fs.readFileSync(errorCodePath, "utf8");
  const allCodes = extractErrorCodesFromJava(javaSrc);
  const constToValue = new Map(allCodes.map((c) => [c.constName, c.value]));
  rows = normalizeExceptionCodes(rows, constToValue);
  rows = assignIds(rows);

  const usedInCatalog = collectReferencedCodes(rows);
  const usedInSrc = collectUsedInSrc(constToValue);

  const md = buildMarkdown(rows, allCodes, usedInCatalog, usedInSrc);
  fs.writeFileSync(outPath, md, "utf8");
  console.log(`Wrote ${outPath}`);
  console.log(`Rules: ${rows.length}; modules: ${sectionOrder(rows).length}`);
  console.log(`ErrorCodes declared: ${allCodes.length}; in catalog: ${usedInCatalog.size}; in src: ${usedInSrc.size}`);
}

main();
