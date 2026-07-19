# Layer 0 — Unit / Integration

| Check | Result | Evidence |
| --- | --- | --- |
| FE `test:unit:all` | PASS | 32/32 + SEC-CLOU-02 PASS |
| FE `vi-ui-blocklist-check` | PASS | 245 files, 0 hits |
| FE `lint` | FAIL (pre-existing) | 12 errors / 193 warnings — parsing `test` redeclare in e2e?, react-hooks impure render; non-blocking for pyramid (recorded) |
| BE `mvn test` | PASS after fix | Was 419/420; fixed `Gd4ToGd6FlowIntegrationTest` expect `RBL_VARIANCE_AGGREGATE`; re-run class PASS |

## Fix applied (L0)
- [Gd4ToGd6FlowIntegrationTest.java](../../../src/test/java/com/sealhackathon/api/integration/Gd4ToGd6FlowIntegrationTest.java): section header assertion aligned with `ExportCsvBuilder` (`RBL_VARIANCE_AGGREGATE`).
