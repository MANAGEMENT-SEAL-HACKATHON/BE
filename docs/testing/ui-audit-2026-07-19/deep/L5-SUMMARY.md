# Layer 5 — Mutating E2E

| Result | 20 passed / 26 skipped / 0 failed (exit 0) |
| --- | --- |
| Mode B Continuous UI GĐ1→GĐ6 | **PASS** (create → FINISHED + export) |
| permission-idor-mutating | Partial PASS (foreign submit/approve/timer/STOMP); some skipped (purged seeds) |
| Fully skipped suites | abuse-guards, websocket-queue-timer, mentor-portal-mutating, event-notification, 5-secondary-portals, fall-track-select-mutating (`test.skip(true)` deprecated seeds) |
| FAIL-02 / concurrent-race | Skipped (file-level skip patterns) — still deferred |

[SAB] IDOR core cases PASS. Full Mode B = strongest HAPPY+mutating evidence this run.
