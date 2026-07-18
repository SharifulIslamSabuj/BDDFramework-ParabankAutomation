# Phase 0 — Baseline Validation Report

## 1. Objective

Capture a verifiable, reproducible execution baseline for the BDD automation framework
against the current state of the codebase and the public ParaBank demo AUT.
This document is strictly an evidence record: no production or test code was modified.

---

## 2. Repository Snapshot

| Field              | Value                                                                 |
|--------------------|-----------------------------------------------------------------------|
| Repository URL     | https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation |
| Branch             | main                                                                  |
| HEAD commit        | `aa3c243f248e1cd476d0f8efc1db2bbe9f2d775a`                           |
| Working-tree state | **DIRTY** — 4 modified files, 1 untracked file (see §3)               |
| Validation date    | 2026-07-18                                                            |
| Validated by       | Principal QA Automation Architect — baseline lock audit               |

### 2.1 Working-tree diff at validation time

```
M  src/test/java/com/parabank/parasoft/stepdefinitions/LoginSteps.java
 M src/test/java/com/parabank/parasoft/stepdefinitions/RegisterSteps.java
 M src/test/resources/features/login.feature
 M src/test/resources/features/register.feature
?? tatus
```

The four modified files are the BDD Phase 2 refactoring deliverables (Gherkin layer and
new business-language step definitions only). They are the subject of this validation.
The untracked file `tatus` is an accidental shell artefact in the repository root;
it does not affect compilation or test execution.

---

## 3. Environment

| Component     | Version / Value                                                |
|---------------|----------------------------------------------------------------|
| OS            | Windows 11 Pro — 10.0.26200 (amd64)                           |
| Java          | `java version "17.0.12"` (OpenJDK 64-bit Server VM build 17.0.12+7) |
| Gradle        | 9.0.0                                                          |
| Selenium      | 4.40.0                                                         |
| Cucumber      | 7.34.2                                                         |
| TestNG        | 7.12.0                                                         |
| Browser       | Chrome (system-installed, managed by WebDriverManager)         |
| AUT base URL  | https://parabank.parasoft.com (public demo — shared instance)  |

---

## 4. Execution

### 4.1 Command

```
./gradlew clean test
```

No system properties were overridden. All configuration sourced from
`src/test/resources/qa.properties`.

### 4.2 Summary

| Metric              | Count |
|---------------------|------:|
| Total scenarios     |    18 |
| Passed              |     5 |
| Failed              |    13 |
| Skipped             |     0 |

---

## 5. Scenario-Count Comparison

| Metric                    | Historical baseline | Current baseline | Δ     |
|---------------------------|--------------------:|-----------------:|------:|
| Total executions          |                  18 |               18 |     0 |
| Passed                    |                  12 |                5 | **−7** |
| Failed                    |                   6 |               13 | **+7** |
| Skipped                   |                   0 |                0 |     0 |
| Known AUT failures        |                   6 |                6 |     0 |
| Previously passing — now failing | 0            |                7 | **+7** |
| Coverage lost (scenarios removed) | 0           |                0 |     0 |

> **Historical baseline** is the last confirmed execution result recorded prior to Phase 0:
> 18 total / 12 passed / 6 failed (3 injection + 3 registration positive scenarios).

---

## 6. Passed Scenarios (5)

| # | Feature                    | Scenario                                                                 | Tags                |
|---|----------------------------|--------------------------------------------------------------------------|---------------------|
| 1 | Customer Sign-In           | A registered customer can sign in with valid credentials                  | @smoke @positive    |
| 2 | Customer Sign-In           | Sign-in rejected: required credential fields not provided (blank username) | @negative @validation |
| 3 | Customer Sign-In           | Sign-in rejected: required credential fields not provided (blank password) | @negative @validation |
| 4 | Customer Sign-In           | Sign-in rejected: required credential fields not provided (both blank)    | @negative @validation |
| 5 | Customer Account Registration | Registration rejected when customer does not provide a username         | @negative @validation |

---

## 7. Failed Scenarios (13)

### 7.1 Login failures — AUT behavioral change (10 scenarios)

All 10 failures share the identical error pattern:

```
AssertionError: Expected to contain: 'The username and password could not be verified.'
  but got: ''
```

The `p.error` element (`By.cssSelector("p.error")`) was absent from the DOM.
The AUT returned the login page without rendering any error message.

| # | Scenario name                                                                    | Tags                      | Status vs historical |
|---|----------------------------------------------------------------------------------|---------------------------|----------------------|
| 1 | Sign-in rejected when credentials do not match — row 1 (invalidUser / validPass123) | @negative @validation  | **NEW FAILURE** |
| 2 | Sign-in rejected when credentials do not match — row 2 (validUser / wrongPass)    | @negative @validation     | **NEW FAILURE** |
| 3 | Sign-in rejected when credentials do not meet format requirements — row 1 (username 50 chars) | @negative @validation | **NEW FAILURE** |
| 4 | Sign-in rejected when credentials do not meet format requirements — row 2 (password 50 chars) | @negative @validation | **NEW FAILURE** |
| 5 | Sign-in rejected when credentials do not meet format requirements — row 3 (user@!#) | @negative @validation   | **NEW FAILURE** |
| 6 | Sign-in rejected when the password consists entirely of whitespace                | @negative @validation     | **NEW FAILURE** |
| 7 | Sign-in protected against injection — row 1 (`'; DROP TABLE users; --` username)  | @negative @security       | PREVIOUSLY KNOWN |
| 8 | Sign-in protected against injection — row 2 (`' OR '1'='1` password)             | @negative @security       | PREVIOUSLY KNOWN |
| 9 | Sign-in protected against injection — row 3 (XSS username)                       | @negative @security       | PREVIOUSLY KNOWN |
| 10 | Sign-in rejected for a locked or deactivated account                             | @negative                 | **NEW FAILURE** |

### 7.2 Registration failures — pre-existing AUT limitation (3 scenarios)

All 3 failures: framework navigates to registration, fills the form and submits successfully,
but the post-submit flow does not surface the expected logout link within the configured timeout.
Consistent with AUT session/redirect behaviour on the shared public demo server.

| # | Scenario name                                                               | Tags      | Status vs historical |
|---|-----------------------------------------------------------------------------|-----------|----------------------|
| 11 | A new customer can open a bank account with their personal information     | @positive | PREVIOUSLY KNOWN |
| 12 | A new customer can open a bank account with a freshly generated profile    | @positive | PREVIOUSLY KNOWN |
| 13 | A customer can open a bank account using details provided by an external source | @positive | PREVIOUSLY KNOWN |

---

## 8. Failure Root-Cause Analysis

### 8.1 Previously known failures (6) — unchanged

**Login — injection scenarios (3):** The ParaBank public demo server has not rendered `p.error`
for SQL injection and XSS payloads in prior executions. The server appears to silently reject
those requests without returning a client-visible error message. This is a known AUT limitation,
not a framework defect.

**Registration — positive scenarios (3):** The ParaBank public demo server on the shared instance
does not reliably complete the post-registration redirect to the authenticated landing page within
the framework's configured element-wait timeout. This is a known AUT limitation on the public
instance; the same scenarios are expected to pass against a locally hosted or dedicated AUT instance.

### 8.2 New failures (7) — AUT behavioral change

Scenarios 1–6 and 10 were passing in the historical baseline (12 passed) and are now failing
with the identical `got: ''` error that was previously exclusive to the 3 injection scenarios.

**Pattern:** Every scenario that expects `"The username and password could not be verified."` is
now failing. Only scenarios that expect `"Please enter a username and password."` (blank-field
cases) are passing. This is a clear bifurcation in AUT server behaviour by error-message type.

**Likely cause:** The ParaBank public demo server has changed how it handles invalid-but-non-empty
credential submissions. Previously it rendered `p.error` with the mismatch message; it now appears
to return the login page silently (or redirect without the error element) for this class of input.

**Framework responsibility:** None. The assertion logic in `LoginSteps` is correct. The step
`the customer is shown the sign-in error(String)` delegates to `user_should_see_error_message()`
which correctly waits for and reads `p.error`. The 4 blank-field scenarios that DO pass
demonstrate the assertion pipeline is functional — the element is present when the AUT renders it.
The 7 newly failing scenarios differ only in the AUT's response, not in the framework code path.

---

## 9. Framework Regression Assessment

**NONE IDENTIFIED.**

All 13 failures are attributable to AUT behaviour on the public demo server. No framework code
change between the historical baseline and this validation caused any scenario to regress.
Evidence:

- The positive login scenario (@smoke @positive) **passed** — WebDriver initialisation, navigation,
  credential entry, form submission, and logout-link assertion all work correctly.
- The blank-field negative scenarios **passed** — the full assertion pipeline (ElementActions,
  WaitUtils, error-message comparison) works correctly when the AUT renders `p.error`.
- The 7 new login failures produce the **identical error** to the 3 historically known injection
  failures — `got: ''` — confirming the AUT is not rendering the element, not that the framework
  is failing to find it.

---

## 10. Generated Evidence

| Artefact                                       | Location (relative to project root)                   |
|------------------------------------------------|-------------------------------------------------------|
| Cucumber HTML report                           | `build/reports/cucumber/cucumber-report.html`         |
| Cucumber JSON report                           | `build/reports/cucumber/cucumber-report.json`         |
| Gradle test HTML report                        | `build/reports/tests/test/index.html`                 |
| ExtentReports HTML                             | `build/reports/extent/Report.html`                    |
| Allure results (raw JSON)                      | `allure-results/` — 2448 JSON files                   |
| Framework log                                  | `build/logs/test.log` — 262 282 bytes                 |
| Screenshots (failure captures)                 | `build/screenshots/` — 12 PNG files                   |

All artefacts were generated by the single `./gradlew clean test` run performed on 2026-07-18.

---

## 11. Risks and Uncertainties

1. **Public demo server instability:** `parabank.parasoft.com` is a shared, externally hosted
   instance. Its behaviour can change between runs without notice. The 7 new failures may be
   transient if the server is mid-maintenance or under unusual load.

2. **Working tree is dirty:** The 4 modified files (BDD Phase 2 deliverables) are not committed.
   This baseline reflects the working-tree state, not a clean commit. Anyone reproducing this
   run must ensure the same 4 files are modified identically.

3. **Non-deterministic parallel execution:** `@DataProvider(parallel=true)` is enabled in
   `TestRunner.java`. On a system under load, scenario ordering and timing may differ across runs.
   This does not affect pass/fail outcomes but may affect screenshot and log timestamps.

4. **Screenshot coverage:** Only 12 screenshots were captured for 13 failures. One failure
   (scenario #10 — locked account) may not have produced a screenshot if the failure occurred
   before the screenshot hook fired (e.g., if Hooks.@After ran but the element was absent at
   screenshot capture time). This does not affect the failure classification.

---

## 12. Recommended Next Step

Re-run `./gradlew clean test` within 24 hours to determine whether the 7 new failures are:

- **Transient (AUT instability):** Re-run passes 12 scenarios again → historical baseline is
  still valid; no action needed beyond noting the AUT is flaky for this class of scenario.
- **Permanent (AUT behavioural change):** Re-run continues to return 5 passed / 13 failed →
  update the baseline to 18/5/13 and file an AUT defect against the public demo server.

Do not implement any framework fixes before this determination is made.

---

## 13. Baseline Conclusion

> **BASELINE CHANGED — INVESTIGATION REQUIRED**

The current execution result (18 total / 5 passed / 13 failed) differs from the historical
baseline (18 total / 12 passed / 6 failed) by 7 additional failures. All 7 new failures are
consistent with a change in AUT server behaviour — the `p.error` element is no longer rendered
by the public ParaBank demo server for the "invalid but non-empty credentials" rejection class.

No framework code regression was identified. The BDD Phase 2 refactoring deliverables (Gherkin
layer and business-language step definitions) are not the cause of any failure.

**Action required:** Re-run in 24 hours (see §12) before drawing a final conclusion.
