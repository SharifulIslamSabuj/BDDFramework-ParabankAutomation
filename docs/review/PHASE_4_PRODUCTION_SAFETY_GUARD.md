# Phase 4 — Production Safety Guard Report

## 1. Objective

Prevent automatic test-data creation from running against a production environment by
placing a fail-fast guard at the single orchestration boundary that dispatches all
write-capable setup operations before any scenario executes.

---

## 2. Repository, Branch, and Starting Commit

| Field          | Value                                                                  |
|----------------|------------------------------------------------------------------------|
| Repository URL | https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation |
| Branch         | main                                                                   |
| HEAD at start  | `33438c56ae2b71f8951aa26e575b42cdc44a01be`                            |
| Phase 4 commit | see §20                                                                |

---

## 3. Initial Working-Tree State

```
?? docs/review/evidence/
```

Exactly one untracked directory. All committed files were clean.

---

## 4. `docs/review/evidence/` Assessment

**Contents**: Raw Phase 0.1 investigation execution artifacts — Cucumber JSON reports,
test log files, and failure screenshots from 6 investigation runs (RUN-0-INITIAL through
RUN-D2-TARGETED).

**Decision: REMOVED locally.**

**Rationale**: All findings from these runs are fully captured in the committed
`docs/review/BASELINE_CHANGE_INVESTIGATION.md`. The artifacts are generated binary
and text outputs (`.png`, `.log`, `.json`) with no source-control value. They are
not staged in Phase 4.

---

## 5. Pre-Change Validation Result

Command: `./gradlew clean test`

| Metric              | Expected | Result |
|---------------------|----------|--------|
| Total executions    |       18 |     18 |
| Passed              |       12 |     12 |
| Failed              |        6 |      6 |
| Known AUT failures  |        6 |      6 |
| Unexpected failures |        0 |      0 |

Baseline confirmed. Implementation proceeded.

---

## 6. Environment-Configuration Map

| Property                   | Value / Behaviour                                                                             |
|----------------------------|-----------------------------------------------------------------------------------------------|
| **Source class**           | `ConfigManager` (singleton, `com.parabank.parasoft.config`)                                   |
| **System property**        | `-Denv`                                                                                       |
| **Default value**          | `"qa"` (via `TestDataConstants.ENV_QA`)                                                       |
| **Case handling**          | Lowercased on load: `System.getProperty("env", ...).toLowerCase()`                            |
| **Whitespace handling**    | Not explicitly trimmed at load; system property typically has no whitespace                   |
| **Properties file**        | `src/test/resources/config/{env}.properties` (falls back to `config/config.properties`)       |
| **Supported config files** | `qa.properties`, `staging.properties`, `uat.properties`, `prod.properties`, `config.properties` |
| **Existing environment methods** | `isQAEnvironment()`, `isStagingEnvironment()`, `isProductionEnvironment()`              |
| **Singleton mutability**   | Immutable after construction; `resetInstance()` available for test isolation                  |
| **Runtime override**       | Via system property; Gradle `build.gradle` forwards `-Denv` to test JVM                      |
| **Production detection**   | By environment name (not URL). Pre-Phase 4: only "prod". Post-Phase 4: "prod" OR "production" |

---

## 7. Supported Environment Values

| Value         | Properties file loaded      | Production? (pre-Phase 4) | Production? (post-Phase 4) |
|---------------|------------------------------|--------------------------|---------------------------|
| `qa`          | `qa.properties`              | No                       | No                         |
| `staging`     | `staging.properties`         | No                       | No                         |
| `uat`         | `uat.properties`             | No                       | No                         |
| `prod`        | `prod.properties`            | Yes                      | Yes                        |
| `production`  | Falls back to `config.properties` | No                  | **Yes (newly added)**      |
| _(missing)_   | Falls back to `config.properties` or throws `ConfigurationException` | No | No |

**Pre-Phase 4 gap**: Environment value `"production"` (common alias) was NOT matched by
`isProductionEnvironment()`. A user passing `-Denv=production` would bypass the guard.
This gap is closed in Phase 4.

---

## 8. Write-Capable Operation Map

### Write paths

| Entry point | Orchestration layer | API write path | Browser fallback path |
|-------------|--------------------|-----------------|-----------------------|
| `Hooks.beforeScenario()` | `Hooks.ensureDefaultTestUserExists()` | `tryApiRegistration()` → `ParaBankApiClient.registerUser()` | `tryBrowserRegistration()` → `LoginPage` → `RegisterPage` → `clickRegisterButtonWithFail()` |

### Full call chain

```
Hooks.@Before beforeScenario(Scenario)
  └─ ensureDefaultTestUserExists()              ← guard applied here (Phase 4)
       └─ tryApiRegistration(url, user, pass)   ← primary write path
            └─ ParaBankApiClient.registerUser() ← HTTP POST to register.htm
       └─ tryBrowserRegistration(url, user, pass) ← fallback write path
            └─ RegisterPage.clickRegisterButtonWithFail() ← browser form submission
```

### Pre-Phase 4 production detection

`isProductionEnvironment()` previously delegated entirely to
`TestDataConstants.ENV_PRODUCTION.equalsIgnoreCase(environment)`, which evaluated to
`"prod".equalsIgnoreCase(environment)`. The alias `"production"` was not covered.

---

## 9. Selected Guard Boundary and Rationale

**Boundary selected**: `ConfigManager.guardAgainstProductionWrite(String operation)`,
called from `Hooks.ensureDefaultTestUserExists()` inside the synchronized block, before
both write paths.

**Rationale**:
- `ensureDefaultTestUserExists()` is the single orchestration entry point for all
  test-data write operations. Both the API path and the browser fallback path originate here.
- Placing the guard here guarantees that neither write path can execute in production,
  regardless of whether the API succeeds or fails.
- The guard fires inside the double-checked lock — it cannot be bypassed by the
  fast-path `AtomicBoolean` check (which only triggers after setup is already done).
- `ConfigManager` is the correct owner of the guard method: environment detection already
  lives there, and `ConfigurationException` is already its established exception type.
- Page objects, element utilities, and API clients are NOT the guard boundary — those
  are implementation details that could be called independently in future scenarios.

---

## 10. Guard Implementation

### `ConfigManager.isProductionEnvironment()` — expanded

```java
// BEFORE
public boolean isProductionEnvironment() {
    return TestDataConstants.ENV_PRODUCTION.equalsIgnoreCase(environment);
}

// AFTER
public boolean isProductionEnvironment() {
    return "prod".equalsIgnoreCase(environment) || "production".equalsIgnoreCase(environment);
}
```

Added the "production" alias. Case-insensitive comparison matches "PROD", "Production",
"PRODUCTION", etc.

### `ConfigManager.guardAgainstProductionWrite(String operation)` — new method

```java
public void guardAgainstProductionWrite(String operation) {
    if (isProductionEnvironment()) {
        throw new ConfigurationException(
                "Automatic test-data creation is disabled in the production environment. " +
                "Detected environment: '" + environment + "'. Blocked operation: " + operation + ".");
    }
}
```

### `Hooks.ensureDefaultTestUserExists()` — guard call added

```java
// Inside the synchronized block, before any write:
config.guardAgainstProductionWrite("default test user registration");
```

The guard is placed after the singleton and config are retrieved but before
`tryApiRegistration()` or `tryBrowserRegistration()` are called.

---

## 11. Exception Behaviour

| Property | Value |
|----------|-------|
| Exception type | `ConfigurationException` (existing class, extends `FrameworkException`) |
| When thrown | Inside `ConfigManager.guardAgainstProductionWrite()`, when `isProductionEnvironment()` is true |
| Message includes | Phrase "production environment", detected environment name, blocked operation name |
| Message excludes | Passwords, tokens, credentials, full configuration dump |
| Propagation | Uncaught — propagates through `ensureDefaultTestUserExists()` and `beforeScenario()`, aborting scenario setup |
| Catch and suppress | Not possible — no try/catch around the guard call in `ensureDefaultTestUserExists()` |

Example message:
```
Automatic test-data creation is disabled in the production environment.
Detected environment: 'prod'. Blocked operation: default test user registration.
```

---

## 12. API Registration Protection

The guard call precedes `tryApiRegistration()` in the method body:

```java
config.guardAgainstProductionWrite("...");   // ← throws if prod
// ... (never reached in production)
boolean done = tryApiRegistration(baseUrl, testUsername, testPassword);
```

When the guard throws, `tryApiRegistration()` is never reached. The HTTP POST to
`ParaBankApiClient.registerUser()` is never initiated.

---

## 13. Browser Registration Fallback Protection

The browser fallback is only reachable after `tryApiRegistration()` returns `false`:

```java
boolean done = tryApiRegistration(...);    // ← never reached
if (!done) {
    tryBrowserRegistration(...);           // ← never reached
}
```

Since `tryApiRegistration()` is never reached, `tryBrowserRegistration()` is also never
reached. The browser fallback is blocked by the same guard that blocks the API path.

---

## 14. Non-Production Behaviour Preservation

For all non-production environments (`qa`, `staging`, `uat`, and default):
- `guardAgainstProductionWrite()` returns without throwing
- `tryApiRegistration()` executes as before
- `tryBrowserRegistration()` executes as before (if API fails)
- No change to API client, browser page objects, timeouts, or assertions
- No change to test-data values, credentials, or registration field values
- No change to scenario execution order or parallelism

---

## 15. Focused Tests Added

**File**: `src/test/java/com/parabank/parasoft/config/ProductionSafetyGuardTest.java`

**Package**: `com.parabank.parasoft.config` — not in Cucumber glue paths; no Cucumber
annotations; no browser or external network contact.

**Test framework**: TestNG (already on classpath; no new dependency added).

**Test isolation**: Each method uses `@BeforeMethod`/`@AfterMethod` to save, set, and
restore the `env` system property, with `ConfigManager.resetInstance()` before and after.

| Test method | What is verified |
|-------------|-----------------|
| `isProductionEnvironment_prod_returnsTrue` | `"prod"` → production |
| `isProductionEnvironment_production_returnsTrue` | `"production"` → production |
| `isProductionEnvironment_PROD_uppercase_returnsTrue` | `"PROD"` → production (case-insensitive) |
| `isProductionEnvironment_Production_mixedCase_returnsTrue` | `"Production"` → production |
| `isProductionEnvironment_qa_returnsFalse` | `"qa"` → not production |
| `isProductionEnvironment_staging_returnsFalse` | `"staging"` → not production |
| `isProductionEnvironment_test_returnsFalse` | `"test"` → not production |
| `guard_prod_throwsConfigurationException` | Guard throws for "prod"; message contains "production environment", env name, blocked operation |
| `guard_production_throwsConfigurationException` | Guard throws for "production" alias |
| `guard_qa_doesNotThrow` | Guard does not throw for "qa" — write path is allowed |
| `guard_staging_doesNotThrow` | Guard does not throw for "staging" |
| `guard_defaultEnvironment_doesNotThrow` | Guard does not throw when no `env` property is set (defaults to "qa") |

**API/browser-fallback protection verification**: Confirmed structurally — `tryApiRegistration()`
and `tryBrowserRegistration()` are unreachable after the guard throws, as demonstrated by
the method body ordering. No mocking library was available, so downstream call prevention
is verified through code structure rather than invocation-count assertions.

---

## 16. Focused-Test Results

Command: `./gradlew clean test --tests=com.parabank.parasoft.config.ProductionSafetyGuardTest`

| Tests executed | Tests passed | Tests failed |
|---------------|-------------|--------------|
| 12 | 12 | 0 |

All production aliases, non-production environments, exception messages, and guard
no-throw paths verified.

---

## 17. Invalid/Missing Environment Observations

| Scenario | Current behaviour | Risk |
|----------|------------------|------|
| Missing `env` property | Defaults to `"qa"` (safe) | None |
| Blank `env` (`-Denv=`) | Lowercased `""` → no properties file match → falls back to `config.properties` | Low — no production write; load may warn |
| `"uat"` | `uat.properties` loaded; `isProductionEnvironment()` returns false | None |
| Unknown value (e.g., `"local"`) | Falls back to `config.properties` or throws `ConfigurationException`; not production | None |
| `"prod"` + URL not matching production URL | Guard fires on env name alone — URL is not consulted | Acceptable — guard is conservative |

No validation redesign was performed. The existing `ConfigurationException` on missing
config file is sufficient for unknown environment values.

---

## 18. Full Regression Result

Command: `./gradlew clean test`

| Metric                        | Baseline | Phase 4 result | Difference |
|-------------------------------|--------:|---------------:|-----------:|
| Cucumber executions           |      18 |             18 |          0 |
| Passed scenarios              |      12 |             12 |          0 |
| Failed scenarios              |       6 |              6 |          0 |
| Known AUT failures            |       6 |              6 |          0 |
| Unexpected scenario failures  |       0 |              0 |          0 |
| Focused safety tests executed |       0 |             12 |        +12 |
| Focused safety tests failed   |       0 |              0 |          0 |
| **Total Gradle tests**        |      18 |         **30** |        +12 |

The Gradle total increased from 18 to 30 because the 12 new focused unit tests run
alongside the 18 Cucumber scenarios in the same `test` task.

---

## 19. Known-Failure Confirmation

All 6 Cucumber failures match the historical AUT limitation categories:

**Login — security/injection AUT failures (3):**

| Scenario | Error | Category |
|----------|-------|----------|
| Injection — SQL DROP TABLE | `AssertionError: p.error absent` | Known AUT behaviour |
| Injection — OR 1=1 | `AssertionError: p.error absent` | Known AUT behaviour |
| Injection — XSS script | `AssertionError: p.error absent` | Known AUT behaviour |

**Registration — redirect/session AUT failures (3):**

| Scenario | Error | Category |
|----------|-------|----------|
| Personal information | `WaitException: a[href='logout.htm'] absent` | Known AUT behaviour |
| Freshly generated profile | `WaitException: a[href='logout.htm'] absent` | Known AUT behaviour |
| External source data | `WaitException: a[href='logout.htm'] absent` | Known AUT behaviour |

---

## 20. Exact Phase 4 Files Changed

### Modified

| File | Change |
|------|--------|
| `src/test/java/com/parabank/parasoft/config/ConfigManager.java` | `isProductionEnvironment()` expanded to match "production" alias; `guardAgainstProductionWrite(String)` method added |
| `src/test/java/com/parabank/parasoft/hooks/Hooks.java` | `config.guardAgainstProductionWrite(...)` call added inside `ensureDefaultTestUserExists()` synchronized block |

### Created

| File | Purpose |
|------|---------|
| `src/test/java/com/parabank/parasoft/config/ProductionSafetyGuardTest.java` | 12 focused unit tests for production detection and guard behaviour |
| `docs/review/PHASE_4_PRODUCTION_SAFETY_GUARD.md` | This document |

### Not changed

| Category | Status |
|----------|--------|
| Feature files | Unchanged |
| Step definitions | Unchanged |
| Page objects and locators | Unchanged |
| `build.gradle` / dependencies | Unchanged |
| Docker, Grid, CI | Unchanged |
| Reporting, logging | Unchanged |
| Waits, assertions, retries | Unchanged |
| All other constants classes | Unchanged |

### Removed (locally, not committed)

| Path | Decision |
|------|----------|
| `docs/review/evidence/` | Removed — disposable generated investigation artifacts; findings already in committed Markdown |

---

## 21. Security Considerations

- The guard exception message does not include passwords, tokens, or configuration values.
- The only information disclosed is the detected environment name and blocked operation name.
- `prod.properties` credentials are blank (`username=`, `password=`) — CI/CD secrets are
  the expected credential source for production; this is unchanged.
- No new environment variable, secret, or credential was introduced in Phase 4.

---

## 22. Risks and Unresolved Observations

1. **`"production"` has no dedicated properties file**: When `-Denv=production` is passed,
   `production.properties` does not exist, so ConfigManager falls back to `config.properties`.
   The guard still fires correctly (detection is by name, not by file existence). However,
   a future run might accidentally succeed if `config.properties` has safe defaults.
   Adding `production.properties` as a redirect to `prod.properties` is a future hardening
   option, not in Phase 4 scope.

2. **`DEFAULT_USER_SETUP_DONE` flag**: Once `ensureDefaultTestUserExists()` throws in a
   hypothetical production run, `DEFAULT_USER_SETUP_DONE` remains `false`. If another
   thread enters `beforeScenario()`, it would re-enter the synchronized block and throw
   again. This is correct fail-fast behaviour — all scenarios abort with the same guard
   exception. No data corruption risk.

3. **ConfigManager singleton state in focused tests**: Tests use `resetInstance()` +
   `@BeforeMethod`/`@AfterMethod` for isolation. If Gradle runs focused tests and Cucumber
   tests in the same JVM, there is a narrow window where singleton state could be observed
   across class boundaries. In practice, TestNG runs `ProductionSafetyGuardTest` before
   `TestRunner` (alphabetical order within the same JVM), and teardown restores clean state.

---

## 23. Final Recommendation

> **PHASE 4 COMPLETE — READY FOR DEAD-CODE CLEANUP**

### Guard behaviour summary

| Environment   | Expected write behaviour     | Verified result              |
|---------------|------------------------------|------------------------------|
| `prod`        | Blocked                      | ✓ `ConfigurationException` thrown |
| `production`  | Blocked                      | ✓ `ConfigurationException` thrown |
| `PROD`        | Blocked                      | ✓ Case-insensitive match confirmed |
| `Production`  | Blocked                      | ✓ Case-insensitive match confirmed |
| `qa`          | Allowed                      | ✓ No exception; full suite 18/12/6 |
| `staging`     | Allowed                      | ✓ No exception                |
| Default/local | Existing behaviour preserved | ✓ No exception; default is "qa" |

### Write-path protection summary

| Write path                    | Guarded before invocation | Fallback prevented | Evidence |
|-------------------------------|--------------------------|-------------------|---------|
| API registration (`ParaBankApiClient.registerUser()`) | Yes — guard throws before `tryApiRegistration()` is called | N/A (same guard) | Method body ordering in `ensureDefaultTestUserExists()`; `guard_prod_throwsConfigurationException` test |
| Browser registration fallback (`tryBrowserRegistration()`) | Yes — guard throws before `tryApiRegistration()` is reached; fallback only reachable after API call returns | Yes — guard fires before both branches | Method body ordering; same guard call protects both paths |
