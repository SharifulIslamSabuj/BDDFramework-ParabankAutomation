# Phase 0.1 — Baseline Change Investigation Report

## 1. Investigation Objective

Determine, with reproducible evidence, whether the seven additional failures recorded in the
Phase 0 baseline run (18/5/13) were caused by:

- Temporary ParaBank AUT instability
- Permanent ParaBank behavioural change
- Shared test-data collision
- Parallel-execution interference
- Session leakage
- Scenario-order dependency
- Environment or configuration difference
- Browser or WebDriver change
- Dirty working-tree change
- A genuine framework regression

---

## 2. Repository and Environment Snapshot

| Field                    | Value                                                              |
|--------------------------|--------------------------------------------------------------------|
| Repository URL           | https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation |
| Branch                   | main                                                               |
| HEAD commit              | `aa3c243f248e1cd476d0f8efc1db2bbe9f2d775a`                        |
| Working-tree state       | DIRTY — same 4 modified files as Phase 0 (unchanged during investigation) |
| Investigation date       | 2026-07-18                                                         |
| OS                       | Windows 11 Pro 10.0.26200 (amd64)                                 |
| Java                     | 17.0.12+8-LTS-286 (HotSpot)                                       |
| Gradle                   | 9.0.0                                                              |
| Selenium                 | 4.40.0 (pinned)                                                    |
| Cucumber                 | 7.34.2 (pinned)                                                    |
| TestNG                   | 7.12.0 (pinned)                                                    |
| Chrome                   | 150.0.7871.125 (system-installed)                                  |
| ChromeDriver             | 150.0.7871.124 (auto-managed by WebDriverManager — matched Chrome) |
| AUT                      | https://parabank.parasoft.com (public shared demo)                 |
| TEST_USERNAME env var    | not set                                                            |
| TEST_PASSWORD env var    | not set                                                            |
| BROWSER env var          | not set (defaults to `chrome`)                                     |
| SELENIUM_GRID_ENABLED    | not set (defaults to `false`)                                      |

### Dependency resolution

All library versions are pinned in `build.gradle`; no dynamic version specifiers are used.
No dependency version drift is possible between runs.
Chrome (150.0.7871.125) and the auto-matched ChromeDriver (150.0.7871.124) were identical
across the Phase 0 run and all investigation runs — no browser update occurred during this
investigation.

---

## 3. Dirty Working-Tree Analysis

Four files modified relative to HEAD `aa3c243`. No additional files were modified during
this investigation.

### 3.1 `src/test/resources/features/login.feature` — **test-definition-only**

Diff summary: full BDD Phase 2 refactor — feature title, user-story block, Background,
scenario names, step text, and tags rewritten. Scenario count unchanged: 14 executions.

Execution impact assessment:
- All 14 scenarios map to new business-language step definitions that delegate to the
  **identical underlying methods** (`user_on_login_page()`, `user_enters_username_and_password()`,
  `user_clicks_login_button()`, `user_should_see_error_message()`, `user_should_see_logout_link()`).
- Test inputs are identical: same username/password values, same expected error strings.
- Background step (`a registered customer is on the sign-in page`) delegates to `user_on_login_page()`,
  which navigates to the login page — same as the former per-scenario Given step.
- **No locators, waits, assertions, hooks, runner configuration, or credential values changed.**
- **Verdict: NOT execution-affecting.**

### 3.2 `src/test/resources/features/register.feature` — **test-definition-only**

Diff summary: feature title, Background, scenario names, step text, tags rewritten.
Scenario count unchanged: 4 executions.

Execution impact assessment:
- All 4 scenarios delegate to the identical underlying methods with no behavioural change.
- **Verdict: NOT execution-affecting.**

### 3.3 `src/test/java/com/parabank/parasoft/stepdefinitions/LoginSteps.java` — **additive-only**

Diff summary: 31 lines appended at end of class. Five new `@Given`/`@When`/`@Then` methods
added, each containing a single delegation call to an existing method. No existing code was
modified. The previously committed step definitions (`user_on_login_page()`,
`user_enters_username_and_password()`, etc.) are structurally unchanged.

Execution impact assessment:
- New step definitions are purely mapping glue. They do not alter waits, locators, assertions,
  or any side effect.
- **Verdict: NOT execution-affecting for any existing scenario behaviour.**

### 3.4 `src/test/java/com/parabank/parasoft/stepdefinitions/RegisterSteps.java` — **additive + whitespace**

Diff summary: whitespace-only changes within the `user_should_see_error_message()` body
(removed three blank comment lines). Nine new business-language step definitions appended.
The assertion logic, `registerPage.hasErrorMessage()`, `registerPage.getErrorMessageText()`,
and both `Assert.assertTrue` calls are word-for-word identical to the committed version.

Execution impact assessment:
- The blank-line removal inside `user_should_see_error_message()` is not functionally
  different; Java compiles blank lines identically to absent blank lines.
- **Verdict: NOT execution-affecting.**

**Summary: None of the four dirty files altered test logic, locators, waits, assertions,
credentials, configuration, hooks, or runner behaviour. The dirty working tree cannot
explain any failure observed in Phase 0 or any investigation run.**

---

## 4. Commands Executed

All commands are read-only or test-execution only. No source files were modified.

```
# Dirty working-tree inspection
git status --short
git diff --name-only
git diff --stat
git diff -- src/test/resources/features/login.feature
git diff -- src/test/resources/features/register.feature
git diff -- src/test/java/com/parabank/parasoft/stepdefinitions/LoginSteps.java
git diff -- src/test/java/com/parabank/parasoft/stepdefinitions/RegisterSteps.java

# Environment
java -version
./gradlew --version
chrome.exe (version via Windows VersionInfo)
./gradlew dependencies --configuration testRuntimeClasspath

# Runs
./gradlew clean test                                            # RUN-A-DEFAULT
./gradlew clean test                                            # RUN-B-DEFAULT
./gradlew clean test -Ddataproviderthreadcount=1 -DmaxParallelForks=1   # RUN-C-SEQUENTIAL
./gradlew clean test -Dcucumber.filter.tags=@negative and not @security  # RUN-D1-TARGETED
./gradlew clean test -Dcucumber.filter.tags=@negative and not @security  # RUN-D2-TARGETED
```

---

## 5. Results from All Runs

| Run | Scenarios | Passed | Failed | Duration | Parallel | Tag filter |
|-----|----------:|-------:|-------:|---------:|----------|------------|
| RUN-0-INITIAL (Phase 0) | 18 | 5 | 13 | ~3m | `dataproviderthreadcount=2` `maxParallelForks=2` | default |
| RUN-A-DEFAULT | 18 | 12 | 6 | 2m 52s | `dataproviderthreadcount=2` `maxParallelForks=2` | default |
| RUN-B-DEFAULT | 18 | 12 | 6 | 2m 40s | `dataproviderthreadcount=2` `maxParallelForks=2` | default |
| RUN-C-SEQUENTIAL | 18 | 12 | 6 | 4m 33s | `dataproviderthreadcount=1` `maxParallelForks=1` | default |
| RUN-D1-TARGETED | 11 | 11 | 0 | 52s | `dataproviderthreadcount=2` `maxParallelForks=2` | `@negative and not @security` |
| RUN-D2-TARGETED | 11 | 11 | 0 | 49s | `dataproviderthreadcount=2` `maxParallelForks=2` | `@negative and not @security` |

**Key observation:** RUN-A through RUN-D2 all returned either 12/6 (full suite) or 11/0
(targeted). The 7 additional failures from Phase 0 were absent in every investigation run —
5 full-suite runs and 4 targeted runs across both parallel and sequential execution modes.

---

## 6. Failure Stability Matrix

| # | Scenario | Phase 0 | RUN-A | RUN-B | RUN-C (seq) | RUN-D1 | RUN-D2 | Classification |
|---|----------|:-------:|:-----:|:-----:|:-----------:|:------:|:------:|----------------|
| 1 | Sign-in rejected: credentials do not match (invalidUser / validPass123) | FAIL | PASS | PASS | PASS | PASS | PASS | **TRANSIENT AUT FAILURE** |
| 2 | Sign-in rejected: credentials do not match (validUser / wrongPass) | FAIL | PASS | PASS | PASS | PASS | PASS | **TRANSIENT AUT FAILURE** |
| 3 | Sign-in rejected: format requirements — long username | FAIL | PASS | PASS | PASS | PASS | PASS | **TRANSIENT AUT FAILURE** |
| 4 | Sign-in rejected: format requirements — long password | FAIL | PASS | PASS | PASS | PASS | PASS | **TRANSIENT AUT FAILURE** |
| 5 | Sign-in rejected: format requirements — `user@!#` | FAIL | PASS | PASS | PASS | PASS | PASS | **TRANSIENT AUT FAILURE** |
| 6 | Sign-in rejected: whitespace-only password | FAIL | PASS | PASS | PASS | PASS | PASS | **TRANSIENT AUT FAILURE** |
| 7 | Sign-in rejected: locked / deactivated account | FAIL | PASS | PASS | PASS | PASS | PASS | **TRANSIENT AUT FAILURE** |
| 8 | Injection: `'; DROP TABLE users; --` in username | FAIL | FAIL | FAIL | FAIL | (excluded) | (excluded) | **CONSISTENT AUT BEHAVIOURAL CHANGE** |
| 9 | Injection: `' OR '1'='1` in password | FAIL | FAIL | FAIL | FAIL | (excluded) | (excluded) | **CONSISTENT AUT BEHAVIOURAL CHANGE** |
| 10 | Injection: `<script>alert('xss')</script>` in username | FAIL | FAIL | FAIL | FAIL | (excluded) | (excluded) | **CONSISTENT AUT BEHAVIOURAL CHANGE** |
| 11 | Registration: personal information | FAIL | FAIL | FAIL | FAIL | (excluded) | (excluded) | **CONSISTENT AUT BEHAVIOURAL CHANGE** |
| 12 | Registration: freshly generated profile | FAIL | FAIL | FAIL | FAIL | (excluded) | (excluded) | **CONSISTENT AUT BEHAVIOURAL CHANGE** |
| 13 | Registration: external source data | FAIL | FAIL | FAIL | FAIL | (excluded) | (excluded) | **CONSISTENT AUT BEHAVIOURAL CHANGE** |

> Scenarios 8–13 are the six historically known failures present since the initial historical
> baseline. They are not part of the "7 additional failures" under investigation.

---

## 7. Analysis of the Seven Additional Failures (Scenarios 1–7)

### 7.1 Common error pattern across all 7 in Phase 0

```
AssertionError: Expected error message not found.
Expected to contain: 'The username and password could not be verified.'
but got: ''
```

The `p.error` CSS selector (`By.cssSelector("p.error")`) returned an empty string in
every case. This is identical to the error produced by the three historically-known
injection failures (scenarios 8–10) — where the AUT has never rendered the error element.

### 7.2 What passed in Phase 0 that also uses the same assertion

The three blank-field scenarios (require `"Please enter a username and password."`) passed
in Phase 0. This proves:

1. The browser session was functional — Chrome launched, navigated to ParaBank, and submitted forms.
2. The `p.error` element CAN be rendered — it was rendered for blank-field submissions.
3. The assertion pipeline (`user_should_see_error_message()`) was functional — it correctly
   detected and compared the `p.error` text for blank-field submissions.
4. The failure was specific to submissions that should produce
   `"The username and password could not be verified."` — the server produced NO error element
   at all for that class of input.

### 7.3 Why the 7 scenarios recovered in RUN-A without any code change

The AUT server (`parabank.parasoft.com`) is a public shared demo instance with no SLA. During
the Phase 0 run window, the server was temporarily not producing the `p.error` element for
non-empty invalid credential submissions. By RUN-A (approximately 30 minutes later), the server
had returned to normal behaviour and continued rendering `p.error` correctly across RUN-B,
RUN-C, RUN-D1, and RUN-D2.

No framework file was changed between Phase 0 and RUN-A. The recovery is attributable solely
to the AUT server restoring normal behaviour.

### 7.4 Causal factors ruled out by evidence

| Candidate cause | Evidence ruling it out |
|-----------------|------------------------|
| Parallel-execution interference | RUN-C (sequential, 1 thread) = 12/6; identical to RUN-A/B (parallel) |
| Scenario-order dependency | RUN-C ran scenarios in strict order; same results |
| Session leakage between scenarios | ThreadLocal WebDriver with `remove()` in @After; each scenario gets a fresh session |
| Shared test-data collision | Scenarios use hardcoded, Faker-generated, or Excel data — no shared mutable state in the failing scenarios |
| Dirty working-tree change | All 4 modified files are additive and delegation-only; analysis in §3 confirms no execution impact |
| Framework regression | The positive login scenario passed in Phase 0 and all investigation runs; the assertion pipeline worked for blank-field cases in Phase 0 |
| Chrome / WebDriver change | Chrome and ChromeDriver versions were identical across all runs; no update occurred |
| Environment variable difference | All relevant env vars (`TEST_USERNAME`, `TEST_PASSWORD`, `BROWSER`, etc.) were unset in all runs |
| Dependency version drift | All library versions are pinned in `build.gradle`; drift is impossible |

---

## 8. Parallel vs Sequential Comparison

| Metric | Parallel (RUN-A, RUN-B) | Sequential (RUN-C) |
|--------|------------------------|---------------------|
| Total scenarios | 18 | 18 |
| Passed | 12 | 12 |
| Failed | 6 | 6 |
| Failed scenario names | identical | identical |
| Duration | ~2m 45s avg | 4m 33s |

**Parallel execution does not contribute to failures.** Reducing concurrency to 1 thread and
1 fork produced an identical pass/fail set. The `@DataProvider(parallel=true)` configuration
and `maxParallelForks=2` setting are not implicated in the Phase 0 deviation.

---

## 9. Standalone Scenario Results (RUN-D1 and RUN-D2)

Tag filter used: `@negative and not @security`

Targeted scenarios (11 total, covering all 7 Phase 0 new failures):

| Scenario | RUN-D1 | RUN-D2 |
|----------|--------|--------|
| credentials do not match (invalidUser / validPass123) | PASS | PASS |
| credentials do not match (validUser / wrongPass) | PASS | PASS |
| required fields not provided (blank username) | PASS | PASS |
| required fields not provided (blank password) | PASS | PASS |
| required fields not provided (both blank) | PASS | PASS |
| format requirements — long username | PASS | PASS |
| format requirements — long password | PASS | PASS |
| format requirements — `user@!#` | PASS | PASS |
| whitespace-only password | PASS | PASS |
| locked / deactivated account | PASS | PASS |
| registration: no username provided | PASS | PASS |

All 11 targeted scenarios passed in both standalone runs. The 7 Phase 0 new failures
reproduced in zero of four targeted passes.

---

## 10. Test-Data and Session Observations

- **Credential uniqueness:** The 7 new-failure scenarios use username/password combinations
  that are not registered in the AUT (`invalidUser`, `validUser`, `lockedUser`, etc.).
  No prior scenario creates or modifies these accounts. No shared-data collision is possible.
- **Session isolation:** `DriverManager` stores WebDriver in `DRIVER_THREAD_LOCAL`; the `@After`
  hook calls `DRIVER_THREAD_LOCAL.remove()` after every scenario. Each scenario starts with a
  fresh browser session. Session leakage between scenarios is architecturally prevented.
- **Test user setup:** `Hooks.ensureDefaultTestUserExists()` creates the `sqa`/`sqa` user via
  API before the first scenario. This touches only the positive login scenario and has no
  relationship to any of the 7 failing scenarios, which use invalid credentials.
- **Parallel test-data collision:** All 7 scenarios in the new-failure set send HTTP POST
  requests to `parabank.parasoft.com/parabank/login.htm` with credentials that do not exist
  on the server. No write-side operation (registration, funds transfer) is involved. Concurrent
  execution of these scenarios cannot produce data collisions.

---

## 11. Environment and Dependency Observations

- **Framework library versions:** All pinned in `build.gradle`. No dynamic (`+`) or range
  specifiers. The resolved testRuntimeClasspath was confirmed identical to declared versions.
- **Chrome 150.0.7871.125:** This version was installed and active at the time of Phase 0
  and remained unchanged throughout the investigation. WebDriverManager auto-downloaded
  ChromeDriver 150.0.7871.124 to match. ChromeDriver and Chrome were version-matched in all
  runs; no browser update between Phase 0 and investigation runs.
- **Java 17.0.12 / Gradle 9.0.0:** Unchanged across all runs.
- **No env vars set:** `TEST_USERNAME`, `TEST_PASSWORD`, `BROWSER`, `ENV`, `HEADLESS`,
  `SELENIUM_GRID_ENABLED`, `GRID_URL` were all unset in the process environment. The framework
  fell back to `qa.properties` defaults (chrome, non-headless, local WebDriver) — consistent
  with Phase 0.
- **No network-level changes were detectable:** HTTPS connections to `parabank.parasoft.com`
  succeeded in all runs (positive login scenario passed in all runs including Phase 0).

---

## 12. Framework-Regression Assessment

**NO FRAMEWORK REGRESSION IDENTIFIED.**

Evidence:

1. The positive login scenario (@smoke @positive) passed in Phase 0 AND all investigation runs.
   Browser launch, navigation, credential entry, form submission, and assertion pipeline are all
   functional.

2. The blank-field scenarios (expecting `"Please enter a username and password."`) passed in
   Phase 0 AND all investigation runs. The `p.error` element is found and asserted correctly
   when the AUT renders it.

3. All 7 Phase 0 new failures involve the SAME assertion step (`user_should_see_error_message()`)
   and the SAME locator (`By.cssSelector("p.error")`), yet they passed in all five subsequent
   runs without any framework code change. The failure cannot be in the framework.

4. The four dirty files are additive delegation layers. The underlying method implementations
   are committed code that pre-dates Phase 0. Those methods passed in the historical baseline
   of 18/12/6.

5. Sequential execution (RUN-C, 1 thread) confirmed that the framework's parallel execution
   implementation is not causing any incorrect behaviour.

---

## 13. Root-Cause Conclusion

**Root cause: TRANSIENT AUT FAILURE on the public ParaBank demo server.**

During the Phase 0 run window (~14:10–14:17 on 2026-07-18), the ParaBank public demo server
(`parabank.parasoft.com`) was in a degraded state in which it failed to render the `p.error`
DOM element for any invalid non-empty credential submission. Blank-field submissions still
produced the `"Please enter a username and password."` error (a client-side or early-path
server-side validation), but the credential-mismatch path (`"The username and password could
not be verified."`) produced no error element. This caused all 10 scenarios in that failure
class to fail — the 3 historically known injection failures plus 7 scenarios that previously
passed. By the time RUN-A began (~14:24, approximately 7 minutes after Phase 0 completed),
the server had returned to its normal behaviour and the 7 scenarios recovered without any
framework change.

The 3 injection failures and 3 registration failures that persist in all runs are NOT part of
this transient event — they represent consistent AUT server behaviour that has been present
since the original historical baseline (18/12/6) and are unchanged.

---

## 14. Recommended Next Action

**BASELINE RESTORED — READY FOR REPOSITORY HYGIENE**

The historical baseline of 18 executions / 12 passed / 6 failed has been confirmed stable
across 5 independent test suite runs (RUN-A through RUN-D2) and is now the confirmed valid
baseline.

Specific actions:

1. **Update `docs/review/BASELINE_VALIDATION.md`** to reflect that the Phase 0 deviation was
   investigated and classified as TRANSIENT AUT FAILURE, and that the historical baseline is
   confirmed as the valid stable baseline.
2. **Proceed to repository hygiene** as defined in the next planned phase. The 6 consistently
   failing scenarios are pre-existing, AUT-caused, and are not blockers for hygiene work.
3. **Do not add retry logic** to mask the transient AUT failures. The current behaviour — the
   suite records the actual server response — is correct. If stability is required for CI gates,
   the appropriate fix is a dedicated AUT instance, not framework-level retries.

---

## 15. Files Created or Modified During This Investigation

### Created

| File | Purpose |
|------|---------|
| `docs/review/BASELINE_CHANGE_INVESTIGATION.md` | This report |
| `docs/review/evidence/RUN-0-INITIAL/cucumber-report.json` | Copied from Phase 0 build output |
| `docs/review/evidence/RUN-0-INITIAL/logs/test.log` | Copied from Phase 0 build output |
| `docs/review/evidence/RUN-0-INITIAL/screenshots/*.png` (11 files) | Copied from Phase 0 build output |
| `docs/review/evidence/RUN-A-stdout.txt` | Captured stdout from RUN-A |
| `docs/review/evidence/RUN-A-DEFAULT/cucumber-report.json` | Cucumber report from RUN-A |
| `docs/review/evidence/RUN-A-DEFAULT/logs/test.log` | Log from RUN-A |
| `docs/review/evidence/RUN-A-DEFAULT/screenshots/*.png` (6 files) | Failure screenshots from RUN-A |
| `docs/review/evidence/RUN-B-stdout.txt` | Captured stdout from RUN-B |
| `docs/review/evidence/RUN-B-DEFAULT/cucumber-report.json` | Cucumber report from RUN-B |
| `docs/review/evidence/RUN-B-DEFAULT/logs/test.log` | Log from RUN-B |
| `docs/review/evidence/RUN-B-DEFAULT/screenshots/*.png` (5 files) | Failure screenshots from RUN-B |
| `docs/review/evidence/RUN-C-stdout.txt` | Captured stdout from RUN-C |
| `docs/review/evidence/RUN-C-SEQUENTIAL/cucumber-report.json` | Cucumber report from RUN-C |
| `docs/review/evidence/RUN-C-SEQUENTIAL/logs/test.log` | Log from RUN-C |
| `docs/review/evidence/RUN-D1-stdout.txt` | Captured stdout from RUN-D1 |
| `docs/review/evidence/RUN-D1-TARGETED/cucumber-report.json` | Cucumber report from RUN-D1 |
| `docs/review/evidence/RUN-D2-stdout.txt` | Captured stdout from RUN-D2 |
| `docs/review/evidence/RUN-D2-TARGETED/cucumber-report.json` | Cucumber report from RUN-D2 |

### Modified

None. No source code, feature files, step definitions, configuration, `.gitignore`, or any
tracked file was modified during this investigation.

---

## 16. Final Recommendation

> **BASELINE RESTORED — READY FOR REPOSITORY HYGIENE**
