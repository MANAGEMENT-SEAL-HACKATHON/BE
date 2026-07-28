# Layer 4 — HAPPY API chains

| Chain | Result | Notes |
| --- | --- | --- |
| `gd3-gd4-gd5-full-chain-api.mjs` (seal-e2e-2026) | 14 PASS / 8 FAIL / 8 SKIP | Blocked at G3-RELEASE-PROBLEM (`EARLY-WAIT` — chưa tới giờ thi) → close-sub/lock/publish cascade FAIL |
| `gd6-full-chain-api.mjs` | 8 PASS / 0 FAIL / 1 SKIP | Confirm → FINISHED + export DONE |
| `phase-b-critical-api.mjs` | Most PASS; 1 CRITICAL FAIL G5-07 participating=0; G2-03b 404 | |

**Root cause L4 main chain:** START_NOW activate still gates release-problem on examAt; chain needs reschedule-before-release or seed with examAt≤now. Non-blocking for L5 after reseed.
