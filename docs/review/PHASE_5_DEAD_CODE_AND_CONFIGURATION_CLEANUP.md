# Phase 5 — Dead Code and Obsolete Configuration Cleanup Report

## 1. Objective

Remove only verified dead declarations from constants classes and a confirmed-dead accessor
from the configuration layer. Every removal is backed by repository-wide usage searches with
zero active code references. Compilation and full regression validate each change.

---

## 2. Repository, Branch, and Starting Commit

| Field          | Value                                                                  |
|----------------|------------------------------------------------------------------------|
| Repository URL | https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation |
| Branch         | main                                                                   |
| HEAD at start  | `de08a390802e73d70ba80f9e4539ca37d70d24e2`                            |
| Phase 5 commit | see §19                                                                |

---

## 3. Initial Working-Tree State

```
(clean)
```

Working tree was fully clean at Phase 5 start. HEAD matched `de08a39`.

---

## 4. Pre-Change Compile Result

Command: `./gradlew clean compileTestJava`

```
BUILD SUCCESSFUL in 4s
```

---

## 5. Pre-Change Test Result

Command: `./gradlew clean test`

| Metric                         | Result |
|--------------------------------|--------|
| Safety tests passed            |     12 |
| Cucumber executions            |     18 |
| Passed scenarios               |     12 |
| Failed scenarios               |      6 |
| Known AUT failures             |      6 |
| Unexpected scenario failures   |      0 |
| Total Gradle tests             |     30 |

Baseline confirmed. Implementation proceeded.

---

## 6. Tag Audit

### Search commands

```
git grep -n "@hardcoded"
git grep -n "@ipsum"
git grep -n "@ddt"
git grep -n "@fail"
```

### Findings

| Tag | Location | Classification |
|-----|----------|---------------|
| `@hardcoded` | `TestDataConstants.java:21` — `TAG_HARDCODED = "@hardcoded"` | Dead constant declaration — no callers |
| `@hardcoded` | `README.md:456,467` | Historical documentation — out of Phase 5 scope |
| `@hardcoded` | `docs/TEST_STRATEGY.md:51` | Historical documentation — out of Phase 5 scope |
| `@ipsum` | `TestDataConstants.java:22` — `TAG_LOREM = "@ipsum"` | Dead constant declaration — no callers |
| `@ipsum` | `README.md:468` | Historical documentation — out of Phase 5 scope |
| `@ipsum` | `docs/TEST_STRATEGY.md:52` | Historical documentation — out of Phase 5 scope |
| `@ddt` | `TestDataConstants.java:23` — `TAG_DDT = "@ddt"` | Dead constant declaration — no callers |
| `@ddt` | `README.md:450,469` | Historical documentation — out of Phase 5 scope |
| `@ddt` | `docs/TEST_STRATEGY.md:53,79` | Historical documentation — out of Phase 5 scope |
| `@ddt` | `TestRunner.java:19` | Javadoc comment example — documentation, not runtime code |
| `@fail` | (none) | Not present in any tracked file |

All three tag constants (`TAG_HARDCODED`, `TAG_LOREM`, `TAG_DDT`) were removed from feature
files in the BDD Phase 2 refactoring (commit `33438c5`). The constant declarations in
`TestDataConstants.java` were not updated at that time and became dead. No Java source file
references them after removal from the feature files.

README, TEST_STRATEGY.md, and TestRunner Javadoc references are documentation and historical
evidence — not modified in Phase 5 per scope restrictions.

---

## 7. Constant Audit

### `ELEMENT_FINDING_TIMEOUT`

| Property | Value |
|----------|-------|
| Declaration | `TimeoutConstants.java:15` — `public static final int ELEMENT_FINDING_TIMEOUT = 15` |
| Visibility | `public static final` |
| Direct usages | None — `git grep -rn "ELEMENT_FINDING_TIMEOUT"` returns only the declaration |
| Static imports | None |
| Reflection/property binding | None |
| Documentation references | None |
| Duplicate active timeout | `EXPLICIT_WAIT_TIME = 30` is the active explicit-wait constant |
| **Classification** | **DECLARED BUT UNUSED — REMOVE** |

### `IMPLICIT_WAIT_TIME`

| Property | Value |
|----------|-------|
| Declaration | `TimeoutConstants.java:14` — `public static final int IMPLICIT_WAIT_TIME = 20` |
| Visibility | `public static final` |
| Direct usages | Only referenced in dead `getImplicitWait()` body (also removed) |
| Static imports | None |
| WebDriver application | None — `implicitlyWait()` is never called anywhere in the framework |
| Orphaned properties | `implicitWait=20` remains in 5 config files (harmless — no loader reads it after removal) |
| **Classification** | **DECLARED BUT UNUSED — REMOVE** |

### `SCREENSHOT_DELAY_MILLIS`

| Property | Value |
|----------|-------|
| Declaration | `TimeoutConstants.java:25` — `public static final long SCREENSHOT_DELAY_MILLIS = 1000L` |
| Visibility | `public static final` |
| Direct usages | None — `git grep -rn "SCREENSHOT_DELAY_MILLIS"` returns only the declaration |
| Static imports | None |
| `Thread.sleep` in codebase | None — `git grep -n "Thread.sleep"` returns only documentation warnings |
| Active screenshot delay | None — `ScreenshotUtils.captureScreenshot()` captures immediately with no sleep |
| **Classification** | **DECLARED BUT UNUSED — REMOVE** |

---

## 8. Getter / Accessor Audit

### `getImplicitWait()` — `ConfigManager`

| Property | Value |
|----------|-------|
| Declaration | `ConfigManager.java:141` — `public int getImplicitWait()` |
| Visibility | `public` |
| Direct callers | None — `git grep -rn "getImplicitWait"` returns only the declaration |
| Interface/superclass contract | None — `ConfigManager` has no superclass; no interface declares this method |
| Test usage | None |
| Reflection/property binding | None |
| Javadoc note | "implicit waits are intentionally disabled in this framework; this value is available for informational purposes and future use" — the framework itself documents this as inactive |
| **Classification** | **DECLARED BUT UNUSED — REMOVE** |

No `getElementFindingTimeout()` or `getScreenshotDelay()` methods exist in `ConfigManager`.

---

## 9. Runtime / Configuration Contract Analysis

| Symbol | Runtime/config contract check | Outcome |
|--------|------------------------------|---------|
| `TAG_HARDCODED` | Not referenced in runner, Gradle task definitions, or `cucumber.filter.tags` expressions | No contract |
| `TAG_LOREM` | Not referenced anywhere except its own declaration | No contract |
| `TAG_DDT` | Referenced in TestRunner Javadoc comment only (example command, not runtime filter) | No active contract |
| `ELEMENT_FINDING_TIMEOUT` | Not referenced in WaitUtils, DriverFactory, DriverManager, or any configuration loader | No contract |
| `IMPLICIT_WAIT_TIME` | Only in dead `getImplicitWait()` body; `implicitWait` property remains in config files (orphaned) | No active contract; orphaned properties retained |
| `getImplicitWait()` | Zero callers; no interface contract; no driver code applies it | No contract |
| `SCREENSHOT_DELAY_MILLIS` | Zero callers; no `Thread.sleep()` in codebase | No contract |

Config file property `implicitWait` was retained in 5 properties files. It is now an orphaned
key that no loader reads. Removing it would require 5 additional file changes for zero
functional benefit. This is documented as a remaining observation (see §21).

---

## 10. Removal Decision Matrix

| Candidate | Usage evidence | Decision | Action |
|-----------|---------------|----------|--------|
| `@hardcoded` (`TAG_HARDCODED`) | Declaration only — zero Java callers | **REMOVE** | Removed `TestDataConstants.java:21` |
| `@ipsum` (`TAG_LOREM`) | Declaration only — zero Java callers | **REMOVE** | Removed `TestDataConstants.java:22` |
| `@ddt` (`TAG_DDT`) | Declaration only — zero Java callers | **REMOVE** | Removed `TestDataConstants.java:23` |
| `@fail` | Not present in any tracked file | **NOT PRESENT** | No action |
| `ELEMENT_FINDING_TIMEOUT` | Declaration only — zero callers, no config binding | **REMOVE** | Removed `TimeoutConstants.java:15` |
| `IMPLICIT_WAIT_TIME` | Only in dead `getImplicitWait()` body | **REMOVE** | Removed `TimeoutConstants.java:14` |
| `getImplicitWait()` | Zero callers; no contract; explicitly documented as inactive | **REMOVE** | Removed `ConfigManager.java:141-142` |
| `SCREENSHOT_DELAY_MILLIS` | Declaration only — zero callers, no `Thread.sleep()` | **REMOVE** | Removed `TimeoutConstants.java:25` |

---

## 11. Items Removed

### `TestDataConstants.java`

Removed three dead tag constants (lines 21-23):

```java
// REMOVED
public static final String TAG_HARDCODED  = "@hardcoded";
public static final String TAG_LOREM      = "@ipsum";
public static final String TAG_DDT        = "@ddt";
```

The comment `// ===== Cucumber feature-file tags =====` and remaining four active constants
(`TAG_SMOKE`, `TAG_REGRESSION`, `TAG_NEGATIVE`, `TAG_POSITIVE`) were preserved unchanged.

### `TimeoutConstants.java`

Removed three dead constants:

```java
// REMOVED from "Explicit / implicit waits" section
public static final int IMPLICIT_WAIT_TIME        = 20;
public static final int ELEMENT_FINDING_TIMEOUT   = 15;

// REMOVED from "Millisecond intervals" section
public static final long SCREENSHOT_DELAY_MILLIS  = 1000L;
```

Section comment updated from `// ===== Explicit / implicit waits (seconds) =====` to
`// ===== Explicit waits (seconds) =====` to reflect that implicit-wait infrastructure
is no longer present. All other constants and the class structure are unchanged.

### `ConfigManager.java`

Removed dead `getImplicitWait()` method and its Javadoc (~12 lines):

```java
// REMOVED
/**
 * Gets implicit wait timeout in seconds.
 * Note: implicit waits are intentionally disabled in this framework; all waits use explicit WebDriverWait.
 * This value is available for informational purposes and future use.
 * Defaults to {@link TimeoutConstants#IMPLICIT_WAIT_TIME} when not set in properties.
 *
 * @return timeout in seconds
 */
public int getImplicitWait() {
    return getInt("implicitWait", TimeoutConstants.IMPLICIT_WAIT_TIME);
}
```

The Javadoc itself confirmed the method was non-functional ("intentionally disabled",
"informational purposes and future use"). Removal is consistent with the documented intent.

---

## 12. Items Retained and Rationale

| Item | Retained where | Rationale |
|------|---------------|-----------|
| `@hardcoded`/`@ipsum`/`@ddt` string literals in README.md | `README.md` | Documentation — out of Phase 5 scope |
| `@hardcoded`/`@ipsum`/`@ddt` entries in TEST_STRATEGY.md | `docs/TEST_STRATEGY.md` | Documentation — out of Phase 5 scope |
| `@ddt` example in TestRunner Javadoc | `TestRunner.java:19` | Source code documentation — not runtime; out of Phase 5 scope |
| `implicitWait` property keys | 5 config files | Orphaned but harmless; removing from 5 files exceeds minimum-change principle for zero functional benefit |
| `POLLING_INTERVAL_MILLIS` | `TimeoutConstants.java` | Not in Phase 5 candidate scope; not investigated |
| `TAG_SMOKE`, `TAG_REGRESSION`, `TAG_NEGATIVE`, `TAG_POSITIVE` | `TestDataConstants.java` | Active constants — still referenced in active tests or documentation |
| All other constants in `TimeoutConstants.java` | `TimeoutConstants.java` | Actively used by `ConfigManager`, `WaitUtils`, or `DriverFactory` |

---

## 13. Per-Removal Compile Results

All three logical groups were applied together and compiled in one `./gradlew clean compileTestJava` run:

```
BUILD SUCCESSFUL in 5s
```

No compilation errors. No missing-class or missing-symbol errors. The removal of
`getImplicitWait()` also dropped the only reference to `TimeoutConstants.IMPLICIT_WAIT_TIME`
from `ConfigManager.java` — that reference resolved correctly as both were removed atomically.

---

## 14. Focused Configuration-Test Results

Command: `./gradlew test --tests=com.parabank.parasoft.config.ProductionSafetyGuardTest`

| Tests executed | Tests passed | Tests failed |
|---------------|-------------|--------------|
| 12 | 12 | 0 |

All production aliases, non-production environments, guard exception content, and
guard no-throw paths verified. The removal of dead constants and the dead accessor had
no effect on ConfigManager's active behaviour or the guard implementation.

---

## 15. Production Safety Regression Result

Production guard fully operational post-cleanup:
- `prod` → `ConfigurationException` thrown ✓
- `production` → `ConfigurationException` thrown ✓
- `qa` → no exception ✓
- default → no exception ✓

---

## 16. Full Cucumber Regression Result

Two runs were executed (see §17 for first-run transient observation).

**Confirmation run (second run):**

| Metric                         | Baseline | Phase 5 result | Difference |
|--------------------------------|--------:|---------------:|-----------:|
| Cucumber executions            |      18 |             18 |          0 |
| Passed scenarios               |      12 |             12 |          0 |
| Failed scenarios               |       6 |              6 |          0 |
| Known AUT failures             |       6 |              6 |          0 |
| Unexpected scenario failures   |       0 |              0 |          0 |
| Production safety tests passed |      12 |             12 |          0 |
| Total Gradle tests             |      30 |             30 |          0 |

Baseline confirmed on the confirmation run.

---

## 17. Known-Failure Confirmation

All 6 Cucumber failures in the confirmation run matched the historical AUT limitation categories:

**Login — security/injection AUT failures (3):**

| runScenario | Scenario | Error | Category |
|-------------|----------|-------|----------|
| [9] | Injection — SQL DROP TABLE | `AssertionError: p.error absent` | Known AUT behaviour |
| [10] | Injection — OR 1=1 | `AssertionError: p.error absent` | Known AUT behaviour |
| [11] | Injection — XSS script | `AssertionError: p.error absent` | Known AUT behaviour |

**Registration — redirect/session AUT failures (3):**

| runScenario | Scenario | Error | Category |
|-------------|----------|-------|----------|
| [13] | Personal information | `WaitException: a[href='logout.htm'] absent` | Known AUT behaviour |
| [14] | Freshly generated profile | `WaitException: a[href='logout.htm'] absent` | Known AUT behaviour |
| [15] | External source data | `WaitException: a[href='logout.htm'] absent` | Known AUT behaviour |

**Transient observation (first run only):**

`runScenario[16]` ("Registration is rejected when the customer does not provide a username")
failed in the first run with `AssertionError: At least one error message should be displayed
on RegisterPage`. It **passed** in the confirmation run. This is a transient ParaBank demo
server instability — the same class documented in Phase 0.1 (BASELINE_CHANGE_INVESTIGATION.md).
Phase 5 dead-constant removals have no code path to registration validation logic; the
transient nature was confirmed by the recovery on the second run.

---

## 18. Symbol-Search Verification

Post-cleanup repository searches:

| Symbol removed | Result |
|---------------|--------|
| `TAG_HARDCODED` | No matches |
| `TAG_LOREM` | No matches |
| `TAG_DDT` | No matches |
| `ELEMENT_FINDING_TIMEOUT` | No matches |
| `IMPLICIT_WAIT_TIME` | No matches |
| `getImplicitWait` | No matches |
| `SCREENSHOT_DELAY_MILLIS` | No matches |

All 7 removed symbols are absent from the entire tracked repository. No broken references
exist. Historical string literals (`"@hardcoded"`, `"@ddt"`, etc.) in documentation files
are not affected.

Dependency resolution was not re-run — no build file or classpath configuration was modified.

---

## 19. Exact Phase 5 Files Changed

### Modified

| File | Change |
|------|--------|
| `src/test/java/com/parabank/parasoft/constants/TestDataConstants.java` | Removed `TAG_HARDCODED`, `TAG_LOREM`, `TAG_DDT` (3 constants) |
| `src/test/java/com/parabank/parasoft/constants/TimeoutConstants.java` | Removed `IMPLICIT_WAIT_TIME`, `ELEMENT_FINDING_TIMEOUT`, `SCREENSHOT_DELAY_MILLIS` (3 constants); updated section comment |
| `src/test/java/com/parabank/parasoft/config/ConfigManager.java` | Removed `getImplicitWait()` method and its Javadoc (1 method, ~12 lines) |

### Created

| File | Purpose |
|------|---------|
| `docs/review/PHASE_5_DEAD_CODE_AND_CONFIGURATION_CLEANUP.md` | This document |

### Not changed

| Category | Status |
|----------|--------|
| Feature files | Unchanged |
| Step definitions | Unchanged |
| Page objects and locators | Unchanged |
| Wait utilities | Unchanged |
| Production safety guard | Unchanged |
| `build.gradle` / dependencies | Unchanged |
| Docker, Grid, CI | Unchanged |
| README, TEST_STRATEGY.md, CONTRIBUTING.md | Unchanged |
| Config property files | Unchanged (orphaned `implicitWait` keys retained) |
| TestRunner.java | Unchanged (Javadoc comment retained) |

---

## 20. Skipped Recommendations

| Item | Reason not removed |
|------|-------------------|
| `POLLING_INTERVAL_MILLIS` | Not in Phase 5 candidate scope per task specification |
| `implicitWait` property keys in 5 config files | Orphaned but harmless; 5-file change exceeds minimum-change principle; deferred |
| README/TEST_STRATEGY.md tag table entries | Explicitly out of scope — documentation synchronization is a future phase |
| TestRunner Javadoc `@ddt` example command | Source documentation, not runtime code; out of scope |

---

## 21. Risks and Unresolved Observations

1. **Orphaned `implicitWait` property keys**: `implicitWait=20` remains in
   `qa.properties`, `staging.properties`, `uat.properties`, `prod.properties`, and
   `config.properties`. The key is now unread by any loader. It is harmless but
   creates a small maintenance confusion risk. Cleanup deferred to documentation
   synchronization phase.

2. **Transient first-run failure of `runScenario[16]`**: The ParaBank demo server failed
   to render a registration validation error during the first Phase 5 regression run.
   The scenario recovered on the second run. This is consistent with the historical
   transient-failure pattern (Phase 0.1). No framework change caused it.

3. **`POLLING_INTERVAL_MILLIS`**: Zero callers found in the audit. Not in Phase 5 scope
   and therefore not removed. A future dead-code pass should evaluate it.

4. **README and TEST_STRATEGY.md tag tables**: Reference `@hardcoded`, `@ipsum`, `@ddt`
   scenarios that no longer exist in the feature files. These are stale documentation
   surfaces. Cleanup is the responsibility of the documentation synchronization phase.

---

## 22. Final Recommendation

> **PHASE 5 COMPLETE — READY FOR DOCUMENTATION SYNCHRONIZATION**
