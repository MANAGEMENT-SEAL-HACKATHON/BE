# Seed API probe report — 2026-07-13

**Result:** 24/76 passed

| Slug | Status | Reason | Detail |
|------|--------|--------|--------|
| `seal-e2e-2026` | PASS |  |  |
| `seal-fall-2025-finished` | FAIL | Email hoặc mật khẩu không đúng | INVALID_CREDENTIALS |
| `seal-gd1-incomplete` | PASS |  |  |
| `seal-gd1-no-kickoff` | PASS |  |  |
| `seal-gd1-no-awards` | PASS |  |  |
| `seal-gd1-judge-final-early` | PASS |  |  |
| `seal-gd1-event-order-bad` | PASS |  |  |
| `seal-gd1-event-order-violation` | PASS |  |  |
| `seal-gd1-prelim-only` | PASS |  |  |
| `seal-gd2-teams-edge` | PASS |  |  |
| `seal-gd2-registration-closed` | FAIL | slug not found in API |  |
| `seal-gd2-lottery-not-locked` | FAIL | slug not found in API |  |
| `seal-gd2-round-active` | FAIL | slug not found in API |  |
| `seal-fall-ongoing-2026` | FAIL | slug not found in API |  |
| `seal-gd3-prelim-open` | FAIL | slug not found in API |  |
| `seal-gd3-late-review` | FAIL | slug not found in API |  |
| `seal-gd3-scoring-live` | FAIL | slug not found in API |  |
| `seal-gd3-scoring-gate` | FAIL | slug not found in API |  |
| `seal-gd3-tiebreak-hybrid` | FAIL | slug not found in API |  |
| `seal-gd3-edge-errors` | FAIL | slug not found in API |  |
| `seal-gd3-calibration-timer` | FAIL | slug not found in API |  |
| `seal-gd3-judge-mentor-conflict` | FAIL | slug not found in API |  |
| `seal-gd3-round-config-edge` | FAIL | slug not found in API |  |
| `seal-gd3-no-lottery` | FAIL | slug not found in API |  |
| `seal-gd3-mentor-portal` | FAIL | slug not found in API |  |
| `seal-gd3-mentor-track-only` | FAIL | slug not found in API |  |
| `seal-gd3-team-mentor-history` | FAIL | slug not found in API |  |
| `seal-gd4-advance-ready` | FAIL | slug not found in API |  |
| `seal-gd4-ck-unpublished` | FAIL | slug not found in API |  |
| `seal-gd4-published` | FAIL | slug not found in API |  |
| `seal-gd4-tiebreak-gate` | FAIL | slug not found in API |  |
| `seal-gd4-ck-activate-ready` | FAIL | slug not found in API |  |
| `seal-gd4-edge-errors` | FAIL | slug not found in API |  |
| `seal-gd4-wildcard-resolved` | FAIL | slug not found in API |  |
| `seal-gd4-tiebreak-resolved` | FAIL | slug not found in API |  |
| `seal-gd4-wildcard-disabled` | FAIL | slug not found in API |  |
| `seal-gd4-judge-assign-warnings` | FAIL | slug not found in API |  |
| `seal-gd4-ck-no-criteria` | FAIL | slug not found in API |  |
| `seal-gd5-final-active` | FAIL | slug not found in API |  |
| `seal-gd5-submit-open` | FAIL | slug not found in API |  |
| `seal-gd5-scoring-live` | FAIL | slug not found in API |  |
| `seal-gd5-calibration-timer` | FAIL | slug not found in API |  |
| `seal-gd5-edge-errors` | FAIL | slug not found in API |  |
| `seal-gd5-late-hardlock` | FAIL | slug not found in API |  |
| `seal-gd5-judge-edge` | FAIL | slug not found in API |  |
| `seal-gd5-late-pending` | FAIL | slug not found in API |  |
| `seal-gd5-not-advanced` | FAIL | slug not found in API |  |
| `seal-gd6-pending-confirm` | FAIL | slug not found in API |  |
| `seal-gd6-prizes-empty` | FAIL | slug not found in API |  |
| `seal-gd6-confirm-ready` | FAIL | slug not found in API |  |
| `seal-gd6-finished-export` | FAIL | slug not found in API |  |
| `seal-gd6-edge-errors` | FAIL | slug not found in API |  |
| `seal-gd6-prize-duplicate` | FAIL | slug not found in API |  |
| `account:unverified-student` | FAIL | expected EMAIL_NOT_VERIFIED, got INVALID_CREDENTIALS |  |
| `account:pending-mentor` | FAIL | expected ACCOUNT_PENDING, got INVALID_CREDENTIALS |  |
| `account:pending-judge` | FAIL | expected ACCOUNT_PENDING, got INVALID_CREDENTIALS |  |
| `account:rejected-judge` | FAIL | expected REJECTED_NOT_ALLOWED_LOGIN, got INVALID_CREDENTIALS |  |
| `account:approved-unverified-mentor` | FAIL | expected EMAIL_NOT_VERIFIED, got INVALID_CREDENTIALS |  |
| `neg:team-on-draft` | PASS |  |  |
| `neg:user-in-another-team` | PASS |  |  |
| `neg:registration-elsewhere` | FAIL | seal-gd3-prelim-open not found |  |
| `neg:invalid-repo-platform` | FAIL | Email hoặc mật khẩu không đúng |  |
| `neg:scoring-not-open` | FAIL | seal-gd3-scoring-gate not found |  |
| `neg:archived-mutation` | PASS |  |  |
| `neg:oauth-token-invalid` | PASS |  |  |
| `neg:duplicate-email` | PASS |  |  |
| `neg:invalid-credentials` | PASS |  |  |
| `neg:forbidden-wrong-role` | PASS |  |  |
| `neg:scoring-locked` | PASS |  |  |
| `neg:email-verification-token-invalid` | PASS |  |  |
| `neg:journey-idor` | PASS |  |  |
| `neg:queue-cross-hackathon` | PASS |  |  |
| `neg:fall-track-cross-season` | PASS |  |  |
| `neg:student-score-forbidden` | PASS |  |  |
| `neg:guest-score-unassigned` | PASS |  |  |
| `neg:rbl-progress-readable` | PASS |  |  |
