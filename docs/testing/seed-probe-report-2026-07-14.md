# Seed API probe report — 2026-07-14

**Result:** 7/26 passed

| Slug | Status | Reason | Detail |
|------|--------|--------|--------|
| `seal-e2e-2026` | PASS |  |  |
| `seal-fall-2025-finished` | PASS |  |  |
| `seal-gd3-prelim-open` | FAIL | slug not found in API |  |
| `seal-gd4-advance-ready` | FAIL | slug not found in API |  |
| `seal-gd5-final-active` | FAIL | slug not found in API |  |
| `seal-gd6-pending-confirm` | FAIL | slug not found in API |  |
| `account:unverified-student` | FAIL | expected EMAIL_NOT_VERIFIED, got INVALID_CREDENTIALS |  |
| `account:pending-mentor` | FAIL | expected ACCOUNT_PENDING, got INVALID_CREDENTIALS |  |
| `account:pending-judge` | FAIL | expected ACCOUNT_PENDING, got INVALID_CREDENTIALS |  |
| `account:rejected-judge` | FAIL | expected REJECTED_NOT_ALLOWED_LOGIN, got INVALID_CREDENTIALS |  |
| `account:approved-unverified-mentor` | FAIL | expected EMAIL_NOT_VERIFIED, got INVALID_CREDENTIALS |  |
| `neg:team-on-archived` | FAIL | Email hoặc mật khẩu không đúng |  |
| `neg:user-in-another-team` | FAIL | Email hoặc mật khẩu không đúng |  |
| `neg:registration-elsewhere` | FAIL | Email hoặc mật khẩu không đúng |  |
| `neg:invalid-repo-platform` | FAIL | Email hoặc mật khẩu không đúng |  |
| `neg:scoring-not-open` | FAIL | seal-gd3-prelim-open not found |  |
| `neg:archived-mutation` | PASS |  |  |
| `neg:oauth-token-invalid` | PASS |  |  |
| `neg:duplicate-email` | PASS |  |  |
| `neg:invalid-credentials` | PASS |  |  |
| `neg:forbidden-wrong-role` | FAIL | Email hoặc mật khẩu không đúng |  |
| `neg:scoring-locked` | FAIL | seal-gd4-advance-ready not found |  |
| `neg:email-verification-token-invalid` | PASS |  |  |
| `neg:journey-idor` | FAIL | Email hoặc mật khẩu không đúng |  |
| `neg:queue-cross-hackathon` | FAIL | Email hoặc mật khẩu không đúng |  |
| `neg:student-score-forbidden` | FAIL | Email hoặc mật khẩu không đúng |  |
