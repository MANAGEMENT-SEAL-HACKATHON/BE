# Layer 4 — HAPPY API chains

| Chain | Result | Notes |
| --- | --- | --- |
| `gd3-gd4-gd5-full-chain-api.mjs` (seal-e2e-2026) | 14 PASS / 8 FAIL / 8 SKIP | Blocked at G3-RELEASE-PROBLEM (`EARLY-WAIT` — chưa tới giờ thi) → close-sub/lock/publish cascade FAIL |
| `gd6-full-chain-api.mjs` | 8 PASS / 0 FAIL / 1 SKIP | Confirm → FINISHED + export DONE |
| `phase-b-critical-api.mjs` | Most PASS; 1 CRITICAL FAIL G5-07 participating=0; G2-03b 404 | |

**Root cause L4 main chain (historical):** START_NOW activate gated release-problem on examAt. **Note (phase 2):** START_NOW removed — use «Dời lịch thi» then KEEP activate when examAt≤now. Non-blocking for L5 after reseed.
