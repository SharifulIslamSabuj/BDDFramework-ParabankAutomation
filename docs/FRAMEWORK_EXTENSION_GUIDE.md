# Framework Extension Guide

**Framework:** ParaBank BDD Automation  
**Validated commit:** 01b054f  
**Last Updated:** 2026-07-18

This guide is for contributors who want to add features, scenarios, page objects, utilities,
or configuration to this repository. Read it before writing any code. Every rule here reflects
the current implementation — not a theoretical standard.

---

## Purpose

Provide implementation-level guidance for extending the framework correctly, safely, and
consistently with the established architecture. For submission requirements see
[PULL_REQUEST_CHECKLIST.md](PULL_REQUEST_CHECKLIST.md). For risk context see
[QUALITY_RISK_ASSESSMENT.md](QUALITY_RISK_ASSESSMENT.md).

---

## Architecture Boundaries

The framework has five verified layers. Every contribution must stay within the correct layer.

```
Feature files  (.feature)
  │  Business-readable scenarios, tags, examples
  │
  ▼
Step definitions  (stepdefinitions/)
  │  Map Gherkin steps to Java; delegate all browser work downward
  │  Assert outcomes; hold page-object state for the current scenario
  │
  ▼
Page objects  (pages/)
  │  Locators; browser actions; state queries; fluent API; navigation
  │
  ▼
ElementActions / WaitUtils / JSUtils  (utils/)
  │  Single-responsibility wrappers for Selenium API calls
  │
  ▼
WebDriver  (driver/)
     ThreadLocal storage; DriverFactory; local or RemoteWebDriver
```

### Supporting layers (horizontal — not part of the main flow)

| Layer | Package | Role |
|---|---|---|
| Hooks | `hooks/` | Scenario lifecycle; one-time test-user setup; screenshot on failure |
| ConfigManager | `config/` | Singleton; all property and env-var access |
| ParaBankApiClient | `utils/` | HTTP POST for test-user registration setup |
| ExcelDataProvider | `utils/` | Reads `ddt.xlsx`; supplies data to step definitions |
| Constants | `constants/` | Named literals; no logic |
| Exceptions | `exceptions/` | Typed hierarchy rooted at `FrameworkException` |

### Contributor boundary decisions

| You need to… | Correct location |
|---|---|
| Define a scenario | Feature file |
| Map a Gherkin step to Java | Step definition |
| Click, type, or read a page element | Page object via `ElementActions` |
| Wait for a page condition | `WaitUtils` wrapper (already used by page objects) |
| Assert a business outcome | Step definition `@Then` method |
| Generate unique test data | `LoremIpsum` or `ExcelDataProvider` (as already used) |
| Read a configuration property | `ConfigManager.getInstance()` |
| Make an HTTP request to ParaBank | `ParaBankApiClient` |
| Manage scenario setup or teardown | `Hooks` — discuss before adding |
| Define a WebDriver browser | `DriverFactory` — discuss before changing |
| Introduce a new type of exception | `exceptions/` extending `FrameworkException` |

---

## Adding a New Feature

"Feature" here means a Gherkin feature file covering a new area of the AUT.

Five page objects are already implemented and awaiting feature file coverage:

| Page object | AUT area |
|---|---|
| `OpenNewAccountPage` | Account opening |
| `OpenedAccountPage` | Account confirmation |
| `RequestLoanPage` | Loan application |
| `ApprovedLoanPage` | Loan approval |
| `UpdateProfilePage` | Profile update |

Steps:
1. Create `src/test/resources/features/<area>.feature`.
2. Choose tags from the active set (see [Adding or Changing Tags](#adding-or-changing-tags)).
3. Write scenarios in business language — no Java, no locator names, no CSS selectors.
4. Map steps to existing or new step-definition methods.
5. Reuse or extend an existing page object, or create a new one extending `BasePage`.
6. Run the focused feature: `./gradlew clean test -Dcucumber.filter.tags="@<your-tag>"`.
7. Run full regression: `./gradlew clean test`.
8. Confirm the expected count increased by exactly the number of new executions you added.

---

## Adding a Scenario

### Before writing Gherkin

1. Confirm no existing scenario already covers the same behaviour.
2. Decide which feature file owns this scenario — one feature = one cohesive area.
3. Choose Scenario (single case) versus Scenario Outline with Examples (multiple data variations).
4. Identify which tags apply. Every scenario must carry at least one classification tag.
5. Record the current execution count (`./gradlew clean test` reports it).

### Gherkin rules

```gherkin
# Good — business readable, no implementation detail
@regression @positive
Scenario: A registered customer can view their account balance
  Given a registered customer is signed in
  When the customer navigates to the account overview
  Then the customer should see at least one account balance

# Bad — technical detail in Gherkin
Scenario: Check balance
  Given user clicks element By.cssSelector(".account")
  When Thread.sleep(2000)
  Then assert text equals "1234"
```

Rules:
- Titles must be complete sentences describing what the customer does or sees.
- `Background` is appropriate when every scenario in the file shares the same starting state.
- `Scenario Outline` requires at least two `Examples` rows; a single row belongs in `Scenario`.
- No locator names, CSS selectors, XPath, class names, or Java identifiers in Gherkin.
- No timing assumptions — steps delegate to `WaitUtils`, which already handles waits.
- Examples column headers must be descriptive; avoid `col1`, `val`, `x`.

### Execution count awareness

Adding one `Scenario` adds one execution.  
Adding one `Scenario Outline` with N `Examples` rows adds N executions.  
The accepted baseline is **18 executions**. Any addition changes this count intentionally.  
Record the new expected count in the pull request description.

---

## Adding Step Definitions

### Step definition rules

```java
// Good — thin, delegates to page object
@When("the customer submits their login credentials")
public void the_customer_submits_their_login_credentials() {
    overviewPage = loginPage.clickLoginButton();
}

// Bad — locator in step definition
@When("the customer clicks {string}")
public void the_customer_clicks(String selector) {
    driver.findElement(By.cssSelector(selector)).click();  // NEVER
}
```

Rules:
- One Gherkin step → one Java method. Do not fan out to multiple operations.
- Delegate all browser interaction to the page object. Steps hold references to page objects but do not call Selenium directly.
- Do not obtain `WebDriver` directly in step classes — use `DriverManager.getDriver()` only when constructing a page object.
- Do not define `By` locators in step classes.
- Do not use `Thread.sleep()`.
- Assertions belong in `@Then` methods only. `@Given` and `@When` must not assert outcomes.
- Step expressions must be unambiguous. Run `./gradlew clean compileTestJava` to catch step conflicts at compile time; ambiguous steps cause a runtime `AmbiguousStepDefinitionException`.
- New business-language aliases for an existing step (as used in BDD Phase 2) must delegate to the existing method — never duplicate the implementation.
- Do not create a generic step like `"the user does {string}"` — steps must describe specific observable business actions.

### Where step-definition files live

```
src/test/java/com/parabank/parasoft/stepdefinitions/
├── LoginSteps.java      ← login feature steps
└── RegisterSteps.java   ← registration feature steps
```

Add a new file for each new feature area. Do not combine unrelated steps in one class.

---

## Extending a Page Object

### Page object rules

```java
// Good — encapsulated locator, fluent return, no WebDriver exposure
public class AccountOverviewPage extends BasePage {
    private static final By ACCOUNT_ROW = By.cssSelector("table#accountTable tbody tr");

    public AccountOverviewPage(WebDriver driver) { super(driver); }

    public boolean hasAtLeastOneAccount() {
        return getElementCount(ACCOUNT_ROW) > 0;
    }
}
```

Rules:
- Extend `BasePage`. Every page object gets `driver`, `configManager`, `logger`, and all `ElementActions`/`WaitUtils` wrappers for free.
- Locators are `private static final By` constants. They are never passed as method arguments or exposed outside the class.
- Use `By.id()` first, then `By.cssSelector()`, then `By.xpath()` — in that precedence order.
- Avoid absolute XPath (`/html/body/div[1]/...`). Avoid index-based selectors (`nth-child(3)`) where a stable attribute exists.
- Action methods (`fillUsername()`, `clickLoginButton()`) return `this` for fluent chaining.
- Navigation methods that move to a new page return the target page via `createPage(TargetPage.class)`.
- Do not expose raw `WebElement` objects from page methods — expose state as `boolean`, `String`, or `int`.
- Do not duplicate wait logic that `ElementActions` or `WaitUtils` already handles.
- Do not add implicit waits (`driver.manage().timeouts().implicitlyWait(...)`).
- Do not use `Thread.sleep()`.
- State-query methods (`hasLogOutLink()`, `hasErrorMessage()`) return a meaningful primitive or typed value.
- Do not perform assertions inside page objects. Page objects describe what the page contains; step definitions assert what it should contain.

### Creating a new page object

1. Create `<PageName>.java` in `src/test/java/com/parabank/parasoft/pages/`.
2. Extend `BasePage`.
3. Declare a constructor: `public <PageName>(WebDriver driver) { super(driver); }`.
4. Declare all locators as `private static final By` at the top of the class.
5. Implement actions that return `this` or the next page.
6. Implement state queries that return `boolean`, `String`, or `int`.

---

## Adding API Operations

The `ParaBankApiClient` currently handles test-user registration via HTTP POST. It is a
framework setup utility, not a test assertion utility.

Rules:
- Add new HTTP operations to `ParaBankApiClient` only when they support framework setup.
- Do not add assertion logic to the API client.
- Do not log sensitive values (passwords, SSNs, account numbers).
- Every new write operation in the API client must be preceded by a production-safety analysis.
- A new write method must be called from behind `ConfigManager.guardAgainstProductionWrite()`.
- Do not invoke raw `HttpClient` or `HttpRequest` from step definitions or page objects.
- Return semantics: `true` = available for use, `false` = error or unavailable — not `true` = HTTP 200.

---

## Adding Test Data

### Principles

- Do not hardcode real names, addresses, SSNs, or phone numbers.
- Do not hardcode test usernames or passwords in Java source files.
- Do not commit secrets or credentials to any source file.
- Do not commit the `.env` file.
- Passwords are never logged — follow the pattern in `ConfigManager.getPassword()`.

### Unique-data strategy

The current framework uses two approaches for generated data:
- `LoremIpsum.getInstance()` — generates first names, last names, addresses, phone numbers, zip codes.
- `System.currentTimeMillis()` appended to username — ensures uniqueness per run.

When adding a new registration or write scenario:
- Generate a unique username per execution (timestamp or UUID suffix).
- Do not depend on a specific username existing in the AUT.
- Do not assume a prior scenario created the data you need.

### Cleanup limitations

The public ParaBank demo has no account-deletion API. Data written during test runs accumulates.
Do not write test scenarios that assume a clean state or that prior test data was removed.

### Static test user (`sqa`)

The `sqa` user is provisioned by `Hooks.ensureDefaultTestUserExists()` via the API-first path
with a browser fallback. Do not add a duplicate setup elsewhere. Do not remove or bypass this
setup without a full suite impact analysis.

---

## Adding Configuration

### Configuration access

All configuration access must go through `ConfigManager.getInstance()`. Do not read system
properties or environment variables directly in step definitions, page objects, or hooks.

```java
// Good
String baseUrl = ConfigManager.getInstance().getBaseUrl();

// Bad
String baseUrl = System.getProperty("baseUrl");
```

### Adding a new property

1. Add the property to all applicable environment files in `src/test/resources/config/`.
2. Add a getter in `ConfigManager` following the existing pattern (`getInt`, `getBoolean`, `get`).
3. Add a sensible default that does not break existing runs.
4. Verify the system-property override chain works if the property should be overridable.
5. Do not add a property key without a corresponding consumer.

### Production safety

Any new configuration path that enables a write operation must:
1. Call `ConfigManager.guardAgainstProductionWrite(String operationDescription)` before the write.
2. Pass through the synchronized block in `Hooks.ensureDefaultTestUserExists()` or apply an equivalent guard.
3. Never assume the environment is safe for writes without an explicit check.

The production guard currently blocks `prod` and `production` (case-insensitive). Do not
weaken this by adding new environment aliases without a safety analysis.

### Orphaned property note

`implicitWait` exists in 5 config files but no code reads it. Do not re-add `getImplicitWait()`
or reference `implicitWait` in new code. Explicit waits only — see [Wait Standards](#wait-standards).

---

## Adding or Changing Tags

### Active tags (current feature files — commit 01b054f)

| Tag | Applied at | Purpose |
|---|---|---|
| `@regression` | Feature | Marks the full regression suite — applied at feature level |
| `@smoke` | Scenario | Single critical happy-path — runs fast sanity check |
| `@positive` | Scenario | Happy-path scenarios with expected successful outcome |
| `@negative` | Scenario | Invalid-input, boundary, or rejection scenarios |
| `@validation` | Scenario | Input-validation and business-rule scenarios |
| `@security` | Scenario | Injection-probe and scripting-attack scenarios |

### Adding a new tag

1. Confirm the tag has a clear suite classification purpose.
2. At least one scenario must carry the tag before the pull request is merged.
3. Add the tag to the active tag table in `docs/TEST_STRATEGY.md` and `README.md`.
4. Document the tag in the run-command examples if it enables a useful isolated execution.
5. Review `CONTRIBUTING.md` for tag references and update if affected.

### Do not restore obsolete tags

`@hardcoded`, `@ipsum`, `@ddt`, and `@fail` were removed in Phase 5. Do not reintroduce them.
If data-source differentiation is needed again, introduce a clearly named new tag and document it.

---

## Modifying Hooks

Hooks manage the entire scenario lifecycle. Modifications here affect every scenario execution.

Rules:
- `@Before`: initializes MDC, starts WebDriver, runs one-time test-user setup.
- `@After`: captures screenshot on failure, quits WebDriver, clears MDC.
- Do not add business logic to hooks.
- Do not add scenario-specific setup in a general `@Before` — use tag-scoped hooks (`@Before("@tagName")`) if a setup applies only to specific scenarios.
- The production write guard must remain inside the synchronized block, before both `tryApiRegistration()` and `tryBrowserRegistration()`. Do not move it.
- Any new shared static state in Hooks requires concurrency analysis — the suite runs in parallel by default.
- Teardown must always release the WebDriver even when setup or the scenario fails.
- New `@Before` or `@After` methods must carry an explicit `@Order` annotation if ordering relative to existing hooks matters.

---

## Locator Standards

| Preference | Example | Use when |
|---|---|---|
| `By.id()` | `By.id("loginButton")` | Element has a stable `id` attribute |
| `By.cssSelector()` with attribute | `By.cssSelector("input[name='username']")` | No id; stable attribute available |
| `By.cssSelector()` with class | `By.cssSelector("p.error")` | Semantic class exists |
| `By.linkText()` | `By.linkText("Log Out")` | Anchor with stable text |
| `By.xpath()` | `By.xpath("//a[@href='logout.htm']")` | No CSS-expressible relationship; used sparingly |
| Avoid | `By.xpath("/html/body/div[1]/p[3]")` | Never — absolute XPath is brittle |
| Avoid | `By.cssSelector("tr:nth-child(4) td")` | Only if no stable attribute alternative exists |

All locators are `private static final By` fields in page object classes.

---

## Wait Standards

The framework uses explicit waits exclusively. `WaitUtils` wraps `WebDriverWait`.

```java
// Good — explicit wait via established abstraction
WaitUtils.waitForElementToBeVisible(driver, LOCATOR);

// Bad — implicit wait
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));

// Bad — arbitrary sleep
Thread.sleep(2000);
```

Rules:
- Use `WaitUtils.waitForElementToBeVisible()`, `waitForElementToBeClickable()`, or `waitForElementToBePresent()`.
- `BasePage` exposes `waitForElementVisible()` and `waitForElementClickable()` — use these in page objects.
- `ElementActions` methods already call `WaitUtils` internally — do not add a second wait before calling `ElementActions`.
- Do not increase `waitTime` globally to work around slow AUT responses. If the AUT is consistently slow, investigate before changing timeouts.
- Do not reintroduce `getImplicitWait()` or any implicit-wait flow.
- Wait for a specific observable condition, not for a fixed time.

---

## Assertion Guidance

Assertions in this framework live in step definitions, specifically in `@Then` steps.

```java
// Good — assertion in @Then step
@Then("the customer is successfully signed in")
public void the_customer_is_successfully_signed_in() {
    Assert.assertNotNull(overviewPage, "User should be on OverviewPage after successful login");
    Assert.assertTrue(overviewPage.hasLogOutLink(), "Logout link should be visible");
}

// Bad — assertion hidden in page object action
public OverviewPage clickLoginButton() {
    click(LOGIN_BUTTON);
    Assert.assertTrue(hasLogOutLink());  // NEVER — page objects describe, not assert
    return createPage(OverviewPage.class);
}
```

Rules:
- `@Given` steps — no assertions; set up state only.
- `@When` steps — no assertions; perform actions only.
- `@Then` steps — assert the observable business outcome.
- Use TestNG `Assert` (already imported in step classes), not JUnit assertions.
- Assertions must describe what they verify: `Assert.assertTrue(condition, "meaningful message")`.
- `Assert.assertNotNull(pageObject, ...)` before accessing page state is correct — it guards against null from a failed navigation.

---

## Validation Workflow

Run these commands in order. Every step must pass before the next.

| Step | Command | Purpose |
|---|---|---|
| 1 | `./gradlew clean compileTestJava` | Confirm no compile errors |
| 2 | `./gradlew test --tests "*ProductionSafetyGuardTest"` | Confirm production guard unaffected |
| 3 | `./gradlew clean test -Dcucumber.filter.tags="@<your-tag>"` | Confirm new/changed scenario in isolation |
| 4 | `./gradlew clean test` | Full regression — 30 tests expected |

### Reading the full regression result

| Result | Classification |
|---|---|
| 30 tests / 12 safety passed / 18 Cucumber (12 passed, 6 failed) | **Baseline — no regression** |
| New scenario added: 30 + N tests / 12 + N Cucumber / baseline fails still 6 | **New scenario verified** |
| Existing passing scenario now fails | **Investigate before merging** |
| One additional failure, resolved on rerun | **Transient AUT instability — document both runs** |
| One additional failure, reproducible across two runs | **Block merge — investigate** |

The six known AUT failures (`runScenario[9,10,11,13,14,15]`) always appear.
See [docs/KNOWN_AUT_LIMITATIONS.md](KNOWN_AUT_LIMITATIONS.md) for their exact scenario titles.

---

## Baseline Interpretation

```
Accepted baseline:
  18 Cucumber executions
  12 passed
   6 known AUT failures
   0 unexpected failures
  12 production-safety tests (all passed)
  30 total Gradle tests
```

Gradle exits with `BUILD FAILED` because the 6 AUT-limitation scenarios do not pass.
This is the correct and expected exit status. A "green build" in the traditional sense is
not achievable while these scenarios remain active — which is intentional.

Do not interpret `BUILD FAILED` alone as evidence of a framework regression.

---

## Common Anti-Patterns

### 1. WebDriver calls in step definitions

```java
// WRONG
@When("the customer clicks login")
public void click_login() {
    DriverManager.getDriver().findElement(By.id("login")).click();
}
// RIGHT — delegate to page object
@When("the customer clicks login")
public void click_login() {
    overviewPage = loginPage.clickLoginButton();
}
```

### 2. Locators in Gherkin or step methods

Never name a CSS selector, ID, XPath, or class name in a feature file or step method signature. Locators belong in page objects.

### 3. `Thread.sleep()`

No call to `Thread.sleep()` exists anywhere in the framework. Do not add one. Use `WaitUtils`.

### 4. Increasing timeouts to hide instability

Raising `waitTime` from 30 to 120 seconds does not fix a slow AUT — it hides it. If a scenario reliably times out, investigate the AUT state, not the timeout value.

### 5. Hardcoded credentials in source

Any username or password in a `.java` or `.properties` file that is not the public `sqa/sqa` fallback is a security violation. Use `TEST_USERNAME`/`TEST_PASSWORD` environment variables.

### 6. Bypassing the production write guard

Calling `tryApiRegistration()` or `tryBrowserRegistration()` directly, or calling `ConfigManager.getInstance()` without the production guard before a write, is a safety regression. The guard must be called first.

### 7. Scenario-order dependence

Scenarios run in parallel. A scenario must not depend on another scenario having run first, having created data, or having left a specific browser state. Each scenario starts with `Hooks.@Before` initializing a fresh WebDriver.

### 8. Hiding failures with retries or skip markers

No retry mechanism exists in this framework. Do not add `@Retry` annotations or suppress failures with `@Disabled`. Known failures are documented in `KNOWN_AUT_LIMITATIONS.md`. Unknown failures must be investigated.

### 9. Duplicate step expressions

Two step methods with the same or overlapping expression pattern cause a runtime `AmbiguousStepDefinitionException`. Verify step uniqueness before adding new step definitions.

### 10. Adding dependencies without usage evidence

Do not add a library to `build.gradle` without at least one verified consumer in the test source. Unused dependencies increase build time and create maintenance debt.

### 11. Not updating documentation

Adding a new tag, a new execution count, a new configuration property, or a new architecture layer without updating `README.md` and `docs/TEST_STRATEGY.md` leaves the repository in an inconsistent state.

---

## Documentation Responsibilities

When adding code, update documentation before closing the pull request:

| Change | Documentation to update |
|---|---|
| New tag | README Tag Filtering table; TEST_STRATEGY tag table; CONTRIBUTING active-tag reference |
| New scenario(s) | README Quick Start expected count; TEST_STRATEGY baseline; CONTRIBUTING baseline |
| New page object | README Project Structure table; TEST_STRATEGY scope table |
| New configuration property | README Configuration section; TEST_STRATEGY if strategy changes |
| New production-write path | QUALITY_RISK_ASSESSMENT guard scope note |
| Architecture change | README Architecture sections; TEST_STRATEGY architecture layer |

Do not modify historical phase reports in `docs/review/`. They are point-in-time evidence records.
