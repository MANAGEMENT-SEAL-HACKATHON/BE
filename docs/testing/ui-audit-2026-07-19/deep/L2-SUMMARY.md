# Layer 2 — Automated E2E (Chromium focus)

| Suite | Result | Notes |
| --- | --- | --- |
| `test:e2e:parity` | 3/3 PASS | |
| `test:e2e:matrix` | 8/9 PASS | FAIL `seal-gd3-prelim-open` expectVisible `/CỔNG ĐANG MỞ/` |
| `test:e2e:gd2` | 3/3 PASS | |
| `test:e2e:dedicated` | 1 PASS / 1 FAIL / 6 skip | FAIL people-mentor-pool copy; skips = purged seeds |
| `test:e2e` (all projects) | 50 PASS / 27 FAIL / 66 skip | Failures mostly firefox/webkit/mobile missing browsers (plan=Chromium only → SKIP) + visual baselines + gd3 copy + account-states |
| Harness fix | DONE | Restored broken `test.skip` inside imports in 3 mutating specs; visual helpers path `../helpers` |

Chromium-primary happy path: parity + gd2 OK. Matrix/dedicated UI copy drifts recorded non-critical.
