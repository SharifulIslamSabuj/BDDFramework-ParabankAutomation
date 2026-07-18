# Phase 3 — Low-Risk Code Improvements Report

## 1. Objective

Apply four pre-approved, low-risk code improvements to the framework source. No functional,
architectural, test-design, or configuration changes are permitted. No feature files, step
definitions, page-object logic, or test behaviour may be altered.

---

## 2. Repository

| Field          | Value                                                                  |
|----------------|------------------------------------------------------------------------|
| Repository URL | https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation |
| Branch         | main                                                                   |
| HEAD at start  | `1aef1a242551f5b44da2f7324c62af18742f063c`                            |
| Phase 3 commit | see §15                                                                |

---

## 3. Branch and Commit Hash

- Branch: `main`
- Starting commit: `1aef1a2 — build: remove verified unused dependencies`

---

## 4. Initial Working-Tree State

```
 M src/test/java/com/parabank/parasoft/stepdefinitions/LoginSteps.java
 M src/test/java/com/parabank/parasoft/stepdefinitions/RegisterSteps.java
 M src/test/resources/features/login.feature
 M src/test/resources/features/register.feature
```

The four pre-existing modified files are the BDD Phase 2 refactoring deliverables, outside
Phase 3 scope. They were not modified, staged, or reverted during this phase.

---

## 5. Approved Changes

| # | File | Change |
|---|------|--------|
| 1 | `ElementActions.java` | Eliminate redundant second DOM lookup in `isDisplayed()` |
| 2 | `LoginPage.java` | Replace XPath password-field locator with CSS selector |
| 3 | `FrameworkConstants.java` | Consolidate three identical blank-credential error constants into one |
| 4 | `ScreenshotUtils.java` | Modernise timestamp from `SimpleDateFormat`/`Date` to Java 17 `java.time` |

---

## 6. Change 1 — `ElementActions.isDisplayed()` Redundant DOM Lookup

### File

`src/test/java/com/parabank/parasoft/utils/ElementActions.java`

### Problem

The previous implementation called `driver.findElements(locator)` to test for element
presence, then immediately called `driver.findElement(locator)` — a second DOM round-trip
over the same WebDriver connection — to obtain the element for `isDisplayed()`. Under
parallel execution each call crosses a WebDriver wire; the second call is unnecessary because
`findElements()` already returns the list.

### Change

Added `import java.util.List;`.

```java
// BEFORE
WebElement element = driver.findElements(locator).isEmpty() ? null : driver.findElement(locator);
if (element == null) {
    return false;
}
boolean displayed = element.isDisplayed();

// AFTER
List<WebElement> elements = driver.findElements(locator);
if (elements.isEmpty()) {
    return false;
}
boolean displayed = elements.get(0).isDisplayed();
```

`elements.get(0)` returns the same DOM node that the previous `driver.findElement(locator)`
would have returned (Selenium's `findElement` is equivalent to `findElements().get(0)`).
Return value, exception handling, and logging are unchanged.

### Compile Verification

```
BUILD SUCCESSFUL in 3s
```

---

## 7. Change 2 — `LoginPage` Password-Field Locator

### File

`src/test/java/com/parabank/parasoft/pages/LoginPage.java`

### Problem

`PASSWORD_INPUT` was the only locator in the framework that used XPath. All other locators —
including the adjacent `USERNAME_INPUT` on the same page — used CSS selectors. Inconsistent
locator strategies without justification make future maintenance harder.

### Change

```java
// BEFORE
private static final By PASSWORD_INPUT = By.xpath("//input[@name='password']");

// AFTER
private static final By PASSWORD_INPUT = By.cssSelector("input[name='password']");
```

Semantic scope is identical: both expressions select `<input>` elements whose `name`
attribute equals `"password"`. The original XPath used no axes, text nodes, index
predicates, or compound conditions — there was no XPath capability that CSS could not express.

### Compile Verification

```
BUILD SUCCESSFUL in 2s
```

---

## 8. Change 3 — `FrameworkConstants` Blank-Credential Constants

### File

`src/test/java/com/parabank/parasoft/constants/FrameworkConstants.java`

### Problem

Three constants — `ERROR_EMPTY_USERNAME`, `ERROR_EMPTY_PASSWORD`, `ERROR_EMPTY_BOTH` —
held the identical value `"Please enter a username and password."` and represented identical
AUT behaviour: regardless of which field (or both) was left blank, the ParaBank demo server
returns the same single error message. Having three names for one string creates ambiguity
about which constant to reference at a new call site.

A `git grep` repository-wide search confirmed zero external callers of any of these three
constants, so no call sites needed updating.

### Change

```java
// BEFORE — three identical constants
public static final String ERROR_INVALID_CREDENTIALS    = "The username and password could not be verified.";
public static final String ERROR_EMPTY_USERNAME         = "Please enter a username and password.";
public static final String ERROR_EMPTY_PASSWORD         = "Please enter a username and password.";
public static final String ERROR_EMPTY_BOTH             = "Please enter a username and password.";
public static final String ERROR_USERNAME_LENGTH        = "The username and password could not be verified.";

// AFTER — one canonical constant
public static final String ERROR_INVALID_CREDENTIALS    = "The username and password could not be verified.";
public static final String ERROR_MISSING_CREDENTIALS    = "Please enter a username and password.";
public static final String ERROR_USERNAME_LENGTH        = "The username and password could not be verified.";
```

The six "could not be verified" constants were retained unchanged — they have different
business meanings (invalid credentials, username length violation, password length violation,
format violation, invalid password, locked account) despite sharing the same string value.

### Caller Verification

```
git grep -n "ERROR_EMPTY_USERNAME" → no matches
git grep -n "ERROR_EMPTY_PASSWORD" → no matches
git grep -n "ERROR_EMPTY_BOTH"     → no matches
```

No call sites existed. No external files were updated.

### Compile Verification

```
BUILD SUCCESSFUL in 5s
```

---

## 9. Change 4 — `ScreenshotUtils` Timestamp Modernisation

### File

`src/test/java/com/parabank/parasoft/utils/ScreenshotUtils.java`

### Problem

The screenshot filename timestamp was generated with `java.text.SimpleDateFormat` and
`java.util.Date` — legacy APIs superseded by `java.time` (JSR-310) in Java 8.
The framework targets Java 17; using the legacy date/time API in new code is inconsistent
with the platform version in use.

### Change

Imports removed:
```java
import java.text.SimpleDateFormat;
import java.util.Date;
```

Imports added:
```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
```

New static field added after `SCREENSHOT_EXTENSION`:
```java
private static final DateTimeFormatter SCREENSHOT_TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern(ReportConstants.TIMESTAMP_FORMAT);
```

In `captureScreenshot()`:
```java
// BEFORE
String timestamp = new SimpleDateFormat(ReportConstants.TIMESTAMP_FORMAT).format(new Date());

// AFTER
String timestamp = LocalDateTime.now().format(SCREENSHOT_TIMESTAMP_FORMAT);
```

Pattern `"yyyyMMdd_HHmmss"` (from `ReportConstants.TIMESTAMP_FORMAT`) is preserved exactly.
Filename structure, screenshot directory, and exception handling are unchanged.

`DateTimeFormatter` is immutable and thread-safe; declaring it as `private static final` is
correct and preferred over creating a new instance per call.

`LocalDateTime.now()` uses the system default timezone, the same effective timezone that the
legacy `new Date()` used — no behavioural change in timestamp values.

### Compile Verification

```
BUILD SUCCESSFUL in 2s
```

---

## 10. Diff Verification (Step 8)

Only the four Phase 3 source files were modified. No feature files, step definitions,
page-object logic, configuration, or build files were touched.

| File | Phase 3 changes |
|------|-----------------|
| `src/test/java/com/parabank/parasoft/utils/ElementActions.java` | Added `List` import; rewrote `isDisplayed()` body |
| `src/test/java/com/parabank/parasoft/pages/LoginPage.java` | Changed `PASSWORD_INPUT` locator strategy |
| `src/test/java/com/parabank/parasoft/constants/FrameworkConstants.java` | Replaced 3 blank-credential constants with 1 |
| `src/test/java/com/parabank/parasoft/utils/ScreenshotUtils.java` | Replaced legacy date/time with `java.time` |

Pre-existing files modified outside Phase 3 (not staged in this commit):

| File | Status |
|------|--------|
| `src/test/java/com/parabank/parasoft/stepdefinitions/LoginSteps.java` | Pre-existing BDD refactoring — not staged |
| `src/test/java/com/parabank/parasoft/stepdefinitions/RegisterSteps.java` | Pre-existing BDD refactoring — not staged |
| `src/test/resources/features/login.feature` | Pre-existing BDD refactoring — not staged |
| `src/test/resources/features/register.feature` | Pre-existing BDD refactoring — not staged |

---

## 11. Full Regression Result

Command: `./gradlew clean test`

| Metric              | Baseline | Phase 3 result | Difference |
|---------------------|--------:|---------------:|-----------:|
| Total executions    |      18 |             18 |          0 |
| Passed              |      12 |             12 |          0 |
| Failed              |       6 |              6 |          0 |
| Known AUT failures  |       6 |              6 |          0 |
| Unexpected failures |       0 |              0 |          0 |

Duration: 3 minutes 1 second.

---

## 12. Known Failure Confirmation

All 6 failures matched the known baseline categories:

**Login — security/injection AUT failures (3):**

| runScenario index | Scenario | Error | Category |
|-------------------|----------|-------|----------|
| [9] | Injection — SQL DROP TABLE in username | `AssertionError: p.error absent — got: ''` | Known AUT behaviour |
| [10] | Injection — OR 1=1 in password | `AssertionError: p.error absent — got: ''` | Known AUT behaviour |
| [11] | Injection — XSS script in username | `AssertionError: p.error absent — got: ''` | Known AUT behaviour |

**Registration — redirect/session AUT failures (3):**

| runScenario index | Scenario | Error | Category |
|-------------------|----------|-------|----------|
| [13] | Personal information | `WaitException: a[href='logout.htm'] absent after 30 s` | Known AUT behaviour |
| [14] | Freshly generated profile | `WaitException: a[href='logout.htm'] absent after 30 s` | Known AUT behaviour |
| [15] | External source data | `WaitException: a[href='logout.htm'] absent after 30 s` | Known AUT behaviour |

---

## 13. Unexpected Failures

**None.** All 6 failures are in the known AUT failure categories.

---

## 14. Files Changed During Phase 3

### Modified

| File | Change |
|------|--------|
| `src/test/java/com/parabank/parasoft/utils/ElementActions.java` | `isDisplayed()` single-lookup optimisation |
| `src/test/java/com/parabank/parasoft/pages/LoginPage.java` | Password locator XPath → CSS |
| `src/test/java/com/parabank/parasoft/constants/FrameworkConstants.java` | 3 blank-credential constants → 1 |
| `src/test/java/com/parabank/parasoft/utils/ScreenshotUtils.java` | `SimpleDateFormat`/`Date` → `java.time` |

### Created

| File | Purpose |
|------|---------|
| `docs/review/PHASE_3_LOW_RISK_CODE_IMPROVEMENTS.md` | This document |

### Not changed

| Category | Status |
|----------|--------|
| All feature files | Unchanged |
| All step definitions | Unchanged |
| All other page objects | Unchanged |
| All other constants classes | Unchanged |
| `build.gradle` | Unchanged |
| Configuration properties | Unchanged |
| Docker files | Unchanged |
| GitHub Actions | Unchanged |
| The four pre-existing BDD files | Unchanged — not staged |

---

## 15. Risks and Unresolved Observations

1. **`import java.util.List` placement:** The import was inserted after the Selenium imports
   and before the SLF4J imports in `ElementActions.java`. The file does not follow strict
   import grouping; the new import compiles correctly and is functionally identical regardless
   of ordering.

2. **Four pre-existing BDD files:** `login.feature`, `register.feature`, `LoginSteps.java`,
   and `RegisterSteps.java` remain as working-tree modifications, outside Phase 3 scope.
   They must be committed separately in a dedicated BDD refactoring commit.

---

## 16. Final Recommendation

> **PHASE 3 COMPLETE — BASELINE CONFIRMED 18/12/6**
