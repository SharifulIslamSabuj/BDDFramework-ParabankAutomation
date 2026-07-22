# Portfolio Overview

**Framework:** ParaBank BDD Automation  
**Repository:** https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation  
**Last Updated:** 2026-07-19

> This document is intended for technical recruiters, hiring managers, and interviewers
> who want a structured summary of what this repository demonstrates.

---

## 1. Project Snapshot

| Property | Value |
|---|---|
| Language | Java 17 |
| AUT | ParaBank — public banking demo application |
| AUT URL | https://parabank.parasoft.com/parabank/ |
| Framework type | Portfolio-grade BDD automation |
| Test count | 30 (12 safety + 18 Cucumber scenarios) |
| CI classification | `VALIDATED_BASELINE` for accepted 18/12/6 result |
| Execution modes | Local browser, Docker container, Selenium Grid |
| Repository type | Public demonstration — not a commercial product |

**What ParaBank is:** A public demo banking application maintained by Parasoft for QA training and demonstration purposes. The framework targets this AUT to simulate real-world banking UI automation in a safe, publicly accessible environment.

---

## 2. Skills Demonstrated

| Skill Area | Evidence in Repository |
|---|---|
| **Behaviour-Driven Development** | Gherkin feature files, Cucumber step definitions, `@DataProvider(parallel=true)` |
| **Page Object Model** | `BasePage` with typed `createPage()` factory; private locators; fluent API |
| **Parallel test execution** | `ThreadLocal<WebDriver>`, TestNG DataProvider parallelism, SLF4J MDC per thread |
| **Explicit wait strategy** | `WaitUtils` using `WebDriverWait`; zero `Thread.sleep()` in framework |
| **API-assisted test setup** | `ParaBankApiClient` with Java 17 `HttpClient`; browser fallback; idempotent registration |
| **Data-driven testing** | `ExcelDataProvider` with Apache POI; `LoremIpsum` generated data |
| **Selenium Grid** | Docker Compose health-check cascade; Hub readiness validation; `RemoteWebDriver` |
| **Docker containerisation** | `Dockerfile`, `docker-compose.yml`, `docker-compose.grid.yml` with volume mounts |
| **CI/CD pipeline** | GitHub Actions with staged gates: compile → safety → regression → classify |
| **Result classification** | Python 3 classifier — JUnit XML execution counts + Cucumber JSON scenario-ID identity; 4 classification outcomes; structured job summary |
| **Multi-environment config** | 4-level priority chain (JVM property → env var → properties file → default) |
| **Production safety control** | Write-guard prevents automatic test-data creation against production aliases |
| **Credential security** | No secrets in source; GitHub Secrets integration; passwords never logged |
| **Typed exception hierarchy** | `FrameworkException` → `WaitException`, `DriverInitializationException`, etc. |
| **Structured reporting** | ExtentReports, Allure, Cucumber HTML, TestNG, MDC logs, failure screenshots |
| **Risk-based QA thinking** | QUALITY_RISK_ASSESSMENT.md with evidence-based risk register and technical debt |
| **Contributor governance** | Framework extension guide, PR checklist, architecture boundaries |

---

## 3. Engineering Problems Solved

This repository demonstrates solutions to real problems encountered in QA automation projects:

### Problem 1 — Production write risk
**Problem:** Automated test-data creation (user registration) running against production environments.  
**Solution:** `ConfigManager.guardAgainstProductionWrite()` throws before either write path (API or browser) executes when `env=prod` or `env=production`.  
**Evidence:** `ProductionSafetyGuardTest` — 12 focused tests covering case-insensitive detection.

### Problem 2 — CI permanently red from known AUT failures
**Problem:** Six scenarios consistently fail against the public demo server. A raw Gradle exit maps every run to a red CI badge, making regression detection impossible.  
**Solution:** `scripts/analyze-test-results.sh` classifies results as `VALIDATED_BASELINE` / `UNEXPECTED_REGRESSION` / `INFRASTRUCTURE_FAILURE` / `RESULTS_UNAVAILABLE`. CI is green when the exact known-failure set is observed.  
**Evidence:** `docs/guides/CI_CD_GUIDE.md`.

### Problem 3 — Selenium Grid startup race condition
**Problem:** `docker-compose.grid.yml` originally started the test container as soon as the Chrome Node container started — before the node had registered with the Hub.  
**Solution:** Docker health-check cascade: hub liveness → chrome-node registration → test container start.  
**Evidence:** `docker-compose.grid.yml` health-check cascade; `docs/guides/SELENIUM_GRID_GUIDE.md`.

### Problem 4 — Parallel test thread interference
**Problem:** Parallel test threads sharing a `WebDriver` instance or a static-initialization resource.  
**Solution:** `DriverManager` uses `ThreadLocal<WebDriver>`; test-user provisioning uses `AtomicBoolean` with double-checked locking; SLF4J MDC provides per-thread log traceability.  
**Evidence:** `src/test/java/.../driver/DriverManager.java`, `hooks/Hooks.java`.

### Problem 5 — Contributor extension risk
**Problem:** New scenarios or page objects added without following layer contracts, bypassing waits, or exposing locators.  
**Solution:** `docs/FRAMEWORK_EXTENSION_GUIDE.md` documents 11 named anti-patterns, layer rules, and a step-by-step scenario addition workflow.  
**Evidence:** `docs/FRAMEWORK_EXTENSION_GUIDE.md`, `docs/PULL_REQUEST_CHECKLIST.md`.

---

## 4. Framework Capabilities

### What the framework can do

- Execute Cucumber BDD scenarios against the ParaBank demo application
- Run tests locally using Chrome, Firefox, or Edge (via WebDriverManager)
- Run tests inside Docker containers (no local Chrome or Java installation required on host)
- Route WebDriver sessions through Selenium Grid for distributed or cross-node execution
- Generate four concurrent report formats per run (ExtentReports, Allure, Cucumber HTML, TestNG)
- Classify CI results against an accepted known-failure baseline
- Prevent automatic test-data writes to production aliases

### What the framework does not do

- Cover all ParaBank features — 7 page objects are implemented; 5 lack feature file coverage
- Support mobile browsers
- Perform backend API contract testing
- Perform database-level assertions
- Perform visual regression (pixel comparison)
- Guarantee behaviour with private or modified ParaBank deployments
- Cover cross-browser execution in CI (Chrome only on GitHub-hosted runners)

---

## 5. Validation Evidence

The following table reflects the current validated state on `main` after PR #1 (the Cucumber JSON scenario-ID classifier) merged:

| Metric | Value |
|---|---|
| Compile | Successful |
| Production-safety tests | 12/12 passed |
| Cucumber executions | 18 |
| Passed scenarios | 12 |
| Known AUT failures | 6 (matched by stable Cucumber JSON scenario `id`, not execution order) |
| Unexpected failures | 0 |
| CI classification | `VALIDATED_BASELINE` |
| Selenium Grid regression | 18/12/6 (identical to local) |

The 6 known failures are documented in [docs/KNOWN_AUT_LIMITATIONS.md](quality/KNOWN_AUT_LIMITATIONS.md). They are intentionally preserved to document AUT behaviour, not suppressed.

---

## 6. Repository Navigation

| Starting point | Best for |
|---|---|
| [README.md](../README.md) | Full project overview and quick start |
| [docs/architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) | Framework design and execution flows |
| [docs/quality/TEST_STRATEGY.md](quality/TEST_STRATEGY.md) | Test scope, tags, and known failure baseline |
| [docs/guides/CI_CD_GUIDE.md](guides/CI_CD_GUIDE.md) | CI pipeline and classification |
| [docs/quality/QUALITY_RISK_ASSESSMENT.md](quality/QUALITY_RISK_ASSESSMENT.md) | Risk register and engineering decisions |
| [docs/quality/KNOWN_AUT_LIMITATIONS.md](quality/KNOWN_AUT_LIMITATIONS.md) | Why 6 scenarios consistently fail |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | How to extend the framework |

---

## 7. Suggested Review Path

### Quick review (10 minutes)

1. Read [README.md](../README.md) — positioning, capabilities, and validation status
2. Skim `src/test/resources/features/login.feature` — BDD scenario structure
3. Open `src/test/java/.../pages/LoginPage.java` — page object design
4. Open `src/test/java/.../stepdefinitions/LoginSteps.java` — step delegation
5. Open `.github/workflows/automation-test.yml` — CI structure
6. Open [docs/KNOWN_AUT_LIMITATIONS.md](quality/KNOWN_AUT_LIMITATIONS.md) — honest limitation documentation

### Deeper technical review (30 minutes)

Add to the quick review:

7. Open `src/test/java/.../hooks/Hooks.java` — lifecycle and provisioning
8. Open `src/test/java/.../config/ConfigManager.java` — credential chain and production guard
9. Open `src/test/java/.../driver/DriverFactory.java` — local vs remote driver selection
10. Open `scripts/analyze-test-results.sh` — CI classification logic
11. Read [docs/QUALITY_RISK_ASSESSMENT.md](quality/QUALITY_RISK_ASSESSMENT.md) — risk-based QA thinking
12. Read [docs/ARCHITECTURE.md](architecture/ARCHITECTURE.md) — design decisions and trade-offs

---

## 8. Honest Limitations

| Limitation | Context |
|---|---|
| AUT is a public shared demo | ParaBank is uncontrolled; all 6 known failures originate from server-side behaviour, not framework defects |
| Chrome only in CI | GitHub-hosted runners support Chrome; Firefox/Edge run locally but not in the current CI pipeline |
| No Grid in CI | Selenium Grid execution requires Docker; GitHub-hosted runners do not run the Grid stack |
| No cleanup | Registration data accumulates on the public AUT; no cleanup API is available |
| 5 page objects without scenarios | `OpenNewAccountPage`, `OpenedAccountPage`, `RequestLoanPage`, `ApprovedLoanPage`, `UpdateProfilePage` are implemented but not yet covered by feature files |
| No license | Public reuse terms are not specified — contact the repository owner before reusing |
| Portfolio context | This repository is a technical demonstration, not a commercial or production deployment |

---

*See [docs/QUALITY_RISK_ASSESSMENT.md](quality/QUALITY_RISK_ASSESSMENT.md) for the full risk register with detailed records and residual risk analysis.*
