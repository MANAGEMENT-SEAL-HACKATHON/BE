# Seed API probe report — 2026-07-12

**Result:** 52/73 passed

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
| `seal-gd2-registration-closed` | PASS |  |  |
| `seal-gd2-lottery-not-locked` | PASS |  |  |
| `seal-gd2-round-active` | PASS |  |  |
| `seal-fall-ongoing-2026` | PASS |  |  |
| `seal-gd3-prelim-open` | PASS |  |  |
| `seal-gd3-late-review` | PASS |  |  |
| `seal-gd3-scoring-live` | PASS |  |  |
| `seal-gd3-scoring-gate` | PASS |  |  |
| `seal-gd3-tiebreak-hybrid` | PASS |  |  |
| `seal-gd3-edge-errors` | PASS |  |  |
| `seal-gd3-calibration-timer` | PASS |  |  |
| `seal-gd3-judge-mentor-conflict` | PASS |  |  |
| `seal-gd3-round-config-edge` | PASS |  |  |
| `seal-gd3-no-lottery` | PASS |  |  |
| `seal-gd3-mentor-portal` | PASS |  |  |
| `seal-gd3-mentor-track-only` | PASS |  |  |
| `seal-gd3-team-mentor-history` | PASS |  |  |
| `seal-gd4-advance-ready` | PASS |  |  |
| `seal-gd4-ck-unpublished` | PASS |  |  |
| `seal-gd4-published` | PASS |  |  |
| `seal-gd4-tiebreak-gate` | PASS |  |  |
| `seal-gd4-ck-activate-ready` | PASS |  |  |
| `seal-gd4-edge-errors` | PASS |  |  |
| `seal-gd4-wildcard-resolved` | PASS |  |  |
| `seal-gd4-tiebreak-resolved` | PASS |  |  |
| `seal-gd4-wildcard-disabled` | PASS |  |  |
| `seal-gd4-judge-assign-warnings` | PASS |  |  |
| `seal-gd4-ck-no-criteria` | PASS |  |  |
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
| `neg:registration-elsewhere` | PASS |  |  |
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
| `neg:fall-track-cross-season` | PASS |  |  |
