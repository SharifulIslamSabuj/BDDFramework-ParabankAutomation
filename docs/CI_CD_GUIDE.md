# CI/CD Guide

**Framework:** ParaBank BDD Automation  
**Workflow file:** `.github/workflows/automation-test.yml`  
**Classifier:** `scripts/analyze-test-results.sh`  
**Last Updated:** 2026-07-18

---

## Related Documents

| Document | Purpose |
|---|---|
| [TEST_STRATEGY.md](TEST_STRATEGY.md) | Test categories, tags, known failures, and baseline |
| [KNOWN_AUT_LIMITATIONS.md](KNOWN_AUT_LIMITATIONS.md) | Why 6 scenarios consistently fail |
| [QUALITY_RISK_ASSESSMENT.md](QUALITY_RISK_ASSESSMENT.md) | Risk register, including QR-009 (CI classification) |
| [SELENIUM_GRID_GUIDE.md](SELENIUM_GRID_GUIDE.md) | Grid execution (not used in CI) |

---

## 1. Pipeline Overview

The CI pipeline runs on every push and pull request to `main` or `develop`.

```
Checkout
  │
  ├─ Java 17 + Gradle cache
  ├─ Gradle wrapper validation
  ├─ Chrome (stable) + Xvfb
  │
  ├─ [COMPILE GATE]       ./gradlew compileTestJava         — fail fast
  ├─ [SAFETY GATE]        ./gradlew test *ProductionSafetyGuardTest — fail fast
  │
  ├─ [FULL REGRESSION]    xvfb-run ./gradlew clean test -Denv=qa
  │     exit code captured — step always exits 0
  │
  ├─ Artifact uploads     (always)
  │
  └─ [CLASSIFY + SUMMARY] scripts/analyze-test-results.sh  — final gate
        Parses JUnit XML → VALIDATED_BASELINE / UNEXPECTED_REGRESSION /
        INFRASTRUCTURE_FAILURE / RESULTS_UNAVAILABLE
        Exits 0 only for VALIDATED_BASELINE
```

---

## 2. Triggers

| Trigger | Condition |
|---|---|
| `push` | `main` or `develop` branch |
| `pull_request` | Targeting `main` or `develop` |
| `workflow_dispatch` | Manual via **Actions → Run workflow** |

Manual execution allows selecting `qa`, `staging`, or `uat`. The `prod` option is intentionally absent — automated execution against production is not supported.

---

## 3. Pipeline Stages

### Stage 1 — Compile

```bash
./gradlew compileTestJava
```

Surfaces compilation errors immediately. If this fails, no tests run and the job fails.

### Stage 2 — Production-safety tests

```bash
./gradlew test --tests "*ProductionSafetyGuardTest"
```

Runs 12 focused tests that verify `ConfigManager` production detection and the write guard. These tests have no browser dependency and complete in under 1 second. If any of the 12 fail, the job fails before the Cucumber suite starts.

### Stage 3 — Full Cucumber regression

```bash
xvfb-run --auto-servernum --server-args="-screen 0 1920x1080x24" \
  ./gradlew clean test \
  -Denv=$TEST_ENV -Dbrowser=chrome \
  -DmaxParallelForks=1 -Ddataproviderthreadcount=1
```

Runs all 18 Cucumber scenario executions. The Gradle exit code is captured in a step output (`steps.regression.outputs.gradle_exit`) and the step itself always exits 0 — the classify step determines the job outcome.

### Stage 4 — Classification (final gate)

```bash
scripts/analyze-test-results.sh
```

Parses `build/test-results/test/TEST-*.xml`. Exits 0 only when the result exactly matches the accepted baseline. See [Section 4](#4-classification-results) for details.

---

## 4. Classification Results

The classifier compares JUnit XML results against the accepted baseline:

| Classification | Meaning | Job outcome |
|---|---|---|
| `VALIDATED_BASELINE` | Exactly `{runScenario[9,10,11,13,14,15]}` failed — matches known AUT failures | **Green** |
| `UNEXPECTED_REGRESSION` | Different or additional failures; or safety tests failed | **Red** |
| `INFRASTRUCTURE_FAILURE` | Tests started but Cucumber suite produced 0 executions | **Red** |
| `RESULTS_UNAVAILABLE` | No JUnit XML files found (compile failed, or test task crashed) | **Red** |

### Accepted baseline

| Metric | Expected value |
|---|---|
| Safety tests | 12/12 passed |
| Cucumber executions | 18 |
| Cucumber passed | 12 |
| Known AUT failures | 6 — `runScenario[9,10,11,13,14,15]` |
| Unexpected failures | 0 |

See [docs/KNOWN_AUT_LIMITATIONS.md](KNOWN_AUT_LIMITATIONS.md) for why `runScenario[9,10,11]` (AUT-LIM-001) and `runScenario[13,14,15]` (AUT-LIM-002) consistently fail.

---

## 5. Interpreting CI Results

### Job is green (VALIDATED_BASELINE)

The 6 known AUT failures were detected and no unexpected failures exist. The framework is working correctly. This is the expected steady-state outcome.

### Job is red — UNEXPECTED_REGRESSION

One or more test failures exist outside the known-failure set. Actions to take:

1. Open the **Classify results** step in the Actions UI — the job summary and stdout show which `runScenario[N]` indices are unexpected.
2. Download artifacts: `extent-report`, `screenshots`, `test-logs`.
3. Re-run once to rule out transient AUT instability (see [QR-001](QUALITY_RISK_ASSESSMENT.md)).
4. If it reproduces, investigate as a framework regression. Do not merge until resolved.

### Job is red — INFRASTRUCTURE_FAILURE

The Cucumber suite produced zero executions. This indicates a setup issue rather than a test failure:
- Xvfb failed to start
- Chrome failed to launch
- JVM crash before any scenario ran

Check the `test-logs` artifact and the "Full regression" step stdout for the error.

### Job is red — RESULTS_UNAVAILABLE

No JUnit XML files were produced. This typically means:
- The compile stage failed (check the "Compile" step)
- The Gradle test task crashed before producing output
- The classifier ran before the regression step completed

Check the step-by-step log in the Actions UI.

---

## 6. Running Stages Locally

The same stages that CI executes can be run locally in the same order:

```bash
# Stage 1 — compile
./gradlew compileTestJava

# Stage 2 — safety gate
./gradlew test --tests "*ProductionSafetyGuardTest"

# Stage 3 — full regression
./gradlew clean test -Denv=qa -Dbrowser=chrome -DmaxParallelForks=1 -Ddataproviderthreadcount=1

# Stage 4 — classify (on Linux/macOS/WSL — requires python3)
GRADLE_EXIT_CODE=$? scripts/analyze-test-results.sh
```

On Windows (PowerShell), run the classifier from WSL or Git Bash since it is a bash script backed by Python 3. The Python logic can also be inspected directly in `scripts/analyze-test-results.sh`.

---

## 7. Artifacts

Each run produces named artifacts in the **Artifacts** panel (retained 14 days):

| Artifact | Contents |
|---|---|
| `extent-report-<env>-run<N>` | Interactive HTML ExtentReports |
| `allure-results-<env>-run<N>` | Raw Allure results |
| `testng-reports-<env>-run<N>` | TestNG / Cucumber HTML |
| `screenshots-<env>-run<N>` | Failure PNGs |
| `test-logs-<env>-run<N>` | Rolling Logback output |

Artifacts are uploaded regardless of job outcome (`if: always()`).

---

## 8. Credentials in CI

Add repository secrets under **Settings → Secrets and variables → Actions**:

- `TEST_USERNAME` — overrides `username` in `qa.properties`
- `TEST_PASSWORD` — overrides `password` in `qa.properties`

If secrets are not configured, the pipeline falls back to `qa.properties` values (public ParaBank demo credentials `sqa/sqa`).

Secrets are never printed in any log statement. See [docs/TEST_STRATEGY.md](TEST_STRATEGY.md) for the credential priority chain.

---

## 9. Concurrency

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}
```

Duplicate runs on the same PR branch or feature branch are cancelled when a newer push arrives. Runs on `main` are never cancelled — each main-branch run is the authoritative evidence record.

---

## 10. Known Limitations

| Limitation | Detail |
|---|---|
| Chrome only | CI runs Chrome via `browser-actions/setup-chrome@v1` and Xvfb — no Firefox or Edge |
| No Selenium Grid in CI | Grid execution requires Docker; GitHub-hosted runners do not run the Grid stack |
| AUT dependency | All Cucumber scenarios contact the public ParaBank demo server — external instability can cause transient `UNEXPECTED_REGRESSION` classifications |

For transient AUT instability, see [QR-001 in QUALITY_RISK_ASSESSMENT.md](QUALITY_RISK_ASSESSMENT.md).
