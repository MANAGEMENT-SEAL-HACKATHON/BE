#!/usr/bin/env node
/**
 * Build FUNCTIONAL-REQUIREMENTS-BACKLOG.md from MF docs, test matrices, audit reports, and code maps.
 * Usage: node BE/docs/testing/scripts/build-fr-backlog-inventory.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '../../../..');
const DOCS = path.join(ROOT, 'docs');
const BE_DOCS = path.join(ROOT, 'BE', 'docs');
const FE_E2E = path.join(ROOT, 'seal-hackathon-fe', 'e2e');

const OUT_JSON = path.join(DOCS, '.backlog-inventory.json');
const OUT_MD = path.join(DOCS, 'FUNCTIONAL-REQUIREMENTS-BACKLOG.md');

function read(relFromRoot) {
  const p = path.join(ROOT, relFromRoot);
  return fs.existsSync(p) ? fs.readFileSync(p, 'utf8') : '';
}

function escCell(s) {
  return String(s ?? '')
    .replace(/\|/g, '\\|')
    .replace(/\r?\n/g, ' ')
    .trim();
}

/** Curated FR catalog — normative from MF-01/02/03 + business rules */
const FR_CATALOG = [
  // GĐ1 Setup
  { id: 'FR-01', module: 'GD1-Setup', title: 'Tao Hackathon moi', actor: 'Coordinator', priority: 'P1', phase: 'GD1',
    ac: 'POST /hackathons → 201 DRAFT; UNIQUE(name,season,year); date range valid',
    api: 'POST /api/v1/hackathons', ui: '/hackathons/create → CreateHackathonPage.jsx',
    test: 'TC-GD1-H01; dev-seed-matrix; playbook §1', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf01/02-functional-requirements.md §2; HackathonController.java' },
  { id: 'FR-02', module: 'GD1-Setup', title: 'Tao / cau hinh Round', actor: 'Coordinator', priority: 'P1', phase: 'GD1',
    ac: 'Round thuoc hackathon; 1 PRELIM + 1 FINAL; exam_at ordering; chk_round_type_final',
    api: 'POST /hackathons/{id}/rounds', ui: '/hackathons/:id/setup?tab=rounds → RoundManagementPage.jsx',
    test: 'TC-GD1-H02; G1-ROUND-03', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf01/02-functional-requirements.md §3' },
  { id: 'FR-03', module: 'GD1-Setup', title: 'Tao Track trong Round', actor: 'Coordinator', priority: 'P1', phase: 'GD1',
    ac: 'Track chi trong round non-FINAL; assigned_group unique per round',
    api: 'POST /rounds/{id}/tracks', ui: '/hackathons/:id/setup?tab=tracks → TrackManagementPage.jsx',
    test: 'TC-GD1-H03', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf01/02-functional-requirements.md §4' },
  { id: 'FR-04', module: 'GD1-Setup', title: 'Thiet lap Criteria', actor: 'Coordinator', priority: 'P1', phase: 'GD1',
    ac: 'XOR track_id (prelim) vs round_id (final); weight sum = 1.0 per scope',
    api: 'POST /tracks/{id}/criteria; POST /rounds/{id}/criteria', ui: 'CriteriaManagementPage.jsx',
    test: 'TC-GD1-H04/H05; H-FORM-CRITERIA', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf01/api/fr-04-criteria.md' },
  { id: 'FR-05', module: 'GD1-Setup', title: 'Quan ly nhan su (Judge/Mentor)', actor: 'Coordinator', priority: 'P1', phase: 'GD1',
    ac: 'CONFLICT_SAME_TRACK block; mentor-judge isolation; FINAL_EXTERNAL for CK',
    api: 'POST /judge-assignments; POST /mentor-assignments', ui: '/hackathons/:id/setup?tab=people → PeopleManagementPage.jsx',
    test: 'TC-GD1-H06/H07/H11; G1-JUDGE-04; Personnel Guard', scenario: 'Happy/Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf01/api/fr-05-personnel.md' },
  { id: 'FR-05A', module: 'GD1-Setup', title: 'Guest Judge (temp) onboard', actor: 'Coordinator', priority: 'P1', phase: 'GD1',
    ac: 'PENDING + mustChangePassword; resend invitation; APPROVED after change-password',
    api: 'POST /users/temp-judges; POST /invitations/{id}/resend', ui: '/admin/temp-judges → TempJudgesPage.jsx',
    test: 'TC-GD1-H14; account-states.spec.js', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/05-register-to-approved-flow-handover.md' },
  { id: 'FR-06', module: 'GD1-Setup', title: 'Len lich su kien (Events)', actor: 'Coordinator', priority: 'P1', phase: 'GD1',
    ac: 'POST order KICKOFF→WORKSHOP→AWARDS; EVENT_REMINDER fan-out',
    api: 'POST /hackathons/{id}/events', ui: '/hackathons/:id/setup?tab=events → EventManagementPage.jsx',
    test: 'TC-GD1-H08; G1-E01..E03; event-notification-mutating.spec.js', scenario: 'Happy/Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf01/api/fr-06a-events.md' },
  { id: 'FR-07', module: 'Auth', title: 'Dang ky tai khoan STUDENT (rut gon)', actor: 'Student', priority: 'P1', phase: 'GD2',
    ac: 'Email+password+confirmPassword only; userType=UNSPECIFIED; email verification; onboarding sau login',
    api: 'POST /auth/register', ui: '/register → RegisterPage.jsx',
    test: 'AuthOnboardingFlowIntegrationTest; abuse-guards duplicate-email', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/01-auth-users.md; RegistrationService.java (2026-07-20 restore)' },
  { id: 'FR-07', module: 'GD1-Setup', title: 'Chuyen trang thai Hackathon (DRAFT→ONGOING→…)', actor: 'Coordinator', priority: 'P1', phase: 'GD1',
    ac: 'Gate G1-G5 readiness; PATCH status; PENDING_CONFIRM→FINISHED via confirm',
    api: 'GET /hackathons/{id}/readiness; PATCH /hackathons/{id}/status', ui: 'HackathonSetupPage header activate',
    test: 'TC-GD1-H09/H10; G1-E02/E03', scenario: 'Happy/Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf01/api/fr-06-status.md; note: same FR-07 ID as auth — cross-ref' },
  { id: 'FR-07B', module: 'GD1-Setup', title: 'Safety net validate weight khi activate Round', actor: 'Coordinator', priority: 'P1', phase: 'GD1',
    ac: 'Activate block neu weight != 1.0 hoac thieu criteria/judge',
    api: 'PATCH /rounds/{id}/activate', ui: 'RoundManagementPage activate button',
    test: 'RoundActivationServiceImplTest', scenario: 'Bad', owner: 'BE', status: 'Done',
    evidence: 'BE/docs/mf01/02-functional-requirements.md §9' },
  { id: 'FR-08', module: 'Auth', title: 'Coordinator duyet tai khoan STUDENT', actor: 'Coordinator', priority: 'P1', phase: 'GD2',
    ac: 'PENDING→APPROVED/REJECTED; student login blocked until approved',
    api: 'PATCH /users/{id}/status', ui: '/admin/users → UserApprovalPage.jsx',
    test: 'AuthOnboardingFlowIntegrationTest', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/01-auth-users.md' },
  { id: 'FR-09', module: 'Auth', title: 'JWT login / refresh', actor: 'All', priority: 'P1', phase: 'GD2',
    ac: 'POST login → access+refresh; OAuth Google/GitHub optional',
    api: 'POST /auth/login; POST /auth/refresh', ui: '/login → LoginPage.jsx',
    test: 'smoke-login.spec.js; auth-recovery.spec.js', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'AuthController.java' },
  { id: 'FR-10', module: 'GD2-Teams', title: 'Hackathon registration (student register event)', actor: 'Student', priority: 'P1', phase: 'GD2',
    ac: 'POST /me/hackathons/{id}/register when ONGOING',
    api: 'POST /me/hackathons/{id}/register', ui: '/student/hackathons → StudentHackathonHistoryPage.jsx',
    test: 'mode-b-continuous-ui.spec.js registerStudentForHackathon', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/02-mainflow-gd2.md' },
  { id: 'FR-11', module: 'GD2-Teams', title: 'Tao doi', actor: 'Student', priority: 'P1', phase: 'GD2',
    ac: 'Leader tao team khi ONGOING; auto-navigate sau tao',
    api: 'POST /teams', ui: '/student/team → StudentTeamPage.jsx',
    test: 'G2-TEAM-01; e2e-gd2-e2e-2026.spec.js', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/01-business-rules-gd2.md §2' },
  { id: 'FR-11C', module: 'GD2-Teams', title: 'Transfer Leader', actor: 'Student', priority: 'P2', phase: 'GD2',
    ac: 'Leader chuyen quyen cho member ACTIVE',
    api: 'PATCH /teams/{id}/transfer-leader', ui: 'StudentTeamPage.jsx',
    test: 'playbook GD2', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/01-business-rules-gd2.md §3' },
  { id: 'FR-11D', module: 'GD2-Teams', title: 'Giai tan doi (Disband)', actor: 'Student', priority: 'P1', phase: 'GD2',
    ac: 'Members released; co the tao doi moi khong F5',
    api: 'PATCH /teams/{id}/disband', ui: 'StudentTeamPage.jsx',
    test: 'G2-TEAM-01', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/01-business-rules-gd2.md §4' },
  { id: 'FR-12', module: 'GD2-Teams', title: 'Moi / chap nhan thanh vien', actor: 'Student', priority: 'P1', phase: 'GD2',
    ac: 'Invite PENDING; accept/reject; USER_IN_ANOTHER_TEAM gate',
    api: 'POST /teams/{id}/invites; PATCH /teams/{id}/members/{uid}', ui: 'StudentTeamPage; matchmaking',
    test: '5-secondary-portals-mutating.spec.js tests 3-4', scenario: 'Happy/Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/02-invitations.md' },
  { id: 'FR-13', module: 'GD2-Teams', title: 'Coordinator duyet doi', actor: 'Coordinator', priority: 'P1', phase: 'GD2',
    ac: 'Batch approve chi READY teams; PENDING→ACTIVE',
    api: 'PATCH /teams/{id}/approve', ui: '/teams → CoordinatorTeamPage ApprovalTable',
    test: 'G2-BULK-03; e2e-gd2', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/01-business-rules-gd2.md §6' },
  { id: 'FR-13A', module: 'GD2-Teams', title: 'Khoa doi sau registration_end', actor: 'System', priority: 'P1', phase: 'GD2',
    ac: 'Cron set is_locked=true; block invite after lock',
    api: 'TeamLockScheduler', ui: 'FormationGraceBanner.jsx',
    test: 'TeamLockServiceImpl', scenario: 'Happy', owner: 'BE', status: 'Done',
    evidence: 'BE/docs/mf02/01-business-rules-gd2.md §7' },
  { id: 'FR-13B', module: 'GD2-Teams', title: 'Boc tham Track (Lottery)', actor: 'Coordinator', priority: 'P1', phase: 'GD2',
    ac: 'POST lottery after lock; team_round_tracks assigned_group',
    api: 'POST /hackathons/{id}/lottery', ui: '/hackathons/:id/setup?tab=lottery',
    test: 'G2-H02; LOTTERY-DATA-01; dev-seed-matrix', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/01-business-rules-gd2.md §8' },
  { id: 'FR-13B-R', module: 'GD2-Teams', title: 'Re-lottery track assignment', actor: 'Coordinator', priority: 'P2', phase: 'GD2',
    ac: 'PATCH track before round ACTIVE; ROUND_ALREADY_ACTIVE if active',
    api: 'PATCH /teams/{id}/rounds/{roundId}/track', ui: 'LotteryManagementPage.jsx',
    test: 'gate matrix G2', scenario: 'Bad', owner: 'BE', status: 'Done',
    evidence: 'BE/docs/mf02/01-business-rules-gd2.md §8' },
  { id: 'FR-13C', module: 'GD2-Teams', title: 'Mentor history per team/round', actor: 'Coordinator/Student', priority: 'P2', phase: 'GD2',
    ac: 'GET /teams/{id}/mentors returns round-scoped history',
    api: 'GET /teams/{id}/mentors', ui: 'StudentTeamPage mentor panel; team-mentor-history.spec.js',
    test: 'team-mentor-history.spec.js', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf02/01-business-rules-gd2.md §9' },
  { id: 'FR-15', module: 'GD3-Scoring', title: 'Activate Round (Gate2)', actor: 'Coordinator', priority: 'P1', phase: 'GD3',
    ac: '1 active round/hackathon; JUDGE_NOT_ASSIGNED; NO_TEAMS_IN_ROUND',
    api: 'PATCH /rounds/{id}/activate', ui: 'RoundManagementPage.jsx',
    test: 'G3-H01; close-submission-early.spec.js', scenario: 'Happy/Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'RoundActivationServiceImpl.java' },
  { id: 'FR-15A', module: 'GD3-Scoring', title: 'Phat de (Release problem)', actor: 'Coordinator', priority: 'P1', phase: 'GD3',
    ac: 'One-way release; sync all tracks; EARLY-WAIT before examAt',
    api: 'PATCH /rounds/{id}/release-problem', ui: 'RoundManagementPage release button',
    test: 'G3-FLOW-01; EARLY-WAIT-01', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'RoundProgressionServiceImpl.releaseProblem' },
  { id: 'FR-16', module: 'GD3-Scoring', title: 'Nop bai Sơ loại', actor: 'Student', priority: 'P1', phase: 'GD3',
    ac: 'POST /submissions upsert; 1 per team per track; eliminated blocked',
    api: 'POST /submissions', ui: '/student/submit → StudentSubmissionPage.jsx',
    test: 'preliminary-student-submit.spec.js; gd5-final-submit-smoke', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'SubmissionServiceImpl.java' },
  { id: 'FR-16A', module: 'GD3-Scoring', title: 'Duyet bai muon (Late review)', actor: 'Coordinator', priority: 'P1', phase: 'GD3',
    ac: 'PATCH review-late APPROVE/REJECT; LATE_PENDING→ACCEPTED',
    api: 'PATCH /submissions/{id}/review-late', ui: '/coordinator/late-submissions',
    test: 'close-submission-early.spec.js; G3-LATE-04', scenario: 'Happy/Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'LateSubmissionReviewPage.jsx' },
  { id: 'FR-17', module: 'GD3-Scoring', title: 'Submission metadata async (optional)', actor: 'System', priority: 'P2', phase: 'GD3',
    ac: 'Enqueue PENDING metadata fetch; no REST required',
    api: 'internal SubmissionMetadataService', ui: 'N/A',
    test: '09-be-backlog optional', scenario: 'N/A', owner: 'BE', status: 'Planned',
    evidence: 'BE/docs/mf03/09-be-backlog-gd4-gd5-gd6.md Phu luc A' },
  { id: 'FR-18', module: 'GD3-Scoring', title: 'Cham diem (Score upsert)', actor: 'Judge', priority: 'P1', phase: 'GD3',
    ac: 'POST /scores; SCORING_LOCKED guard; form reset on team switch',
    api: 'POST /scores', ui: '/judging/:id/scoring → LiveScoringPage.jsx',
    test: 'G3-SCORE-03; G5-H02', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'ScoreServiceImpl.java' },
  { id: 'FR-18A', module: 'GD3-Scoring', title: 'Live scoring WebSocket (STOMP)', actor: 'Judge/Coordinator', priority: 'P1', phase: 'GD3',
    ac: '3 STOMP topics; debounce publish; real-time scoreboard',
    api: 'WS /ws STOMP', ui: 'LiveScoringPage.jsx; useLiveScoringV2.js',
    test: 'websocket-queue-timer.spec.js (queue); live_scoring tests', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf03/06-live-scoring-websocket.md' },
  { id: 'FR-20', module: 'GD3-Scoring', title: 'Ranking preview / leaderboard', actor: 'Coordinator/Student', priority: 'P1', phase: 'GD3',
    ac: 'GET ranking preview after scores; scoreboard public',
    api: 'GET /rounds/{id}/ranking/preview; GET /rounds/{id}/scoreboard', ui: 'RoundRankingPreviewPage; StudentRoundLeaderboardPage',
    test: 'G4-VIEW-03; SEC-AUTH-01', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'RoundRankingQueryService.java' },
  { id: 'FR-20A', module: 'GD3-Scoring', title: 'Khoa cham diem (Lock scoring)', actor: 'Coordinator', priority: 'P1', phase: 'GD3',
    ac: 'PATCH lock-scoring; warnings; scoring_locked=true',
    api: 'PATCH /rounds/{id}/lock-scoring', ui: 'PreliminaryResultsPage lock button',
    test: 'G3-H04; coord-hackathon-progression', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'RoundProgressionServiceImpl.lockScoring' },
  { id: 'FR-21', module: 'GD3-Scoring', title: 'Loai doi (Eliminate)', actor: 'Coordinator', priority: 'P1', phase: 'GD3',
    ac: 'PATCH eliminate → ELIMINATED; read-only portal',
    api: 'PATCH /teams/{id}/eliminate', ui: 'CoordinatorTeamPage; G5-ELIM-02',
    test: 'G5-ELIM-02', scenario: 'Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'TeamServiceImpl.java' },
  { id: 'FR-22A', module: 'GD4-Advance', title: 'Wildcard (Ve vot) — DEPRECATED', actor: 'Coordinator', priority: 'P2', phase: 'GD4',
    ac: 'REMOVED 18/07 — advance Top-N only; API returns empty',
    api: 'GET wildcard-candidates (no-op)', ui: 'Tab Ve vot removed from PreliminaryResultsPage',
    test: 'P0-WC audit PASS (absent)', scenario: 'Deprecated', owner: 'BE+FE', status: 'Deprecated',
    evidence: 'session-changelog §0.1; RoundProgressionServiceImpl.emptyWildcardResponse' },
  { id: 'FR-22B', module: 'GD4-Advance', title: 'Tiebreak (dong diem)', actor: 'Coordinator/Judge', priority: 'P1', phase: 'GD4',
    ac: 'GET tiebreak; POST resolve; TIEBREAK_REQUIRED before advance; TC-TB-01 no ghost',
    api: 'GET/POST /rounds/{id}/tiebreak/*', ui: 'PreliminaryResultsPage tiebreak section',
    test: 'preliminary-results-progression.spec.js; TC-TB-01; G4 tiebreak seeds', scenario: 'Happy/Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'seal-gd4-tiebreak-manual seed' },
  { id: 'FR-23', module: 'GD3-Scoring', title: 'Presentation queue & timer', actor: 'Coordinator/Judge', priority: 'P1', phase: 'GD3',
    ac: 'Shuffle queue; timer TT/Q&A; STOMP broadcast; controller TRANSFER grant',
    api: 'POST /presentation/queue/shuffle; POST /presentation/timer/*', ui: '/presentation/queue → PresentationQueuePage.jsx',
    test: 'websocket-queue-timer.spec.js; G3-TIMER-02; SH-01/SH-02', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'PresentationQueueController.java' },
  { id: 'FR-24', module: 'GD4-Advance', title: 'Publish ket qua So loai', actor: 'Coordinator', priority: 'P1', phase: 'GD4',
    ac: 'PATCH publish after lock; student sync without F5',
    api: 'PATCH /rounds/{prelimId}/publish', ui: 'PreliminaryResultsPage publish',
    test: 'G4-H01; G4-SYNC-02; hackathon-progression-mutating', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'RoundProgressionServiceImpl.publish' },
  { id: 'FR-25', module: 'GD4-Advance', title: 'Activate vong Chung ket', actor: 'Coordinator', priority: 'P1', phase: 'GD4',
    ac: 'Gate RESULT_NOT_PUBLISHED; JUDGE_NOT_ASSIGNED; FINAL_EXTERNAL',
    api: 'PATCH /rounds/{finalId}/activate', ui: 'FinalRoundConfigPage.jsx',
    test: 'G4-H04; G4-N01/N02; final-round-smoke', scenario: 'Happy/Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'RoundActivationServiceImpl final gate' },
  { id: 'FR-26', module: 'GD5-Final', title: 'Nop bai Chung ket', actor: 'Student', priority: 'P1', phase: 'GD5',
    ac: 'POST submission roundId=final; no trackId; HARD_LOCK late',
    api: 'POST /submissions', ui: '/student/submit final tab',
    test: 'G5-H01; gd5-final-submit-smoke.spec.js', scenario: 'Happy/Bad', owner: 'BE+FE', status: 'Done',
    evidence: 'G5-FINAL-01 StudentPortalFinalProblemReuseTest' },
  { id: 'FR-27', module: 'GD4-Advance', title: 'Gan Judge Chung ket', actor: 'Coordinator', priority: 'P1', phase: 'GD4',
    ac: 'POST judge-assignments FINAL_EXTERNAL on final round',
    api: 'POST /rounds/{finalId}/judge-assignments', ui: 'PeopleManagementPage final judges',
    test: 'G4-H03', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'JudgeAssignmentService.assignFinalRoundG4' },
  { id: 'FR-28', module: 'GD4-Advance', title: 'Tiebreak rules config', actor: 'Coordinator', priority: 'P2', phase: 'GD4',
    ac: 'tiebreak_rule on round: PENALTY_SCORE | SUBMISSION_TIME | COORDINATOR_DECISION',
    api: 'PUT /rounds/{id}', ui: 'RoundFormModal tiebreak dropdown',
    test: 'playbook GD4', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'BE/docs/mf03/01-business-rules-gd3.md §10' },
  { id: 'FR-29', module: 'Analytics-RBL', title: 'RBL Calibration (isolated flow)', actor: 'Judge/Coordinator', priority: 'P2', phase: 'GD5',
    ac: 'RBL calibration isolated; old Calibration UI removed; banner CHAM THU',
    api: 'GET/POST /rounds/{id}/rbl/calibration/*', ui: 'AnalyticsPage RBL tab',
    test: 'CALIB-01-ANALYTICS PASS; THESIS-RBL-*', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'RblDashboardController; session-changelog GĐ5' },
  { id: 'FR-30', module: 'GD4-Advance', title: 'Advance teams (Top-N)', actor: 'Coordinator', priority: 'P1', phase: 'GD4',
    ac: 'POST advance; team_round_participation ADVANCED/ELIMINATED; no wildcard',
    api: 'POST /rounds/{prelimId}/advance', ui: 'PreliminaryResultsPage Chot chuyen vong',
    test: 'G4-H02; coord-hackathon-progression; preliminary-results-progression', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'RoundProgressionServiceImpl.advanceTeams' },
  { id: 'FR-30A', module: 'GD5-Final', title: 'Lock CK → PENDING_CONFIRM', actor: 'Coordinator', priority: 'P1', phase: 'GD5',
    ac: 'After final lock-scoring hackathon.status=PENDING_CONFIRM',
    api: 'PATCH /rounds/{finalId}/lock-scoring', ui: 'Hackathon status banner',
    test: 'G5-H04; seal-gd6-pending-confirm seed', scenario: 'Happy', owner: 'BE', status: 'Done',
    evidence: 'BE/docs/mf03/01-business-rules-gd3.md §8' },
  { id: 'FR-31', module: 'GD6-Closure', title: 'Bang XH Team (Final ranking view)', actor: 'Coordinator/Student', priority: 'P1', phase: 'GD6',
    ac: 'GET team-rankings for final round',
    api: 'GET /hackathons/{id}/team-rankings', ui: '/hackathons/:id/results → HackathonResultsPage.jsx',
    test: 'G6-RANK-02; hackathon-closure-smoke', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'FinalRankingQueryService.java' },
  { id: 'FR-32', module: 'GD6-Closure', title: 'Trao / thu hoi giai', actor: 'Coordinator', priority: 'P1', phase: 'GD6',
    ac: 'POST prizes with category+note; revoke with audit',
    api: 'POST /hackathons/{id}/prizes; DELETE /prizes/{id}', ui: 'HackathonResultsPage prizes panel',
    test: 'G6-H02; PRIZE-02 audit', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'PrizeController.java' },
  { id: 'FR-33', module: 'GD6-Closure', title: 'Confirm FINISHED (Chot so)', actor: 'Coordinator', priority: 'P1', phase: 'GD6',
    ac: 'PATCH /confirm when all judges scored + prizes ok',
    api: 'PATCH /hackathons/{id}/confirm', ui: 'HackathonResultsPage confirm closure',
    test: 'G6-H03; G6-CLOS-01; hackathon-closure-smoke', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'HackathonClosureServiceImpl.java' },
  { id: 'FR-33A', module: 'GD6-Closure', title: 'GET bang XH Team (API)', actor: 'Coordinator', priority: 'P1', phase: 'GD6',
    ac: 'Same as FR-31 API surface for closure workflow',
    api: 'GET /hackathons/{id}/team-rankings', ui: 'HackathonResultsPage team tab',
    test: 'G6-CLOS-01', scenario: 'Happy', owner: 'BE', status: 'Done',
    evidence: 'BE/docs/mf03/01-business-rules-gd6.md' },
  { id: 'FR-33B', module: 'GD6-Closure', title: 'Bang XH Chapter (persist)', actor: 'Coordinator', priority: 'P1', phase: 'GD6',
    ac: 'GET chapter-rankings; worker calculateAsync',
    api: 'GET /hackathons/{id}/chapter-rankings', ui: 'AnalyticsPage / results chapter tab',
    test: 'G6-RANK-02', scenario: 'Happy', owner: 'BE', status: 'Done',
    evidence: 'chapter_rankings table' },
  { id: 'FR-33C', module: 'GD6-Closure', title: 'Bang XH Ca nhan (Fall)', actor: 'Coordinator', priority: 'P2', phase: 'GD6',
    ac: 'Gate individual_ranking_enabled; GET individual-rankings',
    api: 'GET /hackathons/{id}/individual-rankings', ui: 'Results individual tab',
    test: 'seal-fall-2025-finished seed', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'FR-33D flag on Hackathon' },
  { id: 'FR-33D', module: 'GD6-Closure', title: 'Co individual_ranking_enabled', actor: 'Coordinator', priority: 'P2', phase: 'GD6',
    ac: 'Boolean on hackathon gates FR-33C',
    api: 'PUT /hackathons/{id}', ui: 'HackathonForm individual ranking toggle',
    test: 'CreateHackathonPage', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'Hackathon entity' },
  { id: 'FR-34', module: 'GD6-Closure', title: 'Export CSV/Excel', actor: 'Coordinator', priority: 'P1', phase: 'GD6',
    ac: 'POST export-jobs; full matrix not top-3 only; CSV-01',
    api: 'POST /hackathons/{id}/export-jobs', ui: 'AnalyticsPage export; HackathonResultsPage',
    test: 'G6-EXPO-03; ExportCsvBuilderRankingsTest', scenario: 'Happy', owner: 'BE', status: 'Done',
    evidence: 'ExportCsvBuilder.java' },
  { id: 'FR-35', module: 'GD6-Closure', title: 'Export RBL anonymized', actor: 'Coordinator', priority: 'P2', phase: 'GD6',
    ac: 'export-jobs type ANONYMIZED_RBL; anonymizedJudgeId',
    api: 'POST /hackathons/{id}/export-jobs', ui: 'AnalyticsPage RBL export',
    test: 'THESIS-RBL-02 PASS', scenario: 'Happy', owner: 'BE+FE', status: 'Done',
    evidence: 'RblVarianceItemResponse anonymizedJudgeId' },
  { id: 'FR-36', module: 'GD6-Closure', title: 'Audit append-only', actor: 'System', priority: 'P1', phase: 'GD6',
    ac: 'Audit log on confirm/export/revoke; AUDIT-RO-01 read-only',
    api: 'GET /audit-logs', ui: 'N/A (admin)',
    test: 'AUDIT-RO-01; PUB-01', scenario: 'Happy', owner: 'BE', status: 'Done',
    evidence: 'AuditService.java' },
];

/** User-role portal requirements (U-*, FR-U, FR-M, FR-J sampled) */
const PORTAL_CATALOG = [
  { id: 'FR-U-06', module: 'Student-Portal', title: 'Dang ky hackathon', actor: 'Student', priority: 'P1',
    ac: 'POST /me/hackathons/{id}/register', api: 'POST /me/hackathons/{id}/register', ui: '/student/hackathons',
    test: 'mode-b-continuous-ui', scenario: 'Happy', owner: 'BE+FE', status: 'Done', evidence: 'BE/docs/user-role/01-student-api-catalog.md' },
  { id: 'FR-U-15-F', module: 'Student-Portal', title: 'Fall track select gate', actor: 'Student', priority: 'P2',
    ac: 'Spring season → NOT_APPLICABLE; no Fall card', api: 'GET /me/hackathons/{id}/selectable-tracks', ui: '/student/team',
    test: 'fall-track-select.spec.js', scenario: 'Bad', owner: 'BE+FE', status: 'Done', evidence: 'fall-track-select-mutating.spec.js' },
  { id: 'FR-U-32', module: 'Student-Portal', title: 'Annual awards view', actor: 'Student', priority: 'P2',
    ac: 'GET /me/annual-awards?year=', api: 'GET /me/annual-awards', ui: '/student/annual-awards',
    test: 'student-portal-parity', scenario: 'Happy', owner: 'BE+FE', status: 'Done', evidence: 'StudentAnnualAwardsPage.jsx' },
  { id: 'FR-M-05', module: 'Mentor-Portal', title: 'Track-only bootstrap card', actor: 'Mentor', priority: 'P2',
    ac: 'Track assignments but no round → bootstrap UI', api: 'GET /me/mentor-track-assignments', ui: '/mentor/rounds',
    test: 'mentor-track-bootstrap.spec.js', scenario: 'Happy', owner: 'FE', status: 'Done', evidence: 'mentor2@ seed' },
  { id: 'FR-M-16', module: 'Mentor-Portal', title: 'Mentor round schedule', actor: 'Mentor', priority: 'P2',
    ac: 'GET /me/mentor/rounds/{id}/schedule stub', api: 'GET /me/mentor/rounds/{id}/schedule', ui: '/mentor/support',
    test: 'mentor-portal-mutating', scenario: 'Happy', owner: 'BE', status: 'Planned', evidence: 'BE/docs/user-role/03-mentor-api-catalog.md' },
  { id: 'FR-J-07', module: 'Judge-Portal', title: 'Judge assignment types', actor: 'Judge', priority: 'P1',
    ac: 'NORMAL vs FINAL_EXTERNAL; HEAD deprecated', api: 'GET /me/judge-track-assignments', ui: '/judge/dashboard',
    test: 'smoke-login judge path', scenario: 'Happy', owner: 'BE+FE', status: 'Done', evidence: 'P0-HEAD removed 18/07' },
  { id: 'U-30', module: 'Student-Portal', title: 'Appeal (24h rule)', actor: 'Student', priority: 'P2',
    ac: 'POST appeal within 24h of publish', api: 'POST /me/appeals', ui: 'Student portal (stub)',
    test: 'N/A', scenario: 'Planned', owner: 'BE', status: 'Planned', evidence: 'BE/docs/user-role/04-be-backlog-user-roles.md' },
  { id: 'U-29', module: 'Student-Portal', title: 'Certificate download', actor: 'Student', priority: 'P2',
    ac: 'GET /me/certificates after FINISHED', api: 'GET /me/certificates', ui: '/student/annual-awards',
    test: 'student-portal-parity', scenario: 'Happy', owner: 'BE+FE', status: 'Done', evidence: 'StudentMeController' },
];

/** Expanded U-* user portal requirements */
function generateUserRoleCatalog() {
  const defs = [
    ['U-01', 'Auth', 'Register account', 'Student', 'POST /auth/register', '/register', 'FR-07'],
    ['U-02', 'Auth', 'Login JWT', 'All', 'POST /auth/login', '/login', 'FR-09'],
    ['U-03', 'Auth', 'Refresh token', 'All', 'POST /auth/refresh', 'auth slice', 'FR-09'],
    ['U-04', 'Auth', 'Profile onboarding', 'Student', 'PATCH /users/me', '/onboarding', 'Register flow 20/07'],
    ['U-06', 'Student-Portal', 'Register for hackathon event', 'Student', 'POST /me/hackathons/{id}/register', '/student/hackathons', 'FR-10'],
    ['U-07', 'Student-Portal', 'Create team', 'Student', 'POST /teams', '/student/team', 'FR-11'],
    ['U-08', 'Student-Portal', 'Invite member', 'Student', 'POST /teams/{id}/invites', '/student/team', 'FR-12'],
    ['U-09', 'Student-Portal', 'Accept invite', 'Student', 'PATCH /teams/{id}/members/{uid}', '/student/team', 'FR-12'],
    ['U-10', 'Student-Portal', 'Reject invite', 'Student', 'PATCH /teams/{id}/members/{uid}', '/student/team', 'FR-12'],
    ['U-11', 'Student-Portal', 'Leave team', 'Student', 'DELETE /teams/{id}/members/me', '/student/team', 'FR-12'],
    ['U-12', 'Student-Portal', 'Transfer leader', 'Student', 'PATCH /teams/{id}/transfer-leader', '/student/team', 'FR-11C'],
    ['U-13', 'Student-Portal', 'Disband team', 'Student', 'PATCH /teams/{id}/disband', '/student/team', 'FR-11D'],
    ['U-18', 'Student-Portal', 'Submit assignment', 'Student', 'POST /submissions', '/student/submit', 'FR-16'],
    ['U-19', 'Student-Portal', 'View submission status', 'Student', 'GET /me/submission', '/student/submit', 'FR-16'],
    ['U-20', 'Student-Portal', 'View round leaderboard', 'Student', 'GET /rounds/{id}/scoreboard', '/student/leaderboard', 'FR-20'],
    ['U-21', 'Student-Portal', 'View announcements', 'Student', 'GET /me/announcements', '/student/notifications', 'PUB-01'],
    ['U-22', 'Student-Portal', 'Presentation queue view', 'Student', 'WS /ws queue topic', '/student/queue', 'FR-23 STT-01'],
    ['U-23', 'Judge-Portal', 'View assigned tracks', 'Judge', 'GET /me/judge-track-assignments', '/judge/dashboard', 'FR-J-07'],
    ['U-24', 'Judge-Portal', 'Score submission', 'Judge', 'POST /scores', '/judging/:id/scoring', 'FR-18'],
    ['U-25', 'Judge-Portal', 'Presentation timer control', 'Judge', 'POST /presentation/timer/*', 'LiveScoringPage', 'FR-23'],
    ['U-26', 'Mentor-Portal', 'View mentor rounds', 'Mentor', 'GET /me/mentor/rounds', '/mentor/rounds', 'G3-H03'],
    ['U-27', 'Mentor-Portal', 'View assigned teams', 'Mentor', 'GET /me/mentor/teams', '/mentor/support', 'G3-H03'],
    ['U-28', 'Coordinator-Portal', 'Approve users', 'Coordinator', 'PATCH /users/{id}/status', '/admin/users', 'FR-08'],
    ['U-29', 'Student-Portal', 'Download certificate', 'Student', 'GET /me/certificates', 'Student results', 'backlog'],
    ['U-30', 'Student-Portal', 'Appeal within 24h', 'Student', 'POST /me/appeals', 'Student portal stub', 'backlog'],
    ['U-31', 'Coordinator-Portal', 'Late submission review', 'Coordinator', 'PATCH /submissions/{id}/review-late', '/coordinator/late-submissions', 'FR-16A'],
    ['U-32', 'Student-Portal', 'Annual awards', 'Student', 'GET /me/annual-awards', '/student/annual-awards', 'FR-U-32'],
  ];
  return defs.map(([id, module, title, actor, api, ui, cross]) => ({
    id, module, title, actor, priority: 'P1', ac: cross, api, ui,
    test: cross.startsWith('FR') ? cross : `U catalog ${id}`,
    scenario: 'Happy', owner: module.includes('Portal') ? 'BE+FE' : 'BE+FE',
    status: id === 'U-30' ? 'Planned' : 'Done',
    evidence: 'BE/docs/user-role/01-student-api-catalog.md; 04-be-backlog-user-roles.md',
  }));
}

/** Full TC-GD1 ID list from qa-uat.md */
function generateTcGd1Full() {
  const happy = ['H01','H02','H03','H04','H05','H06','H07','H08','H09','H10','H11','H12','H13','H14','H15'];
  const batch = ['B01','B02','B03','B04','B05','B06','B07','B08','B09','B10','B11'];
  const neg = ['N01','N02','N03','N04','N05','N21','N06','N22','N07','N08','N09','N16','N17','N10a','N10b','N11','N12','N13','N18','N19','N20','N23','N24','N14','N14b','N25','N26','N27','N28','N15'];
  const edge = ['E01','E02','E03','E04','E05','E06','E07','E07b','E08','E09','E10'];
  const ids = [...happy, ...batch, ...neg, ...edge].map((s) => `TC-GD1-${s}`);
  return ids.map((id) => ({
    id, module: 'GD1-UAT', title: `GĐ1 UAT ${id}`, actor: 'Coordinator',
    priority: id.includes('-N') ? 'P1' : id.includes('-E') ? 'P1' : 'P0',
    ac: 'See BE/docs/mf01/06-qa-uat.md',
    api: 'Postman collection gd1', ui: 'HackathonSetupPage tabs',
    test: id, scenario: id.includes('-N') ? 'Bad' : id.includes('-E') ? 'Regression' : 'Happy',
    owner: 'QA', status: 'Planned', evidence: 'BE/docs/mf01/06-qa-uat.md',
  }));
}

function parseAuditResultsJson() {
  const p = path.join(BE_DOCS, 'testing/ui-audit-2026-07-19/deep/results.json');
  if (!fs.existsSync(p)) return [];
  const data = JSON.parse(fs.readFileSync(p, 'utf8'));
  const items = [];
  for (const row of data.idResults || []) {
    const id = row.id;
    if (!id) continue;
    let module = 'UX-Audit';
    if (/^G[1-6]-/.test(id) || id.startsWith('GD')) module = 'Cross-cutting';
    else if (id.startsWith('SEC') || id.startsWith('IDOR') || id.startsWith('VALID') || id.startsWith('BC')) module = 'Security';
    else if (id.startsWith('THESIS-RBL') || id.startsWith('RBL') || id.startsWith('RQ-SMOKE') || id.startsWith('CALIB')) module = 'Analytics-RBL';
    else if (id.includes('SCORE') || id.startsWith('SH-') || id.startsWith('TIMER') || id.startsWith('STT') || id.startsWith('LOCK')) module = 'GD3-Scoring';
    else if (id.startsWith('LOTTERY') || id.startsWith('G2')) module = 'GD2-Teams';
    else if (id.startsWith('PUB') || id.startsWith('FAIL')) module = 'GD4-Advance';
    else if (id.startsWith('H-FORM') || id.startsWith('H-PEOPLE')) module = 'GD1-Setup';
    const statusMap = { PASS: 'Done', SKIP: 'Manual', FAIL: 'Planned' };
    items.push({
      id,
      module,
      title: row.note || `Deep audit ${id}`,
      actor: 'QA',
      priority: id.startsWith('P0') || id.startsWith('SEC') || id.startsWith('IDOR') ? 'P0' : 'P1',
      ac: row.note || '',
      api: '',
      ui: '',
      test: `ui-audit-2026-07-19 deep ${id}`,
      scenario: id.includes('IDOR') || id.startsWith('BC') ? 'Sabotage' : 'Happy',
      owner: 'QA',
      status: statusMap[row.status] || 'Planned',
      evidence: `BE/docs/testing/ui-audit-2026-07-19/deep/results.json${row.evidence ? `; ${row.evidence}` : ''}`,
    });
  }
  return items;
}

/** Parse markdown table rows: | col1 | col2 | ... | */
function parseMdTable(content, minCols = 3) {
  const rows = [];
  for (const line of content.split('\n')) {
    const t = line.trim();
    if (!t.startsWith('|') || t.includes('---')) continue;
    const cols = t.split('|').slice(1, -1).map((c) => c.trim());
    if (cols.length >= minCols) rows.push(cols);
  }
  return rows;
}

function parseGateMatrix() {
  const content = read('BE/docs/testing/gate-regression-test-matrix-gd1-gd6.md');
  const rows = parseMdTable(content, 4);
  const items = [];
  for (const cols of rows) {
    const id = cols[0].replace(/\*\*/g, '');
    if (!/^G[1-6]-/.test(id)) continue;
    const phase = id.startsWith('G1') ? 'GD1' : id.startsWith('G2') ? 'GD2' : id.startsWith('G3') ? 'GD3' : id.startsWith('G4') ? 'GD4' : id.startsWith('G5') ? 'GD5' : 'GD6';
    const scenario = id.includes('-H') || id.includes('-E') ? 'Happy' : id.includes('-N') ? 'Bad' : id.includes('-R') ? 'Regression' : id.includes('-T') ? 'Regression' : 'Happy';
    items.push({
      id,
      module: `${phase}-Gate`,
      title: cols[1],
      actor: phase === 'GD2' ? 'Student/Coordinator' : 'Coordinator',
      priority: 'P1',
      ac: cols[3] || cols[2],
      api: (cols[2] || '').match(/`(GET|POST|PATCH|DELETE)[^`]+`/g)?.join('; ') || '',
      ui: 'See playbook',
      test: `gate-regression-test-matrix ${id}`,
      scenario,
      owner: 'QA',
      status: 'Planned',
      evidence: 'BE/docs/testing/gate-regression-test-matrix-gd1-gd6.md',
    });
  }
  return items;
}

function parseEnterpriseMatrix() {
  const content = read('docs/testing/enterprise-regression-matrix-gd1-gd6.md');
  const rows = parseMdTable(content, 6);
  const items = [];
  for (const cols of rows) {
    const id = cols[0];
    if (!id || id === 'ID') continue;
    items.push({
      id,
      module: cols[1]?.includes('Cross') ? 'Cross-cutting' : `${cols[1]}-Enterprise`,
      title: cols[4],
      actor: cols[2],
      priority: id.startsWith('SEC') || id === 'TC-TB-01' ? 'P0' : 'P1',
      ac: cols[5] || '',
      api: '',
      ui: cols[5]?.match(/\.jsx|Page\.jsx|\.tsx/)?.[0] || '',
      test: cols[5],
      scenario: cols[3]?.includes('Bad') ? 'Bad' : cols[3]?.includes('Reg') ? 'Regression' : 'Happy',
      owner: cols[5]?.includes('BE') && cols[5]?.includes('FE') ? 'BE+FE' : cols[5]?.includes('FE') ? 'FE' : 'BE',
      status: cols[7] || 'Planned',
      evidence: 'docs/testing/enterprise-regression-matrix-gd1-gd6.md',
    });
  }
  return items;
}

function parseTcGd1() {
  const content = read('BE/docs/mf01/06-qa-uat.md');
  const items = [];
  const re = /^#{2,4} (TC-GD1-[A-Z0-9]+(?:\.\.[A-Z0-9]+)?)/gm;
  let m;
  while ((m = re.exec(content)) !== null) {
    const id = m[1];
    const section = content.slice(m.index, m.index + 400);
    const desc = section.split('\n').slice(1, 3).join(' ').trim().slice(0, 120);
    items.push({
      id,
      module: 'GD1-UAT',
      title: desc || `UAT ${id}`,
      actor: 'Coordinator',
      priority: id.includes('-N') ? 'P1' : 'P0',
      ac: 'See mf01/06-qa-uat.md steps',
      api: 'Postman / API',
      ui: 'HackathonSetupPage',
      test: id,
      scenario: id.includes('-N') ? 'Bad' : id.includes('-E') ? 'Regression' : 'Happy',
      owner: 'QA',
      status: 'Planned',
      evidence: 'BE/docs/mf01/06-qa-uat.md',
    });
  }
  // Batch IDs like TC-GD1-B01 … B11
  const batchRe = /\| (TC-GD1-[A-Z0-9]+(?: … [A-Z0-9]+)?)/g;
  while ((m = batchRe.exec(content)) !== null) {
    const raw = m[1];
    if (raw.includes('…')) continue; // skip range shorthand in summary table
    if (items.some((i) => i.id === raw)) continue;
    items.push({
      id: raw,
      module: 'GD1-UAT',
      title: `UAT batch ${raw}`,
      actor: 'Coordinator',
      priority: 'P0',
      ac: 'See qa-uat',
      api: 'API',
      ui: 'Setup',
      test: raw,
      scenario: 'Happy',
      owner: 'QA',
      status: 'Planned',
      evidence: 'BE/docs/mf01/06-qa-uat.md',
    });
  }
  return items;
}

/** Audit / Chương L IDs from L35 ledger + deep audit */
const AUDIT_CATALOG = [
  { id: 'P0-WC', module: 'UX-Audit', title: 'Wildcard removed verification', actor: 'QA', priority: 'P0', ac: 'No wildcard tab/API active', test: 'ui-ux-deep-audit phase0', scenario: 'Happy', status: 'Done', evidence: 'REPORT.md Phase 0' },
  { id: 'P0-HEAD', module: 'UX-Audit', title: 'HEAD role removed', actor: 'QA', priority: 'P0', ac: 'Controller-grant TRANSFER only', test: 'ui-ux-deep-audit', scenario: 'Happy', status: 'Done', evidence: 'REPORT.md' },
  { id: 'P0-PG', module: 'UX-Audit', title: 'Personnel Guard active', actor: 'QA', priority: 'P0', ac: 'JUDGE_ASSIGN_DUPLICATE; CONFLICT_MENTOR_JUDGE_SAME_TRACK', test: 'mentor-portal-mutating conflict tests', scenario: 'Happy', status: 'Done', evidence: 'REPORT.md' },
  { id: 'COORD-SCORE-ALL-01', module: 'GD3-Scoring', title: 'Coordinator view all component scores', actor: 'Coordinator', priority: 'P1', ac: 'Score breakdown drawer before lock', test: 'ui-ux-deep-audit SCORE phase', scenario: 'Happy', status: 'Done', evidence: 'REPORT-score.md' },
  { id: 'STU-SCORE-01', module: 'GD3-Scoring', title: 'Student view own scores per round', actor: 'Student', priority: 'P1', ac: 'Student results tab shows scores after publish', test: 'student-portal-parity', scenario: 'Happy', status: 'Done', evidence: 'REPORT-score.md' },
  { id: 'SH-01', module: 'GD3-Scoring', title: 'Shuffle presentation queue', actor: 'Coordinator', priority: 'P1', ac: 'POST shuffle assigns order', test: 'websocket-queue-timer.spec.js', scenario: 'Happy', status: 'Done', evidence: 'REPORT-gd3.md' },
  { id: 'SH-02', module: 'GD3-Scoring', title: 'Reshuffle after start blocked', actor: 'Coordinator', priority: 'P1', ac: 'PRESENTATION_ALREADY_STARTED', test: 'manual playbook Chương L', scenario: 'Bad', status: 'Done', evidence: 'REPORT-gd3.md' },
  { id: 'LOCK-03', module: 'GD3-Scoring', title: 'Lock scoring gate', actor: 'Coordinator', priority: 'P1', ac: 'Cannot score after lock', test: 'close-submission-early', scenario: 'Bad', status: 'Done', evidence: 'REPORT-gd3.md' },
  { id: 'LOTTERY-GATE-01', module: 'GD2-Teams', title: 'Lottery gate PENDING teams', actor: 'Coordinator', priority: 'P1', ac: 'TEAMS_PENDING_APPROVAL blocks lottery', test: 'dev-seed-matrix', scenario: 'Bad', status: 'Done', evidence: 'REPORT-gd2.md' },
  { id: 'LOTTERY-DATA-01', module: 'GD2-Teams', title: 'Lottery data panel', actor: 'Coordinator', priority: 'P1', ac: 'GET /teams?hackathonId= shows team count', test: 'ui-ux-deep-audit GD3', scenario: 'Happy', status: 'Done', evidence: 'REPORT.md fix 19/07' },
  { id: 'TC-TB-01', module: 'GD4-Advance', title: 'No ghost tiebreak items', actor: 'Coordinator', priority: 'P0', ac: 'API tiebreak list matches UI; no orphan rows', test: 'l35-catalog-probe.mjs', scenario: 'Happy', status: 'Done', evidence: 'L35-probe-rerun.log 17/17 PASS', api: 'GET/POST /rounds/{id}/tiebreak/*', ui: 'PreliminaryResultsPage tiebreak section' },
  { id: 'EARLY-WAIT-01', module: 'GD1-Setup', title: 'Release problem before examAt disabled', actor: 'Coordinator', priority: 'P1', ac: 'Button disabled + tooltip Chua toi gio thi', test: 'canActivateRound.test.js', scenario: 'Happy', status: 'Done', evidence: 'enterprise-regression-matrix' },
  { id: 'STT-01', module: 'GD3-Scoring', title: 'Student sees presentation order', actor: 'Student', priority: 'P1', ac: 'STT + team code visible after shuffle', test: 'manual playbook', scenario: 'Happy', status: 'Done', evidence: 'REPORT-gd3.md' },
  { id: 'TIMER-RT-01', module: 'GD3-Scoring', title: 'Real-time timer sync 3 sides', actor: 'All', priority: 'P1', ac: 'STOMP timer phase sync coord/judge/student', test: 'websocket-queue-timer.spec.js', scenario: 'Happy', status: 'Done', evidence: 'REPORT-gd3.md' },
  { id: 'UX-CTX-01', module: 'UX-Audit', title: 'Global event context banner', actor: 'Coordinator', priority: 'P1', ac: 'EventContextBanner same hackathon all setup pages', test: 'ui-ux-deep-audit CROSS', scenario: 'Happy', status: 'Done', evidence: 'EventContextBanner.jsx' },
  { id: 'THESIS-RBL-02', module: 'Analytics-RBL', title: 'RBL variance anonymized judges', actor: 'Coordinator', priority: 'P0', ac: 'anonymizedJudgeId not raw judgeId', test: 'ui-ux-deep-audit analytics', scenario: 'Happy', status: 'Done', evidence: 'REPORT.md fix anonymizedJudgeId' },
  { id: 'SEC-AUTH-01', module: 'Security', title: 'Leaderboard 403 non-participant', actor: 'Student', priority: 'P0', ac: 'StudentAccessGuard.assertParticipatedInHackathon', test: 'StudentAccessGuardParticipatedTest', scenario: 'Bad', status: 'Done', evidence: 'enterprise-regression-matrix', api: 'GET /rounds/{id}/scoreboard', ui: 'StudentRoundLeaderboardPage.jsx' },
  { id: 'SEC-CLOU-02', module: 'Security', title: 'No Cloudinary secret in FE', actor: 'System', priority: 'P0', ac: 'scan-cloudinary-secret.mjs PASS', test: 'npm run test:sec:cloudinary', scenario: 'Happy', status: 'Done', evidence: 'scripts/scan-cloudinary-secret.mjs' },
  { id: 'IDOR-01', module: 'Security', title: 'IDOR cross-role API probes', actor: 'QA', priority: 'P0', ac: 'Student cannot mentor/judge APIs → 403', test: 'permission-idor-mutating.spec.js', scenario: 'Sabotage', status: 'Done', evidence: 'REPORT-negative.md' },
  { id: 'VALID-02', module: 'Security', title: 'Validation error codes named 4xx', actor: 'QA', priority: 'P1', ac: 'No 500 on bad input; VALIDATION_FAILED or business code', test: 'abuse-guards.spec.js', scenario: 'Bad', status: 'Done', evidence: 'REPORT-negative.md' },
  { id: 'CALIB-01', module: 'Analytics-RBL', title: 'Old Calibration UI removed', actor: 'QA', priority: 'P1', ac: 'No calibration panel in judge dashboard', test: 'ui-ux-deep-audit', scenario: 'Regression', status: 'Done', evidence: 'session-changelog' },
  { id: 'H-SUB-01', module: 'UX-Audit', title: 'Criteria weight >1.0 blocked in UI', actor: 'Coordinator', priority: 'P2', ac: 'Can bang button disabled when weight>1', test: 'manual H sub-bug checklist', scenario: 'Bad', status: 'Manual', evidence: 'REPORT-L35-ID-LEDGER Missing H' },
  { id: 'H-SUB-02', module: 'UX-Audit', title: 'SOFT_SKILL label correct', actor: 'Coordinator', priority: 'P2', ac: 'Criterion type labels match spec', test: 'manual', scenario: 'Happy', status: 'Manual', evidence: 'REPORT-L35-ID-LEDGER' },
  { id: 'I1', module: 'UX-Audit', title: 'False-success invite judge fix', actor: 'Coordinator', priority: 'P1', ac: 'Invite errors show named code not silent success', test: 'manual Nhom I', scenario: 'Bad', status: 'Done', evidence: 'playbook Nhom I/J' },
  { id: 'I2', module: 'UX-Audit', title: 'Final config no stray PDF upload', actor: 'Coordinator', priority: 'P1', ac: 'CK inherits prelim problem', test: 'G5-FINAL-01', scenario: 'Happy', status: 'Done', evidence: 'FinalRoundConfigPage' },
  { id: 'J2', module: 'UX-Audit', title: 'Duplicate round button gated', actor: 'Coordinator', priority: 'P2', ac: 'Nhan ban disabled when inappropriate', test: 'manual', scenario: 'Bad', status: 'Manual', evidence: 'REPORT-L35-ID-LEDGER' },
  { id: 'BC1', module: 'Security', title: 'Bad path: wrong judge on submission', actor: 'QA', priority: 'P1', ac: '403 FORBIDDEN', test: 'intentional-errors-catalog', scenario: 'Bad', status: 'Manual', evidence: 'REPORT-negative.md' },
  { id: 'BC2', module: 'Security', title: 'Bad path: score wrong criterion', actor: 'QA', priority: 'P1', ac: '422 validation', test: 'catalog', scenario: 'Bad', status: 'Manual', evidence: 'REPORT-negative.md' },
  { id: 'BC3', module: 'Security', title: 'Bad path: advance unpublished', actor: 'QA', priority: 'P1', ac: 'RESULT_NOT_PUBLISHED', test: 'l35-catalog-probe', scenario: 'Bad', status: 'Done', evidence: 'REPORT-negative.md' },
  { id: 'BC4', module: 'Security', title: 'Bad path: student coord API', actor: 'QA', priority: 'P1', ac: '403 FORBIDDEN', test: 'permission-idor', scenario: 'Sabotage', status: 'Manual', evidence: 'REPORT-negative.md' },
  { id: 'BC5', module: 'Security', title: 'Bad path: late submit final', actor: 'Student', priority: 'P1', ac: 'HARD_LOCK or rejected', test: 'G5-LATE-03', scenario: 'Bad', status: 'Done', evidence: 'REPORT-negative.md' },
  { id: 'BC6', module: 'Security', title: 'Bad path: team not advanced submit final', actor: 'Student', priority: 'P1', ac: 'TEAM_NOT_ADVANCED', test: 'l35-catalog-probe', scenario: 'Bad', status: 'Done', evidence: 'REPORT-negative.md' },
  { id: 'FAIL-02', module: 'GD4-Advance', title: 'Concurrent publish race', actor: 'Coordinator', priority: 'P1', ac: 'Exactly one winner 2xx; other 4xx', test: 'coord-concurrent-race.spec.js', scenario: 'Sabotage', status: 'Done', evidence: 'L5-race-rerun.log' },
  { id: 'REG-BASE-01', module: 'Cross-cutting', title: 'Regression baseline compare', actor: 'QA', priority: 'P1', ac: 'Pass/fail vs compat.lock baseline', test: 'reports/enterprise-regression-summary.md', scenario: 'Regression', status: 'Planned', evidence: 'compat.lock' },
];

function normalizeItem(item) {
  return {
    id: item.id,
    module: item.module || 'Cross-cutting',
    title: item.title || item.id,
    actor: item.actor || 'Coordinator',
    priority: item.priority || 'P1',
    ac: item.ac || '',
    test: item.test || '',
    scenario: item.scenario || 'Happy',
    api: item.api || '',
    ui: item.ui || '',
    owner: item.owner || 'BE+FE',
    status: item.status || 'Planned',
    evidence: item.evidence || '',
    phase: item.phase || '',
  };
}

function dedupeById(items) {
  const map = new Map();
  for (const raw of items) {
    const item = normalizeItem(raw);
    const key = `${item.id}|${item.module}`;
    if (!map.has(key)) {
      map.set(key, item);
    } else {
      const prev = map.get(key);
      if (item.status === 'Done' && prev.status !== 'Done') map.set(key, { ...prev, ...item, ac: item.ac || prev.ac });
      else map.set(key, { ...prev, test: [prev.test, item.test].filter(Boolean).join('; '), evidence: [prev.evidence, item.evidence].filter(Boolean).join('; ') });
    }
  }
  return [...map.values()];
}

function listE2eSpecs() {
  if (!fs.existsSync(FE_E2E)) return [];
  const out = [];
  function walk(dir) {
    for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
      const p = path.join(dir, ent.name);
      if (ent.isDirectory()) walk(p);
      else if (ent.name.endsWith('.spec.js')) out.push(path.relative(ROOT, p).replace(/\\/g, '/'));
    }
  }
  walk(FE_E2E);
  return out.sort();
}

function moduleSortKey(m) {
  const order = ['GD1', 'Auth', 'GD2', 'GD3', 'GD4', 'GD5', 'GD6', 'Student', 'Mentor', 'Judge', 'Analytics', 'Security', 'UX', 'Cross'];
  for (let i = 0; i < order.length; i++) {
    if (m.includes(order[i])) return i;
  }
  return 99;
}

function buildMarkdown(items, e2eSpecs) {
  const date = new Date().toISOString().slice(0, 10);
  const frCount = items.filter((i) => i.id.startsWith('FR-')).length;
  const gateCount = items.filter((i) => /^G[1-6]-/.test(i.id)).length;
  const tcCount = items.filter((i) => i.id.startsWith('TC-')).length;
  const auditCount = items.length - frCount - gateCount - tcCount;

  const sorted = [...items].sort((a, b) => {
    const ma = moduleSortKey(a.module);
    const mb = moduleSortKey(b.module);
    if (ma !== mb) return ma - mb;
    return a.id.localeCompare(b.id);
  });

  const lines = [];
  lines.push('# Functional Requirements Backlog — Master Document');
  lines.push('');
  lines.push(`**Generated:** ${date} · **Source:** MF-01/02/03 specs, enterprise/gate matrices, audit reports, E2E harness · **Total rows:** ${items.length}`);
  lines.push('');
  lines.push('## Legend');
  lines.push('');
  lines.push('| Priority | Meaning |');
  lines.push('|----------|---------|');
  lines.push('| P0 | Blocker / security / audit critical |');
  lines.push('| P1 | Core phase requirement |');
  lines.push('| P2 | Optional / enhancement |');
  lines.push('');
  lines.push('| Status | Meaning |');
  lines.push('|--------|---------|');
  lines.push('| Done | Implemented + verified (test or audit PASS) |');
  lines.push('| Planned | Specified; automation partial |');
  lines.push('| Manual | Requires hand playbook verification |');
  lines.push('| Deprecated | Removed by design (still listed for traceability) |');
  lines.push('');
  lines.push('## Summary');
  lines.push('');
  lines.push(`| Family | Count |`);
  lines.push(`|--------|------:|`);
  lines.push(`| FR-* (functional) | ${frCount} |`);
  lines.push(`| G* gate regression | ${gateCount} |`);
  lines.push(`| TC-* UAT | ${tcCount} |`);
  lines.push(`| Audit / SEC / UX / Portal | ${auditCount} |`);
  lines.push(`| **Total** | **${items.length}** |`);
  lines.push('');
  lines.push('## Master Backlog');
  lines.push('');
  lines.push('| Req ID | Module | Functional Requirement / User Story | Actor | Priority | Acceptance Criteria | Test / Demo | Scenario | UI / API / Page | Owner | Status | Evidence Link / Note |');
  lines.push('|--------|--------|-------------------------------------|-------|----------|---------------------|-------------|----------|-----------------|-------|--------|----------------------|');

  let lastModule = '';
  for (const item of sorted) {
    if (item.module !== lastModule) {
      lastModule = item.module;
      lines.push(`| **—** | **${escCell(item.module)}** | *Section* | | | | | | | | | |`);
    }
    lines.push(`| ${escCell(item.id)} | ${escCell(item.module)} | ${escCell(item.title)} | ${escCell(item.actor)} | ${escCell(item.priority)} | ${escCell(item.ac)} | ${escCell(item.test)} | ${escCell(item.scenario)} | ${escCell([item.ui, item.api].filter(Boolean).join(' · '))} | ${escCell(item.owner)} | ${escCell(item.status)} | ${escCell(item.evidence)} |`);
  }

  lines.push('');
  lines.push('---');
  lines.push('');
  lines.push('## Appendix A — FR ID Index');
  lines.push('');
  for (const item of sorted.filter((i) => i.id.startsWith('FR-')).sort((a, b) => a.id.localeCompare(b.id))) {
    lines.push(`- **${item.id}** — ${item.title} (${item.status})`);
  }

  lines.push('');
  lines.push('## Appendix B — API Endpoint Index (BE controllers)');
  lines.push('');
  for (const c of ['HackathonController', 'RoundController', 'TrackController', 'CriteriaController', 'EventController', 'TeamController', 'SubmissionController', 'ScoreController', 'RoundProgressionController', 'PresentationQueueController', 'HackathonClosureController', 'PrizeController', 'AuthController', 'RblDashboardController']) {
    lines.push(`- \`BE/src/main/java/com/sealhackathon/api/controller/${c}.java\``);
  }

  lines.push('');
  lines.push('## Appendix C — FE Route Index (by role)');
  lines.push('');
  lines.push('| Role | Routes |');
  lines.push('|------|--------|');
  lines.push('| Coordinator | `/hackathons`, `/hackathons/:id/setup`, `/teams`, `/coordinator/*`, `/admin/*` |');
  lines.push('| Student | `/student/team`, `/student/submit`, `/student/hackathons`, `/student/leaderboard` |');
  lines.push('| Judge | `/judge/dashboard`, `/judging/:roundId/scoring` |');
  lines.push('| Mentor | `/mentor/rounds`, `/mentor/support` |');
  lines.push('| Auth | `/login`, `/register`, `/onboarding` |');

  lines.push('');
  lines.push(`## Appendix D — E2E Spec Index (${e2eSpecs.length} files)`);
  lines.push('');
  for (const spec of e2eSpecs) {
    lines.push(`- [\`${spec}\`](${spec})`);
  }

  lines.push('');
  lines.push('## Appendix E — Happy Seed Slugs');
  lines.push('');
  lines.push('| Slug | Primary FR / Phase coverage |');
  lines.push('|------|----------------------------|');
  const seeds = [
    ['seal-e2e-2026', 'GD1-GD2 full setup; orphans; Spring gate FR-U-15-F'],
    ['seal-gd1-incomplete', 'GD1 readiness fail; event-notification'],
    ['seal-gd3-prelim-open', 'GD3 submit/score/mentor/websocket queue'],
    ['seal-gd4-advance-ready', 'GD4 publish/advance ready'],
    ['seal-gd4-tiebreak-manual', 'FR-22B tiebreak; TC-TB-01'],
    ['seal-gd4-tiebreak-submission-time', 'GD4 tiebreak by submission time'],
    ['seal-gd4-wildcard-gap', 'GD4 Top-N only (wildcard deprecated)'],
    ['seal-gd5-final-active', 'GD5 final submit/score'],
    ['seal-gd6-pending-confirm', 'GD6 closure confirm'],
    ['seal-fall-2025-finished', 'GD6 FINISHED; analytics unlock; individual ranking'],
  ];
  for (const [slug, cov] of seeds) {
    lines.push(`| \`${slug}\` | ${cov} |`);
  }

  lines.push('');
  lines.push('## Appendix F — Deprecated / Reserved');
  lines.push('');
  lines.push('- **FR-14, FR-19** — Reserved/unused in v4.1 spec (no row).');
  lines.push('- **FR-22A Wildcard** — Deprecated 2026-07-18; advance Top-N only.');
  lines.push('- **HEAD judge role** — Removed; use controller-grant TRANSFER (P0-HEAD).');
  lines.push('- **Register full form** — Reverted 2026-07-20 to email+password only; profile via onboarding (FR-07).');
  lines.push('');
  lines.push('## Appendix G — Source Documents');
  lines.push('');
  lines.push('- [BE/docs/mf01/02-functional-requirements.md](../BE/docs/mf01/02-functional-requirements.md) — GĐ1 FR-01..07B');
  lines.push('- [BE/docs/mf02/01-business-rules-gd2.md](../BE/docs/mf02/01-business-rules-gd2.md) — GĐ2 FR-11..13C');
  lines.push('- [BE/docs/mf03/01-business-rules-gd3.md](../BE/docs/mf03/01-business-rules-gd3.md) — GĐ3-5');
  lines.push('- [BE/docs/mf03/01-business-rules-gd6.md](../BE/docs/mf03/01-business-rules-gd6.md) — GĐ6 FR-31..36');
  lines.push('- [docs/testing/enterprise-regression-matrix-gd1-gd6.md](testing/enterprise-regression-matrix-gd1-gd6.md)');
  lines.push('- [BE/docs/testing/gate-regression-test-matrix-gd1-gd6.md](../BE/docs/testing/gate-regression-test-matrix-gd1-gd6.md)');
  lines.push('- [BE/docs/testing/manual-ui-playbook-gd1-gd6.md](../BE/docs/testing/manual-ui-playbook-gd1-gd6.md)');
  lines.push('- [BE/docs/testing/ui-audit-2026-07-19/deep/REPORT.md](../BE/docs/testing/ui-audit-2026-07-19/deep/REPORT.md)');
  lines.push('');
  lines.push('---');
  lines.push('');
  lines.push('*Regenerate: `node BE/docs/testing/scripts/build-fr-backlog-inventory.mjs`*');

  return lines.join('\n');
}

function verifyCoverage(items) {
  const REQUIRED_FR = [
    'FR-01','FR-02','FR-03','FR-04','FR-05','FR-05A','FR-06','FR-07','FR-07B','FR-08','FR-09','FR-10',
    'FR-11','FR-11C','FR-11D','FR-12','FR-13','FR-13A','FR-13B','FR-13B-R','FR-13C',
    'FR-15','FR-15A','FR-16','FR-16A','FR-17','FR-18','FR-18A','FR-20','FR-20A','FR-21',
    'FR-22A','FR-22B','FR-23','FR-24','FR-25','FR-26','FR-27','FR-28','FR-29','FR-30','FR-30A',
    'FR-31','FR-32','FR-33','FR-33A','FR-33B','FR-33C','FR-33D','FR-34','FR-35','FR-36',
  ];
  const frIds = new Set(items.filter((i) => i.id.startsWith('FR-')).map((i) => i.id));
  const missingFr = REQUIRED_FR.filter((id) => !frIds.has(id));
  const enterpriseContent = read('docs/testing/enterprise-regression-matrix-gd1-gd6.md');
  const enterpriseIds = [...enterpriseContent.matchAll(/^\| ([A-Z0-9_-]+) \|/gm)].map((m) => m[1]).filter((id) => id !== 'ID');
  const itemIds = new Set(items.map((i) => i.id));
  const missingEnterprise = enterpriseIds.filter((id) => !itemIds.has(id));
  const spotCheck = ['FR-01', 'FR-07', 'FR-11', 'FR-16', 'FR-18', 'FR-24', 'FR-30', 'FR-33', 'SEC-AUTH-01', 'TC-TB-01'];
  const spotResults = spotCheck.map((id) => {
    const row = items.find((i) => i.id === id);
    return { id, ok: !!row && (row.api || row.ui), row: row ? `${row.ui} | ${row.api}` : 'MISSING' };
  });
  const report = { missingFr, missingEnterprise, spotResults, total: items.length, frCount: frIds.size };
  console.log('\n=== Coverage Verification ===');
  console.log(`Total rows: ${report.total}`);
  console.log(`FR coverage: ${report.frCount}/${REQUIRED_FR.length}${missingFr.length ? ` MISSING: ${missingFr.join(', ')}` : ' OK'}`);
  console.log(`Enterprise matrix: ${missingEnterprise.length ? `MISSING ${missingEnterprise.join(', ')}` : 'all IDs present'}`);
  console.log('Spot-check (10 rows):');
  for (const s of spotResults) console.log(`  ${s.id}: ${s.ok ? 'OK' : 'FAIL'} — ${s.row}`);
  return report;
}

function main() {
  const frItems = FR_CATALOG.map((x) => ({ ...x, api: x.api, ui: x.ui }));
  const portalItems = [...PORTAL_CATALOG.map((x) => ({ ...x, api: x.api, ui: x.ui })), ...generateUserRoleCatalog()];
  const auditItems = AUDIT_CATALOG.map((x) => ({
    ...x,
    api: x.api || '',
    ui: x.ui || '',
    owner: x.owner || 'QA',
    evidence: x.evidence.startsWith('REPORT') ? `BE/docs/testing/ui-audit-2026-07-19/deep/${x.evidence}` : x.evidence,
  }));

  const gateItems = parseGateMatrix();
  const enterpriseItems = parseEnterpriseMatrix();
  const tcItems = [...parseTcGd1(), ...generateTcGd1Full()];
  const auditJsonItems = parseAuditResultsJson();

  const all = dedupeById([
    ...frItems,
    ...portalItems,
    ...auditItems,
    ...auditJsonItems,
    ...gateItems,
    ...enterpriseItems,
    ...tcItems,
  ]);

  const e2eSpecs = listE2eSpecs();
  const verification = verifyCoverage(all);
  const inventory = {
    generatedAt: new Date().toISOString(),
    counts: {
      total: all.length,
      fr: all.filter((i) => i.id.startsWith('FR-')).length,
      gate: all.filter((i) => /^G[1-6]-/.test(i.id)).length,
      tc: all.filter((i) => i.id.startsWith('TC-')).length,
      u: all.filter((i) => i.id.startsWith('U-')).length,
    },
    verification,
    e2eSpecs,
    items: all,
  };

  fs.mkdirSync(path.dirname(OUT_JSON), { recursive: true });
  fs.writeFileSync(OUT_JSON, JSON.stringify(inventory, null, 2), 'utf8');
  fs.writeFileSync(OUT_MD, buildMarkdown(all, e2eSpecs), 'utf8');

  console.log(`\nWrote ${OUT_JSON} (${all.length} items)`);
  console.log(`Wrote ${OUT_MD}`);
  console.log(`  FR: ${inventory.counts.fr}, Gate: ${inventory.counts.gate}, TC: ${inventory.counts.tc}, U: ${inventory.counts.u}`);
}

main();
