# Test Strategy

**Framework:** ParaBank BDD Automation  
**Application Under Test:** ParaBank Online Banking — [parabank.parasoft.com](https://parabank.parasoft.com/parabank/)  
**Validated commit:** e118ac9  
**Last Updated:** July 2026

---

## Related Documents

| Document | Purpose |
|---|---|
| [QUALITY_RISK_ASSESSMENT.md](QUALITY_RISK_ASSESSMENT.md) | Risk register, technical debt, test debt, and release interpretation |
| [KNOWN_AUT_LIMITATIONS.md](KNOWN_AUT_LIMITATIONS.md) | Detailed records of the 6 known AUT failures |

---

## 1. Why BDD?

Behaviour-Driven Development with Gherkin was chosen over plain JUnit/TestNG for three reasons:

- **Business readability** — feature files describe what the system should do in plain English, enabling non-technical reviewers to verify test coverage without reading Java
- **Separation of concerns** — Gherkin steps map to Java methods, but those methods stay thin; all UI logic lives in page objects, not in step definitions
- **Living documentation** — feature files are executable specifications that stay in sync with the codebase by definition; they cannot become stale comments

---

## 2. Scope

### In Scope

| Feature Area | Test Coverage |
|---|---|
| Authentication | Login success, invalid credentials, blank field validation, injection probes, boundary-length inputs |
| Registration | Form submission with static data, generated data (LoremIpsum), Excel DDT, missing-field validation |
| Account Management | `OpenNewAccountPage`, `OpenedAccountPage` — page objects complete; feature scenarios planned |
| Loan Management | `RequestLoanPage`, `ApprovedLoanPage` — page objects complete; feature scenarios planned |
| Profile Management | `UpdateProfilePage` — page object complete; feature scenario planned |

### Out of Scope

- Backend API contract testing (no REST Assured / Postman collection)
- Database-level assertions (no JDBC)
- Performance and load testing
- Visual regression (no pixel comparison)
- Mobile browser / responsive layout testing

---

## 3. Test Categories and Tags

All scenarios are tagged for flexible suite selection at runtime.

| Tag | Purpose | When to Run |
|---|---|---|
| `@smoke` | Single critical happy-path; fast sanity check | Every push |
| `@regression` | Feature-level tag — full regression suite | Pre-release / nightly |
| `@positive` | Happy-path scenarios | Regression |
| `@negative` | Boundary, invalid input, and rejection scenarios | Regression / nightly |
| `@validation` | Input-validation and business-rule scenarios | Regression |
| `@security` | Injection and scripting-probe scenarios | Regression / nightly |

**Default suite** (`./gradlew clean test`) runs `@smoke or @negative or @regression` — 18 scenario executions.

**Tag override:**
```bash
./gradlew clean test -Dcucumber.filter.tags="@smoke"                    # 1 scenario
./gradlew clean test -Dcucumber.filter.tags="@negative"                 # all negative scenarios
./gradlew clean test -Dcucumber.filter.tags="@regression"               # full regression
./gradlew clean test -Dcucumber.filter.tags="@negative and @security"   # injection probes only
```

---

## 4. Test Data Strategy

### Principles

- **No PII in source code** — no real names, addresses, SSNs, or phone numbers hardcoded anywhere
- **No credentials in source** — usernames and passwords come from environment variables or a non-committed secrets store; the only credentials in properties files are the public ParaBank demo credentials (`sqa / sqa`)
- **Passwords are never logged** — `ConfigManager.getPassword()` fetches credentials silently; no log statement in the framework prints a password

### Data Sources

| Source | Usage |
|---|---|
| `LoremIpsum` library | Generated first/last names, addresses, phone numbers for registration tests |
| `ddt.xlsx` (Apache POI) | External Excel file read by `ExcelDataProvider` for the external-source registration scenario |
| `qa.properties` | Default credentials (`sqa / sqa`) — public ParaBank demo account only |
| `TEST_USERNAME` / `TEST_PASSWORD` | Environment variables that override properties in CI and Docker |

### Credential Priority Chain

```
-DTEST_USERNAME  (JVM system property)
      ↓ fallback
TEST_USERNAME    (environment variable)
      ↓ fallback
username         (properties file)
      ↓ fallback
null             (test fails with clear message)
```

### Test User Setup

Before the first scenario, `Hooks.ensureDefaultTestUserExists()` runs once (thread-safe via `AtomicBoolean`):

1. **API registration** — `ParaBankApiClient` sends an HTTP POST to `/register.htm` using Java 17 `HttpClient`. Success is detected by a redirect away from the register page, or "This username already exists" in the response body (both are considered success).
2. **Browser fallback** — if the API call returns an unexpected response, the hook falls through to browser-based registration as a safety net.

This guarantees the `sqa` test user exists regardless of server state before any login scenario runs.

---

## 5. Environment Strategy

| Environment | Purpose | Activation |
|---|---|---|
| `qa` *(default)* | Public ParaBank demo server | `./gradlew clean test` |
| `staging` | Staging server (when available) | `-Denv=staging` |
| `uat` | UAT server (when available) | `-Denv=uat` |
| `prod` | Production — automatic test-data writes are blocked by the production guard | `-Denv=prod` |

Each environment loads its own `.properties` file from `src/test/resources/config/`. Credentials for non-QA environments must be supplied via `TEST_USERNAME` / `TEST_PASSWORD` environment variables — they are intentionally absent from source.

> **Production write protection:** When `env` is `prod` or `production` (case-insensitive),
> `ConfigManager.guardAgainstProductionWrite()` throws a `ConfigurationException` before any
> automatic test-user registration is attempted. Both the API path and the browser fallback are
> protected by a single orchestration-level guard in `Hooks.ensureDefaultTestUserExists()`.
> Read-only scenario execution is not restricted by this guard.

---

## 6. Parallel Execution Strategy

Tests run in parallel by default via TestNG's `@DataProvider(parallel=true)`.

**Thread safety is maintained by:**
- `DriverManager` stores `WebDriver` in `ThreadLocal<WebDriver>` — one browser instance per thread, no sharing
- One-time test user setup uses double-checked locking (`AtomicBoolean initialized` + `synchronized` block) to prevent races
- SLF4J MDC stores `scenarioName` per thread — log lines from concurrent threads remain traceable

| Environment | Thread Count | Reason |
|---|---|---|
| Local development | 3 (default) | Balanced for developer machines |
| GitHub Actions CI | 1 | Avoids resource contention on 2-vCPU runners |
| Docker + Grid | 1 (compose default) | `docker-compose.grid.yml` sets `DATA_PROVIDER_THREAD_COUNT=1`; host-side default is 2 (build.gradle); chrome-node supports 2 concurrent sessions (`SE_NODE_MAX_SESSIONS=2`) |

---

## 7. Known Failures and Baseline

The following 6 scenarios consistently fail against the public ParaBank demo server. These are **server-side issues, not framework defects**.

| Scenario | Feature | Root Cause |
|---|---|---|
| Sign-in is protected against injection — SQL in username | `login.feature` | Server sanitizes input silently; no `p.error` element is rendered — assertion finds nothing |
| Sign-in is protected against injection — SQL in password | `login.feature` | Same — no error element rendered |
| Sign-in is protected against injection — XSS in username | `login.feature` | Same |
| A new customer can open a bank account with their personal information | `register.feature` | Demo server session does not redirect to overview after registration — logout link absent |
| A new customer can open a bank account with a freshly generated profile | `register.feature` | Same server-side session issue |
| A customer can open a bank account using details provided by an external source | `register.feature` | Same server-side session issue |

These scenarios are tagged `@security` (login) and `@positive` (register); they are intentionally preserved
to document actual AUT behaviour. They are not suppressed, skipped, or retried away.

**Established baseline (commit e118ac9): 18 scenario executions — 12 passed, 6 known AUT failures.**

If your run shows 7+ failures, the public demo server may be experiencing transient instability
(observed during Phase 0.1 and Phase 5 validation). Re-run once; server-side transient failures
resolve on retry. A stable additional failure warrants investigation before being classified as a
framework regression.

---

## 8. Focused Framework Tests

In addition to Cucumber BDD scenarios, the framework includes a set of non-Cucumber, non-browser focused
tests that verify framework internals deterministically.

| Class | Package | Count | Purpose |
|---|---|---|---|
| `ProductionSafetyGuardTest` | `com.parabank.parasoft.config` | 12 | Validates `ConfigManager` production detection and write-guard behaviour |

These tests:
- Do not start a browser
- Do not contact the ParaBank server or any external service
- Use `ConfigManager.resetInstance()` with `System.setProperty("env", ...)` for isolation
- Run as part of `./gradlew clean test` alongside Cucumber scenarios
- Are not affected by ParaBank server instability

**Validated total (commit e118ac9): 30 Gradle tests — 12 focused + 18 Cucumber.**

---

## 9. Future Test Expansion

Five page objects are fully implemented and ready for feature file coverage:

| Page Object | Planned Scenarios |
|---|---|
| `OpenNewAccountPage` | Open CHECKING account, open SAVINGS account, verify account type in dropdown |
| `OpenedAccountPage` | Confirm new account ID displayed, verify account type on confirmation page |
| `RequestLoanPage` | Submit loan application with valid amount, submit with insufficient down payment |
| `ApprovedLoanPage` | Verify loan ID on approval, verify loan status as APPROVED |
| `UpdateProfilePage` | Update phone number, update address, verify success message |

Adding feature coverage for these pages would expand the test suite from 18 to approximately 30–40 scenarios.

---

## 10. Risk Register

| Risk | Likelihood | Mitigation |
|---|---|---|
| Demo server downtime | Medium | API-first setup is idempotent; most login scenarios need no prior registration |
| Repeat-registration throttling | Medium | `ParaBankApiClient` treats "already exists" as success; avoids duplicate registration attempts |
| Parallel test interference | Low | `ThreadLocal` WebDriver; `AtomicBoolean` one-time setup; isolated browser sessions |
| Browser/ChromeDriver version mismatch | Low | `WebDriverManager` auto-downloads the matching driver at runtime |
| Screenshot capture failure on Grid | Low | `TakesScreenshot` is cast from `RemoteWebDriver` — same code path as local Chrome |
