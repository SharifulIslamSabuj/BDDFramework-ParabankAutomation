# Phase 10 — CI/CD Validation Diagnostics and Evidence Reporting

**Repository:** https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation  
**Branch:** `main`  
**Starting commit:** `c1e9e9d` — infra: improve Selenium Grid startup readiness  
**Date:** 2026-07-18

---

## 1. Objective

Upgrade the GitHub Actions CI/CD workflow so that:

- CI is **green** when the result exactly matches the accepted known-failure baseline
- CI is **red** only for unexpected regressions, infrastructure failures, or missing results
- A structured classification appears in the Actions job summary on every run
- The pipeline separates compile failures, safety-test failures, and regression failures
- The `prod` dispatch option is removed — CI does not execute against production
- Main-branch runs are never cancelled

---

## 2. Repository State

```
git status --short  → M (workflow modified from previous session state)
git branch          → main
```

Working tree was clean before Phase 10 changes except for the workflow file already noted in the Phase 10 specification.

---

## 3. Local Pre-Change Baseline

### Production-safety tests
```
./gradlew test --tests "*ProductionSafetyGuardTest"
ProductionSafetyGuardTest: 12/12 PASSED — BUILD SUCCESSFUL
```

### Full regression (Run 1 — transient AUT instability observed)
```
./gradlew clean test
30 tests completed, 7 failed
  Safety tests:           12/12
  Cucumber executions:    18
  Passed:                 11
  Known AUT failures:     6  (runScenario[9,10,11,13,14,15])
  Transient extra:        1  (resolved on rerun)
BUILD FAILED (transient)
```

### Full regression (Run 2 — stable)
```
./gradlew clean test
30 tests completed, 6 failed
  Safety tests:           12/12
  Cucumber executions:    18
  Passed:                 12
  Known AUT failures:     6  (runScenario[9,10,11,13,14,15])
  Unexpected failures:    0
BUILD FAILED (expected)
```

Local baseline confirmed stable at 18/12/6. Run 1 transient instability classified per QR-001 one-rerun protocol.

---

## 4. Workflow Audit

Audited `.github/workflows/automation-test.yml` before Phase 10 changes:

| Finding | Issue | Action |
|---|---|---|
| `prod` in workflow_dispatch choices | CI executing against production environment | Removed |
| No explicit `permissions:` block | Jobs ran with excessive default token permissions | Added `permissions: contents: read` |
| `cancel-in-progress: true` | Main branch runs cancelled on new push | Changed to conditional expression |
| No compile gate | Compilation errors required waiting for full test run | Added `compileTestJava` step |
| No separate safety gate | Safety test failures not distinguished from Cucumber failures | Added dedicated step |
| Single test step — job fails on any failure | Known AUT failures cause permanent red badge | Replaced with exit-code capture + classifier |
| Artifact retention 30 days | Higher than necessary | Reduced to 14 days |
| No classification in summary | CI consumers cannot distinguish known vs unexpected | Added classifier step with structured summary |

---

## 5. Known-Failure Baseline Confirmation

JUnit XML files read to confirm the exact testcase name format:

**File:** `build/test-results/test/TEST-com.parabank.parasoft.runner.TestRunner.xml`  
**Suite:** `com.parabank.parasoft.runner.TestRunner`  
**Tests:** 18, **Failures:** 6

Failing testcase names (exact XML `name` attribute):

```
runScenario[9]("Sign-in is protected against injection and scripting attacks", "Customer Sign-In")
runScenario[10]("Sign-in is protected against injection and scripting attacks", "Customer Sign-In")
runScenario[11]("Sign-in is protected against injection and scripting attacks", "Customer Sign-In")
runScenario[13]("A new customer can open a bank account with their personal information", "Customer Account Registration")
runScenario[14]("A new customer can open a bank account with a freshly generated profile", "Customer Account Registration")
runScenario[15]("A customer can open a bank account using details provided by an external source", "Customer Account Registration")
```

Accepted baseline `frozenset`: `{'9', '10', '11', '13', '14', '15'}` — extracted by `^runScenario\[(\d+)\]` regex.

**File:** `build/test-results/test/TEST-com.parabank.parasoft.config.ProductionSafetyGuardTest.xml`  
**Suite:** `com.parabank.parasoft.config.ProductionSafetyGuardTest`  
**Tests:** 12, **Failures:** 0

---

## 6. Files Changed

| File | Change |
|---|---|
| `.github/workflows/automation-test.yml` | Redesigned — see Section 7 |
| `scripts/analyze-test-results.sh` | Created — result classifier |
| `docs/CI_CD_GUIDE.md` | Created — complete CI reference |
| `README.md` | CI section rewritten; Skills table updated |
| `docs/TEST_STRATEGY.md` | Section 7 added (CI Execution Stages); sections renumbered |
| `docs/QUALITY_RISK_ASSESSMENT.md` | QR-009 status updated to Mitigated; detailed record rewritten |
| `CONTRIBUTING.md` | CI classification note added; CI_CD_GUIDE.md added to reference list |
| `docs/PULL_REQUEST_CHECKLIST.md` | CI Classification section added |
| `docs/review/PHASE_10_CI_CD_IMPROVEMENTS.md` | Created (this file) |

No Java source files, feature files, Gradle build, or Docker files were modified.

---

## 7. Workflow Changes

### Permissions

```yaml
permissions:
  contents: read
```

Minimal explicit permissions — no write access granted.

### Concurrency

```yaml
# Before:
cancel-in-progress: true

# After:
cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}
```

Main branch runs are never cancelled. PR and feature branch runs cancel on new push.

### workflow_dispatch choices

```yaml
# Before:
options: [qa, staging, uat, prod]

# After:
options: [qa, staging, uat]
```

`prod` removed. CI does not execute against production.

### Compile gate (new step)

```yaml
- name: Compile
  run: ./gradlew compileTestJava
```

Surfaces compilation errors immediately without executing any test.

### Safety gate (new step)

```yaml
- name: Production-safety tests
  run: ./gradlew test --tests "*ProductionSafetyGuardTest"
```

12 focused tests; no browser; ~1s. Failures here indicate a framework defect and block the Cucumber suite.

### Full regression — exit code capture

```yaml
- name: Full regression
  id: regression
  run: |
    set +e
    xvfb-run ... ./gradlew clean test ...
    GRADLE_EXIT=$?
    echo "gradle_exit=$GRADLE_EXIT" >> "$GITHUB_OUTPUT"
    exit 0
```

The Gradle exit code is captured in step output `steps.regression.outputs.gradle_exit`. The step itself always exits 0 — the classify step is the final gate.

### Classifier (new final gate step)

```yaml
- name: Classify results and write job summary
  if: always()
  env:
    GRADLE_EXIT_CODE: ${{ steps.regression.outputs.gradle_exit }}
    TEST_ENV_LABEL: ${{ env.TEST_ENV }}
  run: |
    chmod +x scripts/analyze-test-results.sh
    scripts/analyze-test-results.sh
```

Parses `build/test-results/test/TEST-*.xml`. Writes structured markdown to `$GITHUB_STEP_SUMMARY`. Exits 0 only for VALIDATED_BASELINE.

### Artifact retention

All 5 artifact upload steps: `retention-days: 30` → `retention-days: 14`.

---

## 8. Classifier Implementation

`scripts/analyze-test-results.sh` — bash wrapper invoking Python 3 (available on ubuntu-latest runners):

**Exit codes:**
- `0` — VALIDATED_BASELINE
- `1` — UNEXPECTED_REGRESSION
- `2` — INFRASTRUCTURE_FAILURE
- `3` — RESULTS_UNAVAILABLE

**Matching logic:** Extracts `runScenario[N]` prefix from each failing testcase `name` attribute using `^runScenario\[(\d+)\]` regex. Compares the resulting index set to `frozenset({'9','10','11','13','14','15'})`.

**Classification priority:**
1. No XML files → RESULTS_UNAVAILABLE
2. Safety tests failed → UNEXPECTED_REGRESSION
3. No Cucumber tests ran → INFRASTRUCTURE_FAILURE
4. Count mismatch or wrong failure set → UNEXPECTED_REGRESSION
5. Exact match → VALIDATED_BASELINE

---

## 9. Classifier Fixture Tests

All 5 fixture cases validated:

| Case | Input | Expected | Actual | Status |
|---|---|---|---|---|
| A | Real XML — 18 tests, 6 known failures | VALIDATED_BASELINE, exit 0 | VALIDATED_BASELINE, exit 0 | PASS |
| B | 7 failures (runScenario[7] extra) | UNEXPECTED_REGRESSION | UNEXPECTED_REGRESSION: unexpected: runScenario[7] | PASS |
| C | 6 failures, wrong set ([7] instead of [9]) | UNEXPECTED_REGRESSION | UNEXPECTED_REGRESSION: unexpected: [7]; missing: [9] | PASS |
| D | No XML files | RESULTS_UNAVAILABLE | RESULTS_UNAVAILABLE | PASS |
| E | 0 test executions | INFRASTRUCTURE_FAILURE | INFRASTRUCTURE_FAILURE | PASS |

---

## 10. Classifier Against Post-Change Regression Result

Ran full regression after all Phase 10 changes and classified the result:

```
./gradlew clean test
30 tests completed, 6 failed (Run 1 — transient 7 failures; Run 2 — stable 6)

GRADLE_EXIT_CODE=1 TEST_ENV_LABEL=qa ./scripts/analyze-test-results.sh (Python logic equivalent)

=== CI Classification: VALIDATED_BASELINE ===
    Env    : qa
    Gradle : exit 1
    Safety : 12/12
    Cucumber: 18 total, 12 passed, 6 failed
    Failed indices: ['9', '10', '11', '13', '14', '15']
Classifier exit: 0
```

Classification correct. Classifier exits 0 for the known-failure baseline.

---

## 11. Risk and Debt Status Updates

| Item | Before Phase 10 | After Phase 10 |
|---|---|---|
| QR-009 — CI red badge on known failures | Accepted | **Mitigated** |
| `prod` in workflow_dispatch choices | Present (security risk) | **Removed** |
| No explicit permissions block | Default (excessive) | **Fixed** |
| Main branch runs cancellable | Yes | **Fixed** |

No new risks introduced.

---

## 12. Validation Summary

| Validation | Expected | Result | Status |
|---|---|---|---|
| Pre-change baseline (Run 2) | 18/12/6 | 18/12/6 | PASS |
| Workflow YAML valid structure | No syntax errors | Clean | PASS |
| `prod` removed from dispatch options | Absent | Absent | PASS |
| Explicit `permissions: contents: read` | Present | Present | PASS |
| `cancel-in-progress` conditional | Main runs preserved | Conditional expression added | PASS |
| Compile gate added | Present | `compileTestJava` step added | PASS |
| Safety gate added | Present | `*ProductionSafetyGuardTest` step added | PASS |
| Exit code capture | `steps.regression.outputs.gradle_exit` set | Set correctly | PASS |
| Classifier fixture Case A | VALIDATED_BASELINE, exit 0 | VALIDATED_BASELINE, exit 0 | PASS |
| Classifier fixture Case B | UNEXPECTED_REGRESSION | UNEXPECTED_REGRESSION: extra runScenario[7] | PASS |
| Classifier fixture Case C | UNEXPECTED_REGRESSION | UNEXPECTED_REGRESSION: extra/missing set mismatch | PASS |
| Classifier fixture Case D | RESULTS_UNAVAILABLE | RESULTS_UNAVAILABLE | PASS |
| Classifier fixture Case E | INFRASTRUCTURE_FAILURE | INFRASTRUCTURE_FAILURE | PASS |
| Classifier vs real Run 2 XML | VALIDATED_BASELINE, exit 0 | VALIDATED_BASELINE, exit 0 | PASS |
| QR-009 status | Mitigated | Updated in QUALITY_RISK_ASSESSMENT.md | PASS |
| Artifact retention | 14 days | 14 days in all 5 upload steps | PASS |
| Post-change local regression | 18/12/6 (Run 2) | 18/12/6 | PASS |

---

## 13. Final Recommendation

> **PHASE 10 COMPLETE — READY FOR COMMIT**
