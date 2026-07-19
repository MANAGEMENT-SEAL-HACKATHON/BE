# Layer 3.5 — ID ledger (from L3 deep-audit + probes)

Prefix `[HAPPY]|[BAD]|[SAB]`. Source: `ui-ux-deep-audit --phase=all` (2026-07-19) + `REPORT-L35-probes.md`.

## Wired / closed in L3 (sample — full detail in REPORT-gd*.md / REPORT.md)

| ID | Lane | Kết luận | Evidence |
| --- | --- | --- | --- |
| P0-WC / P0-HEAD / P0-PG | [HAPPY] | PASS | REPORT.md Phase 0 |
| COORD-SCORE-ALL-01* / STU-SCORE-01* | [HAPPY] | PASS | SCORE phase |
| SH-01 / LOCK-03 / LOTTERY-GATE-01 | [HAPPY] | PASS | GĐ3 |
| LOTTERY-DATA-01 | [HAPPY] | FAIL | listed=null after L3 mutation / ACTIVE=0 |
| PUB-01 / CSV-01 / AUDIT-RO-01 | [HAPPY] | PASS | GĐ6/PUB |
| CALIB-01-ANALYTICS / RBL-CALIB-01-UI | [HAPPY] | PASS | Calibration purged |
| BC3 / BC5 / BC6 | [BAD]/[SAB] | PASS | REPORT-negative |
| IDOR-01..08 | [SAB] | PASS | REPORT-negative |
| VALID-02..05 | [BAD] | PASS | REPORT-negative |
| UX-CTX-01 / I5 / I2 / AR | [HAPPY] | PASS | CROSS |
| THESIS-RBL-02 | [HAPPY] | FAIL | variance anon=false |
| RBL-PROGRESS-FINISHED / INTERRATER | [HAPPY] | PASS | L35 probes |

## SKIP → L3.5 (deferred / needs live)

| ID | Lane | Kết luận | Lý do |
| --- | --- | --- | --- |
| EARLY-WAIT-01 | [HAPPY] | SKIP | Needs pre-examAt window |
| SH-02 / STT-01 / TIMER-RT-01 | [HAPPY] | SKIP | Needs PRESENTING (TIMER-RT-01-GD5 / STT-01-GD5 PASS on CK) |
| BC1 / BC2 / BC4 | [BAD] | SKIP | no judge/submission/criterion ctx |
| CTRL-01 / FAIL-01 / HEART / XFER / WS-DB / LATE-01 | [HAPPY] | SKIP | deferred→L5 / live queue |
| FAIL-02 | [SAB] | SKIP | deferred→L5 concurrent-race |
| FAIL-03 / PUB-02 / INVARIANT-01/02 / PRIZE-02 | [HAPPY] | SKIP | mutating avoid / soft-hide |
| I1 / I3 / J2 / J3 / H-READONLY | [HAPPY] | SKIP | see deep-audit |
| TC-TB-01 | [HAPPY] | SKIP | ghost-tiebreak hand — CRITICAL backlog |

## Missing → L3.5 (H sub-bugs)

| # | Sub-bug | Kết luận | Note |
| --- | --- | --- | --- |
| 1 | Cân bằng >1.0 | SKIP | H-FORM-CRITERIA top-level only (canBang=false in audit) — needs weight>1 UI |
| 2 | SOFT_SKILL label | SKIP | hand UI |
| 3 | min/max track popup | SKIP | hand UI |
| 4 | Không Bán kết | PASS | H-FORM-ROUND noBanKet=true |
| 5 | Bản nháp default | SKIP | hand |
| 6 | Chờ chốt sổ copy | SKIP | hand on PENDING_CONFIRM |
| 7 | Luật xử lý đồng điểm | SKIP | hand |
| 8–9 | BXH / Cân bằng UX | SKIP | hand |

## Catalog BAD (~23) — API sample

See [REPORT-L35-probes.md](REPORT-L35-probes.md). Full hand walk of remaining ErrorCodes still required for exit criteria completeness; EXTERNAL_JUDGE + READINESS_INCOMPLETE + IDOR PASS in this run.

**L3 was MUTATING** — BE reseed required before Layer 4.
