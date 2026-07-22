# CI/CD Guide

**Framework:** ParaBank BDD Automation  
**Workflow file:** `.github/workflows/automation-test.yml`  
**Classifier:** `scripts/analyze-test-results.sh`  
**Last Updated:** 2026-07-18

---

## Related Documents

| Document | Purpose |
|---|---|
| [TEST_STRATEGY.md](../quality/TEST_STRATEGY.md) | Test categories, tags, known failures, and baseline |
| [KNOWN_AUT_LIMITATIONS.md](../quality/KNOWN_AUT_LIMITATIONS.md) | Why 6 scenarios consistently fail |
| [QUALITY_RISK_ASSESSMENT.md](../quality/QUALITY_RISK_ASSESSMENT.md) | Risk register, including QR-009 (CI classification) |
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

The classifier uses two artifacts together, each responsible for a different part of the result:

- **JUnit XML** (`build/test-results/test/TEST-*.xml`) is the source of truth for **execution counts** — total/passed/failed Cucumber executions, and the ProductionSafetyGuardTest pass/fail count.
- **Cucumber JSON** (`build/reports/cucumber/cucumber-report.json`) is the source of truth for **failed-scenario identity** — *which* scenarios failed.

### Why not JUnit XML alone?

Earlier versions of this classifier matched failed scenarios by their TestNG-generated `runScenario[N]` name. That index is assigned by **data-provider invocation order**, not by scenario content — it is not a stable identity:

- Under `dataproviderthreadcount` > 1 (Gradle's own default is 2), the same index can be reused for two different scenarios, and indices can be assigned out of declaration order. This was directly reproduced and inspected in this repository's own local build output.
- CI happens to avoid the symptom only because it forces `dataproviderthreadcount=1`/`maxParallelForks=1` — a setting documented as being for runner CPU contention (see [TEST_STRATEGY.md](../quality/TEST_STRATEGY.md)), not for classifier stability. Nothing previously guaranteed that property.

The classifier now identifies failed scenarios using the Cucumber JSON **element-level `id`** field (e.g. `customer-sign-in;sign-in-is-protected-against-injection-and-scripting-attacks;;2`). This id is derived from the Gherkin document itself (feature name + scenario name + outline row ordinal) and does not depend on TestNG thread count, execution order, or invocation index. The feature file `uri` is also read from the JSON and shown in diagnostics, but it is never used for equality matching — one `uri` covers every scenario in a feature file and cannot identify a single scenario on its own.

The two artifacts are cross-checked: if the Cucumber JSON element counts (total/passed/failed) disagree with the JUnit XML counts, or the JSON is missing/malformed/incomplete, the classifier treats the result as untrustworthy (`RESULTS_UNAVAILABLE`) rather than silently proceeding or falling back to index matching.

| Classification | Meaning | Job outcome |
|---|---|---|
| `VALIDATED_BASELINE` | Exactly the 6 known-failure IDs failed (see below) — matches known AUT failures | **Green** |
| `UNEXPECTED_REGRESSION` | Different or additional failure IDs; count mismatch; or safety tests failed | **Red** |
| `INFRASTRUCTURE_FAILURE` | Tests started but Cucumber suite produced 0 executions | **Red** |
| `RESULTS_UNAVAILABLE` | No JUnit XML found; or the Cucumber JSON report is missing/malformed/has no scenario elements/lacks required `id` fields; or JSON and XML counts disagree | **Red** |

### Accepted baseline

| Metric | Expected value |
|---|---|
| Safety tests | 12/12 passed |
| Cucumber executions | 18 |
| Cucumber passed | 12 |
| Known AUT failures | 6 (by Cucumber JSON element `id` — see table below) |
| Unexpected failures | 0 |

### Canonical known-failure IDs

| ID | Scenario | Feature URI |
|---|---|---|
| `customer-sign-in;sign-in-is-protected-against-injection-and-scripting-attacks;;2` | Sign-in is protected against injection and scripting attacks (row 1) | `file:src/test/resources/features/login.feature` |
| `customer-sign-in;sign-in-is-protected-against-injection-and-scripting-attacks;;3` | Sign-in is protected against injection and scripting attacks (row 2) | `file:src/test/resources/features/login.feature` |
| `customer-sign-in;sign-in-is-protected-against-injection-and-scripting-attacks;;4` | Sign-in is protected against injection and scripting attacks (row 3) | `file:src/test/resources/features/login.feature` |
| `customer-account-registration;a-new-customer-can-open-a-bank-account-with-their-personal-information` | A new customer can open a bank account with their personal information | `file:src/test/resources/features/register.feature` |
| `customer-account-registration;a-new-customer-can-open-a-bank-account-with-a-freshly-generated-profile` | A new customer can open a bank account with a freshly generated profile | `file:src/test/resources/features/register.feature` |
| `customer-account-registration;a-customer-can-open-a-bank-account-using-details-provided-by-an-external-source` | A customer can open a bank account using details provided by an external source | `file:src/test/resources/features/register.feature` |

See [docs/quality/KNOWN_AUT_LIMITATIONS.md](../quality/KNOWN_AUT_LIMITATIONS.md) for why the injection scenario rows (AUT-LIM-001) and the three positive registration scenarios (AUT-LIM-002) consistently fail.

### Baseline revalidation guidance

Update `KNOWN_FAILURE_IDS` in `scripts/analyze-test-results.sh` only when a deliberate, reviewed change is made to one of the six tracked scenarios:

- The scenario's own name text is edited, or the feature's name is edited (changes the `id` slug).
- Rows within the `Sign-in is protected against injection and scripting attacks` Scenario Outline are reordered (changes the `;;N` ordinal for that scenario's rows only).
- The accepted AUT-limitation baseline itself changes (a scenario starts/stops being an accepted known failure).

Unrelated edits elsewhere in a feature file (new scenarios, comments, reordering other scenarios) do **not** affect these IDs and must not require a baseline update — if they do, investigate before accepting.

---

## 5. Interpreting CI Results

### Job is green (VALIDATED_BASELINE)

The 6 known AUT failures were detected and no unexpected failures exist. The framework is working correctly. This is the expected steady-state outcome.

### Job is red — UNEXPECTED_REGRESSION

One or more test failures exist outside the known-failure set. Actions to take:

1. Open the **Classify results** step in the Actions UI — the job summary and stdout show the expected/actual/unexpected/missing Cucumber JSON `id` values, plus the scenario name and feature `uri` for each.
2. Download artifacts: `extent-report`, `screenshots`, `test-logs`, `cucumber-reports`.
3. Re-run once to rule out transient AUT instability (see [QR-001](../quality/QUALITY_RISK_ASSESSMENT.md)).
4. If it reproduces, investigate as a framework regression. Do not merge until resolved.

### Job is red — INFRASTRUCTURE_FAILURE

The Cucumber suite produced zero executions. This indicates a setup issue rather than a test failure:
- Xvfb failed to start
- Chrome failed to launch
- JVM crash before any scenario ran

Check the `test-logs` artifact and the "Full regression" step stdout for the error.

### Job is red — RESULTS_UNAVAILABLE

Either the JUnit XML or the Cucumber JSON report could not be trusted for classification. This typically means:
- No JUnit XML files were produced — the compile stage failed, the Gradle test task crashed before producing output, or the classifier ran before the regression step completed
- `build/reports/cucumber/cucumber-report.json` is missing, malformed, has no executable scenario elements, or a scenario element is missing its `id` field
- The Cucumber JSON element counts (total/passed/failed) disagree with the JUnit XML counts — the classifier will not guess which artifact is correct

Check the step-by-step log in the Actions UI and the `cucumber-reports` artifact.

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
| `cucumber-reports-<env>-run<N>` | Cucumber HTML + JSON (`cucumber-report.json` — classifier's identity source of truth) |
| `screenshots-<env>-run<N>` | Failure PNGs |
| `test-logs-<env>-run<N>` | Rolling Logback output |

Artifacts are uploaded regardless of job outcome (`if: always()`).

---

## 8. Credentials in CI

Add repository secrets under **Settings → Secrets and variables → Actions**:

- `TEST_USERNAME` — overrides `username` in `qa.properties`
- `TEST_PASSWORD` — overrides `password` in `qa.properties`

If secrets are not configured, the pipeline falls back to `qa.properties` values (public ParaBank demo credentials `sqa/sqa`).

Secrets are never printed in any log statement. See [docs/quality/TEST_STRATEGY.md](../quality/TEST_STRATEGY.md) for the credential priority chain.

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

For transient AUT instability, see [QR-001 in QUALITY_RISK_ASSESSMENT.md](../quality/QUALITY_RISK_ASSESSMENT.md).
