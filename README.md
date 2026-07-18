# ParaBank BDD Automation Framework

[![CI](https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation/actions/workflows/automation-test.yml/badge.svg)](https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation/actions)
[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Selenium](https://img.shields.io/badge/Selenium-4.40.0-43B02A?logo=selenium&logoColor=white)](https://www.selenium.dev/)
[![Cucumber](https://img.shields.io/badge/Cucumber-7.34.2-23D96C?logo=cucumber&logoColor=white)](https://cucumber.io/)
[![Gradle](https://img.shields.io/badge/Gradle-9.0.0-02303A?logo=gradle&logoColor=white)](https://gradle.org/)

A professional BDD test automation framework for the [ParaBank](https://parabank.parasoft.com/parabank/) online banking application, built to demonstrate real-world automation engineering skills. The framework covers Cucumber BDD with parallel TestNG execution, full Selenium Grid support via Docker Compose, multi-environment configuration with secure credential handling, enterprise-grade reporting through ExtentReports and Allure, and a complete GitHub Actions CI/CD pipeline.

---

## Skills and Patterns Demonstrated

A quick reference for technical reviewers and hiring managers.

| Category | Demonstrated |
|---|---|
| **Test Design** | BDD / Gherkin, data-driven testing (Excel via Apache POI), tag-based suite management |
| **Page Object Model** | Fluent API, private locators, typed `createPage()` factory with reflection |
| **Parallel Execution** | `ThreadLocal<WebDriver>`, TestNG `@DataProvider(parallel=true)`, SLF4J MDC thread logging |
| **Wait Strategy** | Explicit waits only via `WaitUtils` and `WebDriverWait` — zero `Thread.sleep()` in the codebase |
| **API Integration** | Java 17 `HttpClient` for API-first test data setup (`ParaBankApiClient`) with browser fallback |
| **Infrastructure** | Docker (multi-stage Dockerfile + Compose), Selenium Grid 4 (Hub + Chrome Node + noVNC) |
| **CI/CD** | GitHub Actions pipeline — compile gate, safety gate, Xvfb regression, result classification (VALIDATED_BASELINE / UNEXPECTED_REGRESSION), artifact upload |
| **Configuration** | 4-level priority chain (JVM property → env var → properties file → default), 4 environments |
| **Security** | No credentials in source; env-var / GitHub Secrets integration; passwords never logged |
| **Exception Handling** | Typed custom hierarchy (`FrameworkException` → `WaitException`, `DriverInitializationException`, etc.) |
| **Reporting** | ExtentReports (interactive HTML), Allure (trend graphs), Cucumber HTML, TestNG — all generated per run |
| **Test Data** | `LoremIpsum` for generated data, Excel DDT, API registration — no PII in source code |

---

## Table of Contents

- [Skills and Patterns Demonstrated](#skills-and-patterns-demonstrated)
- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Execution Options](#execution-options)
- [Configuration](#configuration)
- [Tag Filtering](#tag-filtering)
- [Reporting](#reporting)
- [Architecture Details](#architecture-details)
- [Parallel Execution](#parallel-execution)
- [Logging](#logging)
- [Secret Management](#secret-management)
- [Selenium Grid](#selenium-grid)
- [Docker Support](#docker-support)
- [GitHub Actions CI](#github-actions-ci)
- [Troubleshooting](#troubleshooting)
- [Quality and Risk Documentation](#quality-and-risk-documentation)

---

## Architecture Overview

### Execution Flow

```
Feature Files (.feature)
        |
        v
TestRunner (TestNG + AbstractTestNGCucumberTests)
  - parallel DataProvider
  - tag filtering via -Dcucumber.filter.tags
        |
        +------ Hooks (@Before / @After)
        |         - MDC logging per scenario thread
        |         - WebDriver init and quit
        |         - One-time test data setup:
        |             ConfigManager production guard
        |             → API registration (ParaBankApiClient)
        |             → browser registration fallback
        |
        +------ Step Definitions (LoginSteps, RegisterSteps)
                        |
                        v
                Page Object Layer
                BasePage <-- LoginPage
                         <-- RegisterPage
                         <-- OverviewPage
                        |
               +--------+---------+
               |                  |
        ElementActions        WaitUtils / JSUtils
        click, type,          explicit waits only
        getText, clear        (no Thread.sleep)
               |
               v
        Driver Management
        DriverManager (ThreadLocal) <-- DriverFactory
               |
        +------+------------------------+
        |                               |
  Local Browser                  Selenium Grid
  Chrome / Firefox               RemoteWebDriver
  Edge / Safari                  docker-compose.grid.yml
```

### Component Responsibilities

| Component | Responsibility |
|---|---|
| `TestRunner` | Cucumber + TestNG integration; parallel DataProvider; runtime tag selection |
| `Hooks` | `@Before`: MDC setup, driver init, one-time test data; `@After`: screenshot on fail, driver quit |
| `Step Definitions` | Map Gherkin steps to Java; delegate all UI work to page objects |
| `BasePage` | Shared driver reference; fluent `createPage()` factory; delegates to ElementActions and WaitUtils |
| `Page Objects` | Locators + page-specific actions; expose fluent API to step definitions |
| `ElementActions` | Single-responsibility wrappers: click, sendKeys, getText, clear |
| `WaitUtils` | Explicit waits (WebDriverWait) for visibility, clickability, presence |
| `DriverFactory` | Creates local or RemoteWebDriver based on `seleniumGridEnabled` config |
| `DriverManager` | ThreadLocal WebDriver — one instance per test thread for parallel safety |
| `ConfigManager` | Singleton; loads `<env>.properties`; env-var and system-property overrides; production-write guard |
| `ExcelDataProvider` | Apache POI reader; `@DataProvider` for TestNG DDT scenarios |
| `ParaBankApiClient` | HTTP form POST for test user setup — faster than browser registration |
| `ScreenshotUtils` | Captures PNG on failure; attaches to Cucumber and Extent reports |
| Logback + MDC | Rolling log files; `scenarioName` MDC key traces each parallel thread |

---

## Technology Stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Gradle | 9.0.0 | Build and dependency management |
| Selenium WebDriver | 4.40.0 | Browser automation |
| Cucumber | 7.34.2 | BDD / Gherkin execution |
| TestNG | 7.12.0 | Test runner, parallel execution |
| WebDriverManager | 5.7.0 | Automatic browser driver setup (local only) |
| ExtentReports | 5.1.2 | Interactive HTML test reports |
| Allure | 2.32.0 | Test trends and history reporting |
| Apache POI | 5.2.3 | Excel data-driven testing |
| SLF4J + Logback | 2.0.13 / 1.5.6 | Structured logging with MDC |
| LoremIpsum | 2.2 | Generated test data (no PII in source) |

---

## Project Structure

```
src/test/
├── java/com/parabank/parasoft/
│   ├── config/
│   │   └── ConfigManager.java           # Singleton; env-aware; 4-level credential priority
│   ├── constants/
│   │   ├── BrowserConstants.java        # Chrome / Firefox / Edge / Safari identifiers
│   │   ├── TimeoutConstants.java        # Wait, poll, page-load, thread-pool values
│   │   ├── PathConstants.java           # All file-system paths and file names
│   │   ├── ReportConstants.java         # ExtentReports config, date formats, delimiters
│   │   ├── TestDataConstants.java       # Env names, Cucumber tags, default tag expression
│   │   └── FrameworkConstants.java      # ParaBank URLs, page titles, error strings
│   ├── driver/
│   │   └── DriverManager.java           # ThreadLocal WebDriver — parallel-safe
│   ├── exceptions/                      # FrameworkException hierarchy
│   ├── factory/
│   │   └── DriverFactory.java           # Local browser or RemoteWebDriver (Grid)
│   ├── hooks/
│   │   └── Hooks.java                   # @Before / @After; MDC; test data setup
│   ├── pages/
│   │   ├── BasePage.java                # Shared driver; createPage(); fluent helpers
│   │   ├── LoginPage.java               # Login flow; credential validation; fluent API
│   │   ├── RegisterPage.java            # Registration form; success/failure paths
│   │   ├── OverviewPage.java            # Post-login landing; session verification
│   │   ├── OpenNewAccountPage.java      # Account opening workflow (CHECKING / SAVINGS)
│   │   ├── OpenedAccountPage.java       # Post-creation confirmation; new account ID
│   │   ├── RequestLoanPage.java         # Loan application form; source account selection
│   │   ├── ApprovedLoanPage.java        # Loan approval confirmation; loan ID extraction
│   │   └── UpdateProfilePage.java       # Customer profile update; success validation
│   ├── runner/
│   │   └── TestRunner.java              # @CucumberOptions; tag filter via system property
│   ├── stepdefinitions/
│   │   ├── LoginSteps.java
│   │   └── RegisterSteps.java
│   └── utils/
│       ├── ElementActions.java          # click, sendKeys, getText, clear
│       ├── ExcelDataProvider.java       # POI reader; TestNG @DataProvider
│       ├── JSUtils.java                 # JavaScript execution helpers
│       ├── ParaBankApiClient.java       # HTTP POST for test user registration
│       ├── ScreenshotUtils.java         # PNG capture; Cucumber attachment
│       └── WaitUtils.java              # WebDriverWait wrappers (no Thread.sleep)
│
└── resources/
    ├── config/
    │   ├── qa.properties                # Active: public demo credentials (env-var override)
    │   ├── staging.properties           # Staging: credentials via TEST_USERNAME/TEST_PASSWORD
    │   ├── uat.properties               # UAT: credentials via TEST_USERNAME/TEST_PASSWORD
    │   └── prod.properties             # Production: read-only; credentials via secrets only
    ├── data/
    │   └── ddt.xlsx                     # Excel data for data-driven scenarios
    ├── features/
    │   ├── login.feature
    │   └── register.feature
    ├── logback.xml                      # Rolling log + console; MDC scenarioName
    └── extent-config.xml                # ExtentReports theme and document settings
```

---

## Quick Start

### Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 17 or later |
| Google Chrome | Latest stable |
| Docker Desktop | 24+ (optional — for Docker / Grid execution) |

Chrome driver is managed automatically by WebDriverManager. No manual driver download needed.

### Run Tests

```bash
# Clone and enter the project
git clone https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation.git
cd BDDFramework-ParabankAutomation

# Run the full default suite (@smoke, @negative, @regression)
./gradlew clean test
```

Expected result: **18 tests — 12 passed, 6 failed** (the 6 failures are caused by the
ParaBank public demo server's input sanitization; they are not framework defects).

---

## Execution Options

### 1. Local Execution

```bash
# Full default suite
./gradlew clean test

# Specific tag
./gradlew clean test -Dcucumber.filter.tags="@smoke"

# Firefox browser
./gradlew clean test -Dbrowser=firefox

# Headless Chrome
./gradlew clean test -Dheadless=true

# Different environment
./gradlew clean test -Denv=staging

# Increase thread count (parallel)
./gradlew clean test -DmaxParallelForks=4 -Ddataproviderthreadcount=4
```

### 2. Docker Execution

No Java or Chrome installation needed on the host. Chrome and Xvfb run inside the container.

```bash
# Build the image
docker build -t parabank-bdd:latest .

# Run with Docker Compose (recommended)
docker compose up --build

# Pass environment and browser
ENV=staging BROWSER=chrome docker compose up

# Run in background, then follow logs
docker compose up -d
docker compose logs -f

# Clean up
docker compose down
```

The compose file maps `build/reports`, `build/screenshots`, `build/logs`, and
`allure-results` to the host so artifacts are available after the container exits.

### 3. Selenium Grid Execution

Grid execution routes WebDriver commands through the Selenium Hub to a Chrome Node.
A health-check cascade in `docker-compose.grid.yml` ensures the Hub and Chrome Node are
both ready before tests start. See [docs/SELENIUM_GRID_GUIDE.md](docs/SELENIUM_GRID_GUIDE.md)
for the complete workflow, configuration reference, and failure diagnostics.

**Quick start (host-side execution):**

```powershell
# 1. Start Hub and Chrome Node
docker compose -f docker-compose.grid.yml up -d selenium-hub chrome-node

# 2. Wait for Grid readiness (exits 0 when a node is registered)
.\scripts\wait-for-grid.ps1

# 3. Run tests via RemoteWebDriver
./gradlew clean test -DseleniumGridEnabled=true -DgridUrl=http://localhost:4444/wd/hub

# 4. Clean up
docker compose -f docker-compose.grid.yml down -v --remove-orphans
```

**Grid Console UI:** `http://localhost:4444/ui`
**Live Chrome session view (noVNC):** `http://localhost:7900` (password: `secret`)

#### Local vs Grid Comparison

| Feature | Local | Selenium Grid |
|---|---|---|
| Browser location | Same machine as tests | Chrome Node container |
| Driver creation | `ChromeDriver` via WebDriverManager | `RemoteWebDriver` via Hub URL |
| Setup | None (WebDriverManager auto-downloads) | `docker compose -f docker-compose.grid.yml up` |
| Parallel browsers | Limited by local resources | Scale by adding Node containers |
| Session visibility | Not visible remotely | noVNC on port 7900 |
| Use case | Local development, single-machine CI | Distributed CI, cross-browser grids |

### 4. GitHub Actions CI

Push to `main` or `develop` triggers the pipeline automatically.
Manual runs are available from **Actions → Run workflow** with environment selection.

```
Push / PR → ubuntu-latest runner
  → Java 17 setup + Gradle cache
  → Chrome stable install
  → Xvfb virtual display
  → ./gradlew clean test
  → Artifacts uploaded (reports, screenshots, logs)
```

See [GitHub Actions CI](#github-actions-ci) section for full details.

---

## Configuration

### Priority Resolution

Configuration values are resolved in this order (first match wins):

```
1. JVM system property  (-Dkey=value on command line)
2. Environment variable  (TEST_USERNAME, TEST_PASSWORD, SELENIUM_GRID_ENABLED ...)
3. Properties file       (src/test/resources/config/qa.properties)
4. Built-in default      (hardcoded in ConfigManager)
```

### Browser Selection

```bash
./gradlew clean test -Dbrowser=chrome    # default
./gradlew clean test -Dbrowser=firefox
./gradlew clean test -Dbrowser=edge
./gradlew clean test -Dbrowser=safari    # macOS only
```

Or set in `qa.properties`:

```properties
browserName=chrome
```

### Environment Selection

```bash
./gradlew clean test -Denv=qa        # default
./gradlew clean test -Denv=staging
./gradlew clean test -Denv=uat
./gradlew clean test -Denv=prod
```

Each environment requires a matching properties file:
`src/test/resources/config/<env>.properties`

### Headless Mode

```bash
# Headless Chrome (no visible window)
./gradlew clean test -Dheadless=true

# In Docker (Xvfb provides virtual display — headless=false is the default)
docker compose up
```

In the Docker pipeline, `headless=false` with Xvfb is the deliberate choice: it produces
more realistic test conditions and accurate failure screenshots. Pass `-Dheadless=true`
to override when a fully headless run is preferred.

### Credentials

Credentials follow the full 4-level priority chain:

```
1. -DTEST_USERNAME / -DTEST_PASSWORD  (JVM system property)
2. TEST_USERNAME / TEST_PASSWORD       (environment variable)
3. username / password                 (qa.properties)
4. No default — missing value causes ConfigManager to return null
```

The values in `qa.properties` are the public ParaBank demo credentials (`sqa / sqa`).
They are intentionally kept in the file as a fallback for open-source / demo use.
In real projects, replace them with your environment's credentials via env vars or secrets.

```properties
# qa.properties — fallback values only
# Override via environment variables (preferred for CI/CD):
#   export TEST_USERNAME=<value>
#   export TEST_PASSWORD=<value>
username=sqa
password=sqa
```

```bash
# Local override via system property
./gradlew clean test -DTEST_USERNAME=myuser -DTEST_PASSWORD=mypass

# Local override via environment variable
export TEST_USERNAME=myuser
export TEST_PASSWORD=mypass
./gradlew clean test
```

### Selenium Grid Configuration

```bash
# Run against any Selenium Grid
./gradlew clean test \
  -DseleniumGridEnabled=true \
  -DgridUrl=http://your-hub:4444/wd/hub
```

Or set in `qa.properties`:

```properties
seleniumGridEnabled=false
remoteGridUrl=http://selenium-hub:4444/wd/hub
```

---

## Production Write Protection

> **Warning:** Running `./gradlew clean test -Denv=prod` or `-Denv=production` will block
> automatic test-data writes before they reach the network. A `ConfigurationException` is
> thrown immediately with a clear message. No registration request is made.

The framework includes an orchestration-level guard that prevents automatic test-user creation
from running against a production environment.

### How it works

```
Hooks.ensureDefaultTestUserExists()
    → ConfigManager.guardAgainstProductionWrite("default test user registration")
        → throws ConfigurationException  (when env = "prod" or "production")
        → continues                      (all other environments)
    → ParaBankApiClient  (API registration attempt)
    → browser registration fallback
```

`prod` and `production` are both treated as production aliases. Detection is case-insensitive.
The guard is placed at the single orchestration boundary that controls both the API write path
and the browser fallback — one call protects both.

### What the guard protects

- Automatic test-user registration via API (`ParaBankApiClient`)
- Automatic test-user registration via browser fallback

### What the guard does not restrict

- Read-only scenario execution (login tests, page navigation, assertions)
- Any write action initiated by a scenario step against the AUT itself

### Validated

Tested by `ProductionSafetyGuardTest` (12 focused TestNG tests, no browser, no network):

| Environment value | Guard behaviour |
|---|---|
| `prod` | Throws `ConfigurationException` |
| `production` | Throws `ConfigurationException` |
| `PROD` / `Production` | Throws `ConfigurationException` (case-insensitive) |
| `qa`, `staging`, `uat`, default | Passes — no exception thrown |

---

## Tag Filtering

Tag selection is driven at runtime via `-Dcucumber.filter.tags`.
The default expression is `@smoke or @negative or @regression`.

```bash
# Default — runs @smoke + @negative + @regression (18 scenario executions)
./gradlew clean test

# Smoke tests only (fast sanity check)
./gradlew clean test -Dcucumber.filter.tags="@smoke"

# Full regression suite
./gradlew clean test -Dcucumber.filter.tags="@regression"

# Security-oriented negative scenarios only
./gradlew clean test -Dcucumber.filter.tags="@negative and @security"

# Everything except negative tests
./gradlew clean test -Dcucumber.filter.tags="not @negative"
```

Active tags (current feature files — commit e118ac9):

| Tag | Feature file(s) | Purpose |
|---|---|---|
| `@smoke` | `login.feature` | Single critical happy-path; fast sanity check |
| `@regression` | `login.feature`, `register.feature` | Full feature regression suite (feature-level tag) |
| `@positive` | `login.feature`, `register.feature` | Happy-path scenarios |
| `@negative` | `login.feature`, `register.feature` | Boundary, invalid-input, and rejection scenarios |
| `@validation` | `login.feature`, `register.feature` | Input-validation and business-rule scenarios |
| `@security` | `login.feature` | Injection and scripting-probe scenarios |

---

## Reporting

### ExtentReports (Interactive HTML)

Generated after every run:

```
build/reports/extent/Report.html
```

Open in any browser. Includes: scenario timeline, pass/fail pie chart, step details, embedded failure screenshots.

Configuration: `src/test/resources/extent-config.xml`

### Allure Report

Raw results written to `allure-results/`. Generate the full HTML report with:

```bash
allure generate allure-results --clean -o allure-report
allure open allure-report
```

Allure provides trend graphs, flaky test detection, and step-level attachments.

### Cucumber HTML Report

```
build/reports/cucumber/cucumber-report.html
```

Standard Cucumber feature/scenario/step breakdown.

### TestNG Reports

```
build/reports/tests/test/index.html
```

TestNG suite summary with pass/fail counts and timing.

### Failure Screenshots

Captured automatically on scenario failure:

```
build/screenshots/<scenario-name>_<timestamp>.png
```

Screenshots are also embedded inline in both the ExtentReport and the Cucumber HTML report.

### Logs

Rolling log files with MDC `scenarioName` tag for parallel tracing:

```
build/logs/test.log          # current run
build/logs/test.2026-06-19.log   # rolled daily or at 10 MB
```

Log format: `[timestamp] [thread] [scenarioName] LEVEL message`

---

## Architecture Details

### Page Object Model

Each page class extends `BasePage` and exposes a fluent API.
Locators are private `By` fields — never exposed to step definitions.

```java
public class LoginPage extends BasePage {
    private static final By USERNAME_FIELD = By.cssSelector("input[name='username']");
    private static final By PASSWORD_FIELD = By.cssSelector("input[name='password']");
    private static final By LOGIN_BUTTON   = By.cssSelector("input[value='Log In']");

    public LoginPage(WebDriver driver) { super(driver); }

    public LoginPage fillUsername(String username) {
        sendKeys(USERNAME_FIELD, username);
        return this;
    }

    public LoginPage fillPassword(String password) {
        sendKeys(PASSWORD_FIELD, password);
        return this;
    }

    public OverviewPage clickLoginButton() {
        click(LOGIN_BUTTON);
        return createPage(OverviewPage.class);
    }
}
```

`BasePage.createPage()` uses reflection — returns a typed page object so step definitions
get compile-time type safety when navigating between pages.

### Thread-Safe Driver Management

WebDriver is stored in `ThreadLocal` so each parallel test thread has its own isolated browser instance:

```java
// Hooks.@Before — called once per scenario per thread
DriverManager.initializeDriver();

// Step definitions and page objects
WebDriver driver = DriverManager.getDriver();

// Hooks.@After — always called, even on failure
DriverManager.quitDriver();
```

`DriverFactory` decides whether to create a `ChromeDriver`/`FirefoxDriver` (local) or
`RemoteWebDriver` (Selenium Grid) based on `ConfigManager.isSeleniumGridEnabled()`.

### Explicit Waits

All element interactions go through `WaitUtils`. `Thread.sleep()` does not appear anywhere in the framework.

```java
// Wait for element to be visible before reading text
WaitUtils.waitForElementToBeVisible(driver, locator);

// Wait for element to be clickable before clicking
WaitUtils.waitForElementToBeClickable(driver, locator);

// Wait for element to exist in DOM
WaitUtils.waitForElementToBePresent(driver, locator);
```

### Step Definitions

Gherkin steps map to Java methods. All UI work is delegated to page objects.

```java
@Given("User is on Login Page")
public void user_on_login_page() {
    loginPage = new LoginPage(DriverManager.getDriver());
}

@When("User enters username {string} and password {string}")
public void user_enters_credentials(String username, String password) {
    loginPage.fillUsername(username).fillPassword(password);
}

@When("User clicks login button")
public void user_clicks_login_button() {
    overviewPage = loginPage.clickLoginButton();
}

@Then("User should see logout link")
public void user_should_see_logout_link() {
    Assert.assertTrue(overviewPage.hasLogOutLink(), "Logout link should be visible");
}
```

---

## Parallel Execution

Tests run in parallel by default. `TestRunner` uses TestNG's `@DataProvider(parallel = true)`.

```bash
# Default: 2 Gradle forks, 2 DataProvider threads
./gradlew clean test

# Scale up for larger suites
./gradlew clean test -DmaxParallelForks=4 -Ddataproviderthreadcount=4
```

Each thread has its own `WebDriver` via `DriverManager` (ThreadLocal), its own MDC log context, and is fully isolated from other threads.

CI runs use `maxParallelForks=1` and `dataproviderthreadcount=1` to avoid contention on the 2-vCPU GitHub-hosted runner.

---

## Logging

Logback is configured with:
- **Console appender** — INFO level with MDC `scenarioName` tag
- **Rolling file appender** — daily rotation + 10 MB size cap, 30-day retention, 500 MB total cap

```java
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

logger.info("Action started: {}", description);
logger.warn("Potential issue: {}", detail);
logger.error("Step failed", exception);
```

MDC keys set per scenario (in `Hooks.@Before`):
- `scenarioName` — allows filtering the rolling log file by specific scenario name across parallel threads

Log location: `build/logs/test.log`

---

## Secret Management

### How credentials are resolved

```
Priority 1: JVM system property  →  -DTEST_USERNAME=value
Priority 2: Environment variable →  export TEST_USERNAME=value
Priority 3: Properties file      →  username=sqa in qa.properties
Priority 4: null (no default)
```

Passwords are **never printed in any log statement** in the framework. Only usernames are logged (for traceability).

### Local development

```bash
# Option A: system property (temporary, session-scoped)
./gradlew clean test -DTEST_USERNAME=myuser -DTEST_PASSWORD=mypass

# Option B: environment variable (shell session)
export TEST_USERNAME=myuser
export TEST_PASSWORD=mypass
./gradlew clean test

# Option C: edit qa.properties (local only, never commit real credentials)
username=myuser
password=mypass
```

### CI/CD (GitHub Actions)

Add repository secrets in **Settings → Secrets and variables → Actions**:
- `TEST_USERNAME`
- `TEST_PASSWORD`

The workflow reads them as:
```yaml
env:
  TEST_USERNAME: ${{ secrets.TEST_USERNAME }}
  TEST_PASSWORD: ${{ secrets.TEST_PASSWORD }}
```

When secrets are not configured, the workflow falls back to `qa.properties` values automatically (the `isBlank()` guard in `ConfigManager` skips empty strings).

### Docker

Pass credentials as environment variables — never bake them into the image:

```bash
# docker run
docker run --rm \
  -e TEST_USERNAME=myuser \
  -e TEST_PASSWORD=mypass \
  parabank-bdd:latest

# docker compose (recommended)
TEST_USERNAME=myuser TEST_PASSWORD=mypass docker compose up

# .env file (do not commit to git)
echo "TEST_USERNAME=myuser" >> .env
echo "TEST_PASSWORD=mypass" >> .env
docker compose up
```

---

## Selenium Grid

### What is Selenium Grid?

Selenium Grid 4 separates test execution (the automation container) from browser execution (the Chrome Node). The Hub receives WebDriver commands and routes them to available nodes.

```
Automation Container              Selenium Grid
(no Chrome needed)                +-----------+      +-----------+
                                  |           |      |           |
  Test Code  --WebDriver-HTTP-->  |    Hub    | ---> | Chrome    |
                                  |           |      | Node      |
                                  +-----------+      +-----------+
                                  :4444/wd/hub       :7900 (noVNC)
```

### Start the Grid and run tests

```bash
# All-in-one: builds image, starts Hub + Node, runs tests
docker compose -f docker-compose.grid.yml up --build

# Watch the Chrome session live (while tests are running)
# Open in browser: http://localhost:7900  (password: secret)

# After tests complete
docker compose -f docker-compose.grid.yml down
```

### Grid UI and debugging

| URL | Purpose |
|---|---|
| `http://localhost:4444/ui` | Grid Console — registered nodes, active sessions |
| `http://localhost:4444/wd/hub/status` | JSON status endpoint |
| `http://localhost:7900` | noVNC — live Chrome session view (password: `secret`) |

### Run tests against an external grid

```bash
./gradlew clean test \
  -DseleniumGridEnabled=true \
  -DgridUrl=http://your-hub-host:4444/wd/hub \
  -Dbrowser=chrome
```

### Grid startup note

The Hub health check (`/wd/hub/status?ready=true`) must pass before the Chrome Node starts,
and the Node must register with the Hub before tests start. Allow 30–60 seconds for the grid
to be fully ready on first boot. If the first test run fails with "no available nodes",
wait and re-run.

---

## Docker Support

### Files

| File | Purpose |
|---|---|
| `Dockerfile` | Ubuntu 22.04 + Java 17 + Chrome + Xvfb — test image |
| `docker-compose.yml` | Single-container local execution with volume mounts |
| `docker-compose.grid.yml` | Hub + Chrome Node + test container for Grid execution |
| `docker-entrypoint.sh` | Starts Xvfb, logs runtime config, calls `./gradlew clean test` |
| `.dockerignore` | Excludes build artefacts, IDE files, `.env` from build context |

### Build

```bash
docker build -t parabank-bdd:latest .
```

Layer caching order (outer layers change rarely, inner layers change often):
```
Layer 1 — system packages (Chrome, Xvfb)     → rebuild: never
Layer 2 — Gradle wrapper                      → rebuild: Gradle version change
Layer 3 — build.gradle (dependencies)         → rebuild: dependency change
Layer 4 — src/ (test code)                    → rebuild: every code change (fast copy)
```

### Run

```bash
# Docker Compose (recommended)
docker compose up --build

# Override variables inline
ENV=staging BROWSER=chrome MAX_PARALLEL_FORKS=1 docker compose up

# Plain docker run with volume mounts
docker run --rm \
  --shm-size=2g \
  -e ENV=qa \
  -e BROWSER=chrome \
  -v "$(pwd)/build/reports:/app/build/reports" \
  -v "$(pwd)/build/screenshots:/app/build/screenshots" \
  -v "$(pwd)/build/logs:/app/build/logs" \
  -v "$(pwd)/allure-results:/app/allure-results" \
  parabank-bdd:latest
```

> **`--shm-size=2g` is required.** Chrome uses `/dev/shm` for renderer sandboxing.
> The Docker default of 64 MB causes tab crashes. Docker Compose sets this automatically.

### Supported environment variables

| Variable | Default | Description |
|---|---|---|
| `ENV` | `qa` | Properties file to load (`qa.properties`) |
| `BROWSER` | `chrome` | Browser to use |
| `MAX_PARALLEL_FORKS` | `1` | Gradle parallel test forks |
| `DATA_PROVIDER_THREAD_COUNT` | `1` | TestNG DataProvider threads |
| `JAVA_OPTS` | `-Xmx1g -Xms512m` | JVM heap settings |
| `SELENIUM_GRID_ENABLED` | `false` | Route to Selenium Hub instead of local Chrome |
| `GRID_URL` | *(empty)* | Selenium Hub URL (used when `SELENIUM_GRID_ENABLED=true`) |
| `TEST_USERNAME` | *(falls back to properties file)* | Override test username |
| `TEST_PASSWORD` | *(falls back to properties file)* | Override test password |

### Artifact paths (host-side after run)

| Path | Contents |
|---|---|
| `build/reports/extent/Report.html` | ExtentReports interactive HTML |
| `build/reports/cucumber/cucumber-report.html` | Cucumber HTML report |
| `build/reports/tests/` | TestNG suite report |
| `build/screenshots/` | Failure screenshots (PNG) |
| `build/logs/test.log` | Rolling Logback output with MDC tags |
| `allure-results/` | Raw JSON for `allure generate` |

---

## GitHub Actions CI

For full details, see [docs/CI_CD_GUIDE.md](docs/CI_CD_GUIDE.md).

### Workflow file

```
.github/workflows/automation-test.yml
```

### Triggers

| Trigger | Condition |
|---|---|
| `push` | `main` or `develop` branch |
| `pull_request` | Targeting `main` or `develop` |
| `workflow_dispatch` | Manual run — `qa`, `staging`, `uat` |

### Pipeline stages

| Stage | Command | Behaviour |
|---|---|---|
| Compile | `./gradlew compileTestJava` | Fail fast — no tests run on error |
| Production-safety tests | `./gradlew test --tests "*ProductionSafetyGuardTest"` | 12 tests; no browser; fail fast |
| Full regression | `xvfb-run ./gradlew clean test` | 18 Cucumber scenarios; exit code captured |
| Classify + summary | `scripts/analyze-test-results.sh` | Final gate — see table below |

### CI classification

| Result | Meaning | Badge |
|---|---|---|
| `VALIDATED_BASELINE` | Exactly the 6 known AUT failures — no regression | Green |
| `UNEXPECTED_REGRESSION` | Different or additional failures | Red |
| `INFRASTRUCTURE_FAILURE` | Cucumber suite did not execute | Red |
| `RESULTS_UNAVAILABLE` | No JUnit XML produced | Red |

A green CI badge means the run matched the accepted known-failure baseline exactly.

### Manual execution

1. Go to **Actions → ParaBank BDD Automation Tests**
2. Click **Run workflow**
3. Select the target environment (`qa`, `staging`, or `uat`)
4. Click **Run workflow**

### Credentials in CI

Add repository secrets under **Settings → Secrets and variables → Actions**:
- `TEST_USERNAME` — overrides `username` in `qa.properties`
- `TEST_PASSWORD` — overrides `password` in `qa.properties`

If secrets are not configured, the pipeline falls back to `qa.properties` values.

### Concurrency

Main branch runs are never cancelled. PR and feature branch runs cancel on new push:

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}
```

### Artifacts

Each run produces named artifacts in the **Artifacts** panel (retained 14 days):

| Artifact | Contents |
|---|---|
| `extent-report-<env>-run<N>` | Interactive HTML report |
| `allure-results-<env>-run<N>` | Raw Allure results |
| `testng-reports-<env>-run<N>` | TestNG / Cucumber HTML |
| `screenshots-<env>-run<N>` | Failure PNGs |
| `test-logs-<env>-run<N>` | Rolling Logback output |

---

## Troubleshooting

### Build fails to compile

```bash
./gradlew clean --refresh-dependencies
./gradlew clean test
```

Check Java version:
```bash
java -version   # must be 17+
```

### Tests time out waiting for elements

```bash
# Increase explicit wait timeout
./gradlew clean test -DwaitTime=60
```

### Chrome crashes in Docker

Ensure `--shm-size=2g` is set (Docker Compose does this automatically):
```bash
docker run --rm --shm-size=2g parabank-bdd:latest
```

### Selenium Grid: "no available nodes"

The Chrome Node needs 20–30 seconds to register with the Hub after startup.
Wait for the Node to appear on `http://localhost:4444/ui` before running tests,
or run `docker compose -f docker-compose.grid.yml up --build` which uses the
Hub's health check to gate the test container startup.

### Memory issues

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew clean test
```

Or in Docker:
```bash
JAVA_OPTS="-Xmx2g -Xms1g" docker compose up
```

### Known external failures (ParaBank demo server)

Six scenarios fail against the public ParaBank demo due to server-side behaviour —
not framework defects:

| Failure type | Count | Root cause |
|---|---|---|
| Injection tests | 3 | Server sanitizes input; `p.error` element never rendered |
| Registration redirect | 3 | Server session handling does not redirect to overview |

These are the established baseline. Framework changes do not affect this count.

---

## Best Practices

1. **No `Thread.sleep()`** — use `WaitUtils` explicit waits
2. **Private locators** — keep `By` fields private in page classes
3. **Fluent API** — chain page methods for readable steps
4. **No PII in source** — use `LoremIpsum` or generated data for test inputs
5. **Credentials via env vars** — never commit real usernames or passwords
6. **One assertion focus per scenario** — keep `@Then` steps specific
7. **MDC logging** — add `scenarioName` to all log calls via Logback MDC
8. **Page objects over raw driver calls** — never call `driver.findElement()` in step definitions or hooks

---

## Quality and Risk Documentation

| Document | Description |
|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contributor setup, branch workflow, validation baseline, and documentation obligations |
| [docs/FRAMEWORK_EXTENSION_GUIDE.md](docs/FRAMEWORK_EXTENSION_GUIDE.md) | Architecture boundaries, layer rules, locator/wait standards, anti-patterns, and scenario workflow |
| [docs/PULL_REQUEST_CHECKLIST.md](docs/PULL_REQUEST_CHECKLIST.md) | Copyable PR submission checklist with validation evidence template |
| [docs/SELENIUM_GRID_GUIDE.md](docs/SELENIUM_GRID_GUIDE.md) | Selenium Grid startup, readiness verification, host-side execution, failure diagnostics, and teardown |
| [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md) | Test strategy, tag model, environment model, and execution guidance |
| [docs/QUALITY_RISK_ASSESSMENT.md](docs/QUALITY_RISK_ASSESSMENT.md) | Evidence-based risk register, technical debt, test debt, and release interpretation |
| [docs/KNOWN_AUT_LIMITATIONS.md](docs/KNOWN_AUT_LIMITATIONS.md) | Records and analysis of the 6 known AUT failures — why they occur and why they remain active |

---

**Framework Version:** 1.0.0
**Validated commit:** e118ac9
**Last Updated:** July 2026
