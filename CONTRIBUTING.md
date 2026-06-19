# Contributing

Contributions are welcome — bug fixes, new test scenarios, new page objects for untested features, and documentation improvements.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 17 or later |
| Google Chrome | Latest stable |
| Git | 2.x |

---

## Setup

```bash
git clone https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation.git
cd BDDFramework-ParabankAutomation

# Verify baseline (18 tests — 12 passed, 6 known failures)
./gradlew clean test
```

ChromeDriver is managed automatically by WebDriverManager. No manual download needed.

---

## How to Add a New Test Scenario

### 1. Write the Gherkin

Add a scenario to an existing `.feature` file in `src/test/resources/features/`, or create a new one:

```gherkin
@regression
Scenario: Open a new savings account
  Given User is on the overview page
  When User opens a new SAVINGS account
  Then The new account ID should be displayed
```

Tag selection: `@smoke` for critical happy paths, `@regression` for full coverage, `@negative` for invalid-input scenarios.

### 2. Write the step definitions

Add or extend a class in `src/test/java/.../stepdefinitions/`. Keep steps thin — delegate all UI interaction to the page object:

```java
@When("User opens a new SAVINGS account")
public void user_opens_new_savings_account() {
    openNewAccountPage = overviewPage.clickOpenNewAccountLink();
    openNewAccountPage.selectAccountType("SAVINGS")
                      .selectFirstAvailableAccount()
                      .clickOpenNewAccountButton();
}
```

### 3. Use or extend a page object

All page objects live in `src/test/java/.../pages/`. Five page objects (`OpenNewAccountPage`, `OpenedAccountPage`, `RequestLoanPage`, `ApprovedLoanPage`, `UpdateProfilePage`) are already implemented and await feature file coverage.

If you need a new page:

1. Extend `BasePage`
2. Declare all locators as `private static final By` — prefer `By.id()` when the element has an `id` attribute
3. Return `this` on action methods for fluent chaining
4. Return a new page via `createPage(TargetPage.class)` on navigation methods

### 4. Verify

```bash
./gradlew clean test

# Run only your new scenario by tag
./gradlew clean test -Dcucumber.filter.tags="@regression"
```

The baseline must remain 18 tests with the same 6 known failures.

---

## Code Standards

### Page objects

- Locators must be `private static final By`
- Prefer `By.id()` > `By.cssSelector()` > `By.xpath()` — in that precedence order
- Action methods return `this`; navigation methods return the target page via `createPage()`
- Never call `driver.findElement()` directly outside of `BasePage`

### Step definitions

- One Gherkin step → one Java method
- Assertions belong in `@Then` steps only — never in `@When`
- Page object fields must be `private`

### Waits

- Use `WaitUtils` or the `BasePage` protected wait helpers
- Never use `Thread.sleep()`
- Never configure implicit waits (they conflict with explicit waits)

### Security

- Never commit credentials to source — use `TEST_USERNAME` / `TEST_PASSWORD` environment variables
- Never log passwords — only usernames may appear in log output
- No real names, SSNs, or addresses in test data — use the `LoremIpsum` generator

---

## Commit Message Format

```
feat: add open savings account scenario
fix: correct RegisterPage error message locator
docs: update TEST_STRATEGY with account scenarios
refactor: replace hardcoded wait in LoginPage
```

---

## Before Submitting

- [ ] `./gradlew clean test` passes with the same 18-test, 6-failed baseline
- [ ] No `Thread.sleep()` introduced
- [ ] No credentials added to any source file
- [ ] New locators use `By.id()` where the element has an `id` attribute
- [ ] Step definitions delegate entirely to page object methods
