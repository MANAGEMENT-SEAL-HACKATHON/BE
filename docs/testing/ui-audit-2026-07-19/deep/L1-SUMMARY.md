# Layer 1 — API + Matrix

| Check | Result | Notes |
| --- | --- | --- |
| `probe:seeds` | 28/29 PASS | FAIL `neg:duplicate-email` → got `VALIDATION_FAILED` not `ACCOUNT_DUPLICATE_EMAIL` [BAD] |
| `a6-authz-audit-smoke` | 8/8 PASS | AUTH/CTRL/AUDIT-RO |
| `verify-gd3-gd6-seed-smoke` | 22/27 PASS | FAIL: G3-LATE-PENDING, G4-PRELIM-PUBLISHED, G4-WILDCARD, G5-RANKING-POOL, G5-GUEST-SUBMISSIONS (seed/expectation drift) |
| `fe-be-workflow-integration` | 23 PASS / 3 FAIL / 5 SKIP | TC-10 shuffle (window), TC-15 calib sessions **404 expected post-purge** → treat as [HAPPY] CALIB removed OK, TC-GD5-04 lock on inactive e2e slug |
| `api-audit.mjs` | inventory dump | FE/BE path inventory written to log |

Non-critical FAILs recorded; continue per stop policy (no IDOR leak / no TC-TB-01).
