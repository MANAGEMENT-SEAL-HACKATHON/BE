# Seed API probe report — 2026-07-16

**Result:** 23/26 passed

| Slug | Status | Reason | Detail |
|------|--------|--------|--------|
| `seal-e2e-2026` | FAIL | status expected ONGOING, got FINISHED |  |
| `seal-fall-2025-finished` | PASS |  |  |
| `seal-gd3-prelim-open` | PASS |  |  |
| `seal-gd4-advance-ready` | PASS |  |  |
| `seal-gd5-final-active` | PASS |  |  |
| `seal-gd6-pending-confirm` | PASS |  |  |
| `account:unverified-student` | PASS |  |  |
| `account:pending-mentor` | PASS |  |  |
| `account:pending-judge` | PASS |  |  |
| `account:rejected-judge` | PASS |  |  |
| `account:approved-unverified-mentor` | PASS |  |  |
| `neg:team-on-archived` | PASS |  |  |
| `neg:user-in-another-team` | FAIL | expected one of [USER_IN_ANOTHER_TEAM], got HACKATHON_NOT_ONGOING — Hackathon chưa mở đăng ký (không phải ONGOING) |  |
| `neg:registration-elsewhere` | FAIL | expected one of [REGISTRATION_ALREADY_ACTIVE_ELSEWHERE], got REGISTRATION_CLOSED — Thời gian đăng ký đã kết thúc. |  |
| `neg:invalid-repo-platform` | PASS |  |  |
| `neg:scoring-not-open` | PASS |  |  |
| `neg:archived-mutation` | PASS |  |  |
| `neg:oauth-token-invalid` | PASS |  |  |
| `neg:duplicate-email` | PASS |  |  |
| `neg:invalid-credentials` | PASS |  |  |
| `neg:forbidden-wrong-role` | PASS |  |  |
| `neg:scoring-locked` | PASS |  |  |
| `neg:email-verification-token-invalid` | PASS |  |  |
| `neg:journey-idor` | PASS |  |  |
| `neg:queue-cross-hackathon` | PASS |  |  |
| `neg:student-score-forbidden` | PASS |  |  |
