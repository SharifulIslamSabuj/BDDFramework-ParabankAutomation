# Known AUT Limitations

**Framework:** ParaBank BDD Automation  
**Application Under Test:** ParaBank Online Banking — [parabank.parasoft.com](https://parabank.parasoft.com/parabank/)  
**Validated against commit:** 96fa5a7 (pre-Phase 7 baseline)  
**Last Updated:** 2026-07-18

---

## Purpose

This document records the observed limitations of the public ParaBank demo application that
cause consistent or intermittent test failures. It distinguishes AUT behaviour from framework
defects and explains why the affected test scenarios remain active and valuable.

---

## Validated Baseline

| Metric | Value |
|---|---|
| Cucumber scenario executions | 18 |
| Passed | 12 |
| Known AUT failures | 6 |
| Unexpected failures | 0 |
| Framework regressions | 0 |
| Validation commit | `96fa5a7` |

---

## Classification Rules

| Term | Definition |
|---|---|
| **AUT limitation** | Observed application behaviour that differs from the test assertion and is reproducible without evidence of framework malfunction |
| **Environment instability** | Intermittent external behaviour that may change between identical executions without source changes |
| **Framework defect** | A reproducible problem in framework code causing incorrect execution, false failure, or false pass |
| **Accepted known failure** | A consistently failing scenario that is preserved intentionally to document AUT behaviour |

All six scenarios documented below are classified as **accepted known failures** caused by AUT
limitations or environment conditions. No framework defect is involved.

---

## Current Known Limitations

### AUT-LIM-001 — Security-Oriented Login Inputs Do Not Produce the Expected Error State

**Limitation category:** AUT behaviour  
**Framework impact:** Test failure (assertion not satisfied)  
**Owner:** External AUT / Outside repository control

#### Affected scenarios

All three executions of the Scenario Outline:
> `Sign-in is protected against injection and scripting attacks` (`login.feature`)

Executed with these input combinations:

| runScenario | Username | Password |
|---|---|---|
| [9] | `'; DROP TABLE users; --` | `validPass123` |
| [10] | `validUser` | `' OR '1'='1` |
| [11] | `<script>alert('xss')</script>` | `validPass123` |

#### Expected behaviour

For each input combination, the AUT should reject the login attempt and render a validation
error element (`p.error`) containing a message such as
`"The username and password could not be verified."`.

#### Actual observed behaviour

The `p.error` element is not rendered by the AUT. The page does not transition to the overview
and no error element becomes visible within the explicit wait timeout (30 seconds).
The assertion `waitForElementToBeVisible(driver, ERROR_MESSAGE)` times out and throws
`WaitException → TimeoutException`.

#### Determination of AUT vs. framework cause

- The same assertion logic (`user_should_see_error_message()`) passes for 8 of 11 invalid-login
  scenarios, including similar non-special-character invalid-credential scenarios.
- The framework's wait and locator are correct — confirmed by consistently passing scenarios.
- The inputs contain characters that the ParaBank public demo server appears to process
  differently from ordinary invalid credentials: either silently redirecting, returning an empty
  page, or handling without rendering the expected error element.
- No framework code path differs between the passing and failing invalid-login executions.

#### Interpretation

The public ParaBank demo server does not produce the expected visible error state for these
specific security-oriented inputs. This is an input-handling characteristic of the AUT, not
a confirmed exploitable vulnerability. The test observes that the expected error response is
absent. No assertion is made about whether these inputs represent an exploitable condition.

#### First confirmed evidence

Phase 0 Baseline Validation — commit `aa3c243` — 2026-07-18  
(Originally observed as part of the 13-failure run, then confirmed as part of the stable 6-failure
baseline in Phase 0.1 investigation.)

#### Latest confirmed evidence

Phase 7 pre-change validation — commit `96fa5a7` — 2026-07-18  
`runScenario[9]`, `runScenario[10]`, `runScenario[11]` — all FAILED with `WaitException`.

#### Why the test remains active

These scenarios provide visible evidence of how the AUT responds to security-oriented inputs.
Suppressing or removing them would hide a real behavioural difference between expected and
actual AUT response. Their consistent failure is the signal; a future fix of the AUT behaviour
would cause them to pass, providing automatic regression detection.

#### Current treatment

- Active scenarios — not skipped or suppressed
- Classified as known AUT failures in all documentation
- Counted against the 6-failure baseline
- Visible in all reports (ExtentReports, Cucumber HTML, TestNG, Allure)

#### Workaround

None available at the framework level without changing assertions or AUT behaviour.

#### Revalidation guidance

If a future run shows these scenarios **passing**, investigate whether:
1. The AUT has been updated to display the expected error element
2. The ParaBank server response has changed in a way that now satisfies the assertion

If a future run shows **fewer than 3** of these failing (e.g. only 2), investigate whether:
1. A partial AUT fix occurred
2. The scenario tag selection changed

---

### AUT-LIM-002 — Registration Does Not Consistently Expose the Expected Authenticated State

**Limitation category:** AUT behaviour / environment condition  
**Framework impact:** Test failure (assertion not satisfied)  
**Owner:** External AUT / Outside repository control

#### Affected scenarios

Three positive registration scenarios in `register.feature`:

| runScenario | Scenario title | Data source |
|---|---|---|
| [13] | A new customer can open a bank account with their personal information | Static profile (page object default data) |
| [14] | A new customer can open a bank account with a freshly generated profile | LoremIpsum generated data |
| [15] | A customer can open a bank account using details provided by an external source | Excel (`ddt.xlsx`) via `ExcelDataProvider` |

#### Expected behaviour

After submitting the registration form:
1. The AUT redirects to the account overview page.
2. The logout link (`a[href='logout.htm']`) is present and visible, confirming the session is
   authenticated.

The framework asserts `waitForElementToBeVisible(driver, LOGOUT_LINK)` — the presence of this
element confirms successful registration and session establishment.

#### Actual observed behaviour

The `a[href='logout.htm']` element is not found after registration form submission. The AUT
does not redirect to the overview page as expected. The explicit wait times out and throws:

```
java.lang.RuntimeException: Registration may have failed - logout link not found on OverviewPage
    org.openqa.selenium.TimeoutException: Expected condition failed: waiting for visibility
    of element found by By.cssSelector: a[href='logout.htm']
    org.openqa.selenium.NoSuchElementException: no such element
```

#### Determination of AUT vs. framework cause

- The `RegisterPage.clickRegisterButtonWithFail()` method submits the form correctly — confirmed
  by consistent execution without driver errors.
- The locator `a[href='logout.htm']` on `OverviewPage` is correct — it passes in login-flow
  scenarios that reach the same page via a different route.
- The AUT either rejects the registration silently, encounters a duplicate-username condition,
  or returns to the registration page without producing the expected authenticated session.
- All three scenarios use different data sources (static, generated, Excel), which means the
  data source is not the distinguishing factor — the shared behaviour is the AUT's server-side
  session handling after registration.

#### Registration data and duplicate consideration

The `sqa` test user is registered by `Hooks.ensureDefaultTestUserExists()` before the suite.
The positive registration scenarios use separate data (not `sqa`), but the ParaBank public demo
server accumulates registrations across sessions from all users worldwide. Registration attempts
on the shared public server may encounter duplicate-username rejection or server-side throttling
that does not surface an explicit error but also does not establish an authenticated session.

Whether the registration data was submitted to the server cannot be determined from the framework
side when the session state is not returned as expected.

#### First confirmed evidence

Phase 0 Baseline Validation — commit `aa3c243` — 2026-07-18

#### Latest confirmed evidence

Phase 7 pre-change validation — commit `96fa5a7` — 2026-07-18

#### Why the test remains active

These are the primary positive registration scenarios. They verify the complete happy-path
registration flow. Their current failure documents the actual behaviour of the public demo AUT.
A future AUT fix or a deployment against a clean, dedicated server would cause them to pass.

#### Current treatment

- Active scenarios — not skipped or suppressed
- Classified as known AUT failures
- Tagged `@positive` — their failure makes it clear the positive path is not consistently
  accessible on the public demo
- Visible in all reports

#### Workaround

Running these scenarios against a private, resettable ParaBank deployment would eliminate the
shared-state and duplicate-user conditions. This is outside the scope of the current repository.

#### Revalidation guidance

If these scenarios pass in a future run, investigate whether:
1. Tests ran against a fresh or private ParaBank deployment
2. Unique username generation was introduced
3. AUT server-side session behaviour changed

---

### AUT-LIM-003 — Public ParaBank Environment Exhibits Intermittent Availability and State Instability

**Limitation category:** Environment instability  
**Framework impact:** Additional transient failures beyond the stable 6 baseline  
**Owner:** Outside repository control

#### Description

The public ParaBank demo server at `parabank.parasoft.com` is a shared, publicly accessible
instance. Its state, availability, and response behaviour are not controlled by this repository.

#### Observed evidence

Two documented transient instability events:

**Phase 3 (commit 7713bf0) — first regression run:**  
Result: 18/5/13 — 7 additional failures beyond the 6-failure baseline.  
Pattern: The `p.error` element was absent for ALL invalid non-empty credential scenarios (not
just the injection probes), which is clearly different from the stable pattern where only the
injection scenarios fail. This indicates server-side degradation, not framework regression.  
Confirmation run: 18/12/6 — baseline restored without framework changes.

**Phase 5 (commit e118ac9) — first regression run:**  
Result: `runScenario[16]` ("Registration is rejected when the customer does not provide a
username") failed with `AssertionError: At least one error message should be displayed on
RegisterPage`. This scenario passes consistently in all other validated runs.  
Confirmation run: 18/12/6 — baseline restored without framework changes.

#### Pattern characteristics

- Instability affects scenarios beyond the known 6-failure set
- Affected scenarios pass before and after the instability window
- The instability always resolves on re-run without any framework changes
- Server recovery typically occurs within minutes

#### Framework impact

When transient instability occurs, the run result temporarily shows 7 or more failures instead
of the expected 6. This should not be classified as a framework regression without:
1. A second run confirming the same additional failure
2. A diff showing a framework source change that could explain it

#### Current treatment

- Documented as an accepted environment risk
- The established protocol is: one rerun before classifying a deviation as a framework regression

---

## Affected Scenario Matrix

| Limitation | Feature | Scenario execution | runScenario | Expected | Observed | Status |
|---|---|---|---|---|---|---|
| AUT-LIM-001 | `login.feature` | Sign-in is protected against injection and scripting attacks | [9] | `p.error` element visible | `p.error` absent — timeout | Known AUT failure |
| AUT-LIM-001 | `login.feature` | Sign-in is protected against injection and scripting attacks | [10] | `p.error` element visible | `p.error` absent — timeout | Known AUT failure |
| AUT-LIM-001 | `login.feature` | Sign-in is protected against injection and scripting attacks | [11] | `p.error` element visible | `p.error` absent — timeout | Known AUT failure |
| AUT-LIM-002 | `register.feature` | A new customer can open a bank account with their personal information | [13] | `a[href='logout.htm']` visible | Element absent — timeout | Known AUT failure |
| AUT-LIM-002 | `register.feature` | A new customer can open a bank account with a freshly generated profile | [14] | `a[href='logout.htm']` visible | Element absent — timeout | Known AUT failure |
| AUT-LIM-002 | `register.feature` | A customer can open a bank account using details provided by an external source | [15] | `a[href='logout.htm']` visible | Element absent — timeout | Known AUT failure |
| AUT-LIM-003 | All | Any scenario beyond the 6 listed above | n/a | Pass | Transient timeout | Environment — resolves on rerun |

---

## Reproduction and Revalidation

### Reproducing the known 6 failures

```bash
git clone https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation.git
cd BDDFramework-ParabankAutomation
./gradlew clean test
# Expected: 30 tests — 18 Cucumber (12 passed, 6 failed), 12 safety (all passed)
```

### Reproducing injection-probe failures only

```bash
./gradlew clean test -Dcucumber.filter.tags="@negative and @security"
# Expected: 3 executions, 3 failed (AUT-LIM-001)
```

### Reproducing registration failures only

```bash
./gradlew clean test -Dcucumber.filter.tags="@positive"
# Expected: 4 executions, 3 failed (AUT-LIM-002), 1 passed (login positive)
```

### Revalidation after an apparent regression

1. Confirm the failing scenario is not in the established 6-failure list.
2. Confirm no framework source changes were introduced (`git diff`).
3. Re-run once: `./gradlew clean test`.
4. If the additional failure resolves: classify as AUT-LIM-003 (transient) — no action needed.
5. If the additional failure persists through two runs: open a framework investigation.

---

## Framework Behaviour

The framework does not retry, suppress, or skip known failing scenarios. Every run that
produces exactly 18 executions with 12 passed and 6 failed represents a successful validation
of the framework. The 6 failures are the expected signal, not an error condition in the
framework itself.

The Gradle build exits with a non-zero exit code because TestNG reports test failures. This is
correct CI behaviour: the build accurately signals that scenarios did not meet assertions.

---

## External Environment Instability

The AUT's public demo server is shared with the internet and does not offer:

- State isolation between test runs
- Guaranteed availability SLA
- Deterministic registration outcomes for repeated usernames
- Controlled server-side input handling for special characters

This repository cannot control or mitigate these conditions at the framework level. The
accepted approach is documented in AUT-LIM-003 above.

---

## What These Failures Do Not Mean

| Common misinterpretation | Accurate statement |
|---|---|
| "The framework is broken" | The framework produces the same 6 failures reproducibly — framework execution is correct |
| "These are SQL injection vulnerabilities" | The AUT does not display the expected error state for these inputs; no exploitation has been confirmed |
| "The registration feature is not implemented" | Registration step definitions, page objects, and assertions are fully implemented; the AUT does not produce the expected session state |
| "The tests are flaky" | The 6 failures are consistent; AUT-LIM-003 events are transient and distinct from the baseline |
| "The framework needs retries" | Retries would mask AUT behaviour and reduce the diagnostic value of the test suite |

---

## Resolution and Ownership

| Limitation | Resolution path | Owner |
|---|---|---|
| AUT-LIM-001 (login injection response) | AUT must render expected error element for these inputs | External AUT / ParaBank maintainers |
| AUT-LIM-002 (registration session state) | AUT must establish an authenticated session after registration on the public demo | External AUT / ParaBank maintainers |
| AUT-LIM-003 (server instability) | Public demo server reliability — not addressable by this repository | Outside repository control |

The framework maintainer's role for these limitations is:
1. Monitor that the 6-failure count does not increase unexpectedly
2. Treat a count decrease as a potential AUT fix — revalidate
3. Treat a count increase as a potential regression — investigate before accepting
