# ParaBank BDD Automation Framework

A production-ready BDD automation framework for testing the ParaBank web application using Cucumber, Selenium 4, and TestNG.

## Stack

- **Java 17**
- **Selenium 4.40.0** - Web browser automation
- **Cucumber 7.34.2** - BDD test execution
- **TestNG 7.12.0** - Test framework
- **Gradle 9.0.0** - Build tool
- **ExtentReports** - Test reporting
- **Allure** - Test reporting & trends
- **WebDriverManager** - Automatic browser driver management
- **SLF4J + Logback** - Logging

## Project Structure

```
src/test/
├── java/com/parabank/parasoft/
│   ├── config/              # Configuration management
│   ├── constants/           # Framework constants
│   ├── driver/              # WebDriver management
│   ├── exceptions/          # Custom exceptions
│   ├── factory/             # DriverFactory
│   ├── hooks/               # Cucumber lifecycle hooks
│   ├── pages/               # Page objects
│   ├── runner/              # Test runner
│   ├── stepdefinitions/     # BDD step definitions
│   ├── utils/               # Utilities (wait, actions, js, screenshots)
│   └── util/                # Legacy utilities
│
└── resources/
    ├── config/              # config.properties
    ├── features/            # Cucumber feature files
    └── extent-config.xml    # ExtentReports config
```

## Quick Start

### Prerequisites

- Java 17+
- Gradle 7.0+

### Run Tests

```bash
# Run all tests
./gradlew test

# Run smoke tests only
./gradlew testSmoke

# Run regression tests
./gradlew testRegression

# Run with specific browser
./gradlew test -Dbrowser=firefox

# Run in headless mode
./gradlew test -Dheadless=true

# Run with specific environment
./gradlew test -Denv=uat

# Run with parallel execution
./gradlew test -DmaxParallelForks=3
```

## Configuration

Configuration is centralized in `ConfigManager`. Properties are loaded from `src/test/resources/config/<env>.properties`.

Core property names (must be present in each environment file):
- baseUrl
- browserName
- headless
- waitTime
- implicitWait
- pageLoadTimeout
- username
- password

Example `config/qa.properties` (minimal):

```properties
baseUrl=https://parabank.parasoft.com/parabank/
browserName=chrome
headless=false
waitTime=30
implicitWait=20
pageLoadTimeout=45
username=sqa
password=sqa
```

Config access from code:

```java
// Generic access
String base = ConfigManager.getInstance().get("baseUrl");
boolean headless = ConfigManager.getInstance().getBoolean("headless");
int wait = ConfigManager.getInstance().getInt("waitTime");

// Username/Password are read from environment variables first (TEST_USERNAME / TEST_PASSWORD),
// falling back to properties when env vars are absent:
String username = ConfigManager.getInstance().getUsername();
String password = ConfigManager.getInstance().getPassword();
```

### Environment Configuration

Create environment-specific configs:
- `qa.properties`
- `uat.properties`
- `staging.properties`

Run with an environment:

```bash
./gradlew test -Denv=qa
```

## Architecture

### Page Object Model

Page classes extend `BasePage` and expose fluent methods:

```java
public class LoginPage extends BasePage {
    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By LOGIN_BUTTON = By.id("submit");

    public LoginPage fillUsername(String username) {
        sendKeys(USERNAME, username);
        return this;
    }

    public OverviewPage clickLogin() {
        click(LOGIN_BUTTON);
        return navigateToPage(OverviewPage.class);
    }
}
```

### Thread-Safe Driver Management

WebDriver is stored in ThreadLocal for parallel execution:

```java
// Initialize before test
DriverManager.initializeDriver();

// Get current driver
WebDriver driver = DriverManager.getDriver();

// Quit after test
DriverManager.quitDriver();
```

### Explicit Waits

No Thread.sleep(). Use WaitUtils instead:

```java
// Wait for element to be visible
WaitUtils.waitForElementVisibility(driver, locator);

// Wait for element to be clickable  
WaitUtils.waitForElementClickable(driver, locator);

// Wait with custom timeout
WaitUtils.waitForElementPresence(driver, locator, 60);
```

### Step Definitions

Gherkin steps are mapped to Java methods:

```gherkin
Feature: User Login
  Scenario: User logs in successfully
    Given User is on Login Page
    When User enters username "sqa" and password "sqa"
    And User clicks login button
    Then User should see overview page
```

```java
@Given("User is on Login Page")
public void user_is_on_login_page() {
    driver = DriverManager.getDriver();
    loginPage = new LoginPage(driver).navigateToPage(LoginPage.class);
}

@When("User enters username {string} and password {string}")
public void user_enters_credentials(String username, String password) {
    loginPage.fillUsername(username)
             .fillPassword(password);
}

@And("User clicks login button")
public void user_clicks_login_button() {
    overviewPage = loginPage.clickLogin();
}

@Then("User should see overview page")
public void user_should_see_overview_page() {
    Assert.assertTrue(overviewPage.hasLogOutLink());
}
```

## Reports

### ExtentReports

```
build/reports/extent/Report.html
```

### Allure Reports

```
build/allure-report/index.html
```

## Hooks

Cucumber hooks handle setup and teardown:

```java
@Before
public void beforeScenario() {
    // Initialize WebDriver before each scenario
    DriverManager.initializeDriver();
}

@After
public void afterScenario(Scenario scenario) {
    if (scenario.isFailed()) {
        // Capture screenshot on failure
        ScreenshotUtils.captureScreenshot(driver, scenario.getName());
    }
    // Quit WebDriver
    DriverManager.quitDriver();
}
```

## Utilities

### WaitUtils

```java
WaitUtils.waitForElementPresence(driver, locator);
WaitUtils.waitForElementVisibility(driver, locator);
WaitUtils.waitForElementClickable(driver, locator);
WaitUtils.waitForElementInvisibility(driver, locator);
```

### ElementActions

```java
ElementActions.click(driver, locator);
ElementActions.sendKeys(driver, locator, "text");
ElementActions.getText(driver, locator);
ElementActions.isDisplayed(driver, locator);
```

### JSUtils

```java
JSUtils.executeScript(driver, "return document.title");
JSUtils.scrollToElement(driver, element);
JSUtils.clickElementByJS(driver, element);
```

### ScreenshotUtils

```java
ScreenshotUtils.captureScreenshot(driver, "test-name");
```

## Parallel Execution

Tests run in parallel by default (2 threads):

```bash
# Run with 4 threads
./gradlew test -DmaxParallelForks=4
```

Each thread gets its own WebDriver instance via ThreadLocal.

## Logging

Logs go to console and `build/logs/test.log`:

```java
logger.info("Action: {}", message);
logger.warn("Warning: {}", message);
logger.error("Error: {}", message, exception);
```

## Troubleshooting

### Build Fails

```bash
# Clear cache
./gradlew clean --refresh-dependencies

# Check Java version
java -version  # Should be 17+
```

### Tests Timeout

```bash
# Increase wait time
./gradlew test -DwaitTime=60
```

### Browser Issues

```bash
# Run in headless mode
./gradlew test -Dheadless=true

# Use Firefox
./gradlew test -Dbrowser=firefox
```

### Memory Issues

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew test
```

## Best Practices

1. **Use Explicit Waits**: Never use Thread.sleep()
2. **Private Locators**: Keep By selectors private in page classes
3. **Fluent API**: Chain page methods for readability
4. **Clear Names**: Use descriptive method names
5. **One Assertion per Test**: Keep scenarios focused
6. **Page Objects**: Encapsulate UI interactions
7. **Logging**: Add meaningful log statements
8. **Error Messages**: Use clear assertion error messages

---

**Framework Version**: 1.0.0  
**Last Updated**: May 15, 2026

