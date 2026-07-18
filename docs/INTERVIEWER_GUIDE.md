# Interviewer Review Guide

**Framework:** ParaBank BDD Automation  
**Repository:** https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation  
**Last Updated:** 2026-07-19

> This guide helps technical interviewers and reviewers navigate repository evidence during
> a structured technical review. It does not supply scripted answers. Its purpose is to point
> to evidence so reviewers can evaluate depth independently.

---

## 1. Five-Minute Review

| Step | What to look at | What to observe |
|---|---|---|
| 1 | `README.md` (first screen) | Project positioning, capability table, validation status |
| 2 | `src/test/resources/features/login.feature` | Scenario structure, tag usage, Gherkin quality |
| 3 | `src/test/java/.../pages/LoginPage.java` | Locator visibility, fluent API, `createPage()` usage |
| 4 | `src/test/java/.../stepdefinitions/LoginSteps.java` | Step delegation, assertion placement, driver access pattern |
| 5 | `.github/workflows/automation-test.yml` | Stage structure, classification, permission model |
| 6 | `docs/KNOWN_AUT_LIMITATIONS.md` | Honesty about AUT limitations; baseline understanding |

---

## 2. Fifteen-Minute Technical Review

Add to the five-minute review:

| Step | What to look at | What to observe |
|---|---|---|
| 7 | `src/test/java/.../hooks/Hooks.java` | Lifecycle management, test-user provisioning, guard placement |
| 8 | `src/test/java/.../config/ConfigManager.java` | Singleton design, credential priority chain, `guardAgainstProductionWrite()` |
| 9 | `src/test/java/.../factory/DriverFactory.java` | Local vs remote driver selection, Grid URL handling |
| 10 | `src/test/java/.../driver/DriverManager.java` | ThreadLocal storage, init/quit lifecycle |
| 11 | `scripts/analyze-test-results.sh` | JUnit XML parsing logic, classification states, exit codes |
| 12 | `docker-compose.grid.yml` | Health-check cascade, dependency conditions |
| 13 | `docs/QUALITY_RISK_ASSESSMENT.md` | Risk register depth, technical debt awareness |
| 14 | `git log --oneline` | Commit discipline, phase-by-phase development evidence |

---

## 3. Architecture Questions

The following questions have evidence in the repository. Reviewers can verify answers directly in source code.

**Q: How does the framework ensure test threads don't share browser instances?**  
Evidence: `src/test/java/.../driver/DriverManager.java` — `ThreadLocal<WebDriver>` field.

**Q: How does the page factory pattern work here?**  
Evidence: `BasePage.createPage(Class<T>)` — uses reflection to instantiate typed page objects, giving step definitions compile-time safety.

**Q: How does the framework switch between local and remote browser execution?**  
Evidence: `DriverFactory.createDriver()` — checks `ConfigManager.isSeleniumGridEnabled()` and instantiates either local driver or `RemoteWebDriver`.

**Q: Why does the framework use four levels of configuration priority?**  
Evidence: `ConfigManager` comment block — JVM property → env var → properties file → default. CI secrets, local overrides, and default public credentials coexist without conflict.

**Q: How are assertions enforced at the correct layer?**  
Evidence: `LoginSteps.java` — `Assert.assertTrue()` only in `@Then` methods, never in page objects or `@When`.

---

## 4. Automation Design Questions

**Q: Why are `By` locators kept private?**  
Evidence: `LoginPage.java`, `RegisterPage.java` — all `By` fields are `private static final`. Prevents test code from bypassing the page object layer.

**Q: How does the framework avoid `Thread.sleep()`?**  
Evidence: `WaitUtils.java` — all element interactions use `WebDriverWait`. Run `git grep "Thread.sleep"` in the repository — returns no matches.

**Q: How does the framework handle test-user setup across parallel threads?**  
Evidence: `Hooks.java` — `AtomicBoolean DEFAULT_USER_SETUP_DONE` with `synchronized` block ensures exactly one setup call per JVM, regardless of thread count.

**Q: How does the Excel data-driven scenario work?**  
Evidence: `ExcelDataProvider.java`, `register.feature` — POI reads `ddt.xlsx`; TestNG `@DataProvider` feeds rows to `TestRunner`.

**Q: What happens when the API registration endpoint returns an unexpected response?**  
Evidence: `Hooks.java` — `tryApiRegistration()` returns `true` on success or "already exists"; on unexpected response, control falls through to `tryBrowserRegistration()`.

---

## 5. CI/CD Questions

**Q: What problem did the old CI pipeline have?**  
Evidence: `docs/review/PHASE_10_CI_CD_IMPROVEMENTS.md` Section 4 — six AUT failures caused every run to show a red badge with no distinction between expected and unexpected failures.

**Q: How does the CI classifier work?**  
Evidence: `scripts/analyze-test-results.sh` — Python 3 parses JUnit XML, extracts `runScenario[N]` indices from failing testcase names, and compares the set to a hard-coded frozenset.

**Q: What does a green CI badge mean for this repository?**  
Evidence: `docs/CI_CD_GUIDE.md` — green means `VALIDATED_BASELINE`: exactly `{runScenario[9,10,11,13,14,15]}` failed, and no other deviations exist.

**Q: Why is `prod` absent from the workflow_dispatch choices?**  
Evidence: `.github/workflows/automation-test.yml` comment — "prod is intentionally absent — CI does not execute against production."

**Q: Why doesn't cancelling in-progress runs affect `main`?**  
Evidence: `automation-test.yml` concurrency block — `cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}`. Main-branch runs are the authoritative evidence record.

---

## 6. Risk and Quality Questions

**Q: Why are 6 test failures considered acceptable?**  
Evidence: `docs/KNOWN_AUT_LIMITATIONS.md` — documents two server-side issues (AUT-LIM-001: injection sanitization without error element; AUT-LIM-002: session handling without overview redirect). Both are reproducible, deterministic, and documented.

**Q: How does the production write guard work?**  
Evidence: `ConfigManager.guardAgainstProductionWrite()` — called inside the `synchronized` block in `Hooks.ensureDefaultTestUserExists()` before both write paths. Throws `ConfigurationException` on `prod`/`production` (case-insensitive).

**Q: What evidence exists that the guard is effective?**  
Evidence: `src/test/java/.../config/ProductionSafetyGuardTest.java` — 12 focused tests; verified in every phase regression.

**Q: How was the Selenium Grid startup race condition identified and fixed?**  
Evidence: `docs/review/PHASE_9_SELENIUM_GRID_READINESS.md` Sections 4, 8, 9 — original `grep '"ready":true'` approach caused deadlock; replaced with HTTP liveness check (`curl -f`); added chrome-node health check.

---

## 7. Trade-offs to Discuss

These design choices involve deliberate trade-offs worth exploring in a technical conversation:

| Decision | Trade-off to explore |
|---|---|
| Known failures kept active | What would suppressing them cost? When is suppression acceptable? |
| CI classifier with hard-coded failure set | What happens when the test suite grows? How would you automate baseline updates? |
| API registration with browser fallback | What are the failure modes of each path? How would you test the fallback? |
| Production guard at orchestration level | What scenario-step writes are not covered? Is that gap acceptable? |
| Public AUT dependency | How would the framework behave against a private ParaBank deployment? |
| Chrome-only Grid node | What would adding Firefox coverage require? |
| Single Chrome Node in Grid | How would you scale for higher parallelism? |

---

## 8. Known Limitations

These limitations are documented honestly in the repository. A reviewer who asks about them should find clear acknowledgement in the source:

| Limitation | Where documented |
|---|---|
| 5 page objects without scenario coverage | [KNOWN_AUT_LIMITATIONS.md](KNOWN_AUT_LIMITATIONS.md), [QUALITY_RISK_ASSESSMENT.md](QUALITY_RISK_ASSESSMENT.md) |
| Chrome only in CI | [CI_CD_GUIDE.md](CI_CD_GUIDE.md), [QUALITY_RISK_ASSESSMENT.md](QUALITY_RISK_ASSESSMENT.md) (QR-011) |
| No Grid in CI | [CI_CD_GUIDE.md](CI_CD_GUIDE.md) — requires Docker on runner |
| AUT is shared public demo | [TEST_STRATEGY.md](TEST_STRATEGY.md), [PORTFOLIO_OVERVIEW.md](PORTFOLIO_OVERVIEW.md) |
| No registration data cleanup | [QUALITY_RISK_ASSESSMENT.md](QUALITY_RISK_ASSESSMENT.md) (QR-013) |
| No license | Noted in this document and phase review |
| Portfolio context only | Stated throughout — not a commercial or production deployment |

---

## 9. Commit History as Evidence

The `git log` is itself portfolio evidence. Each commit represents a validation phase with a corresponding review report in `docs/review/`:

| Commit message prefix | Phase | Evidence |
|---|---|---|
| `docs: add quality risk assessment` | Phase 7 — Risk assessment | `docs/review/PHASE_7_QUALITY_RISK_ASSESSMENT.md` |
| `docs: add contributor governance` | Phase 8 — Contributor governance | `docs/review/PHASE_8_CONTRIBUTOR_GOVERNANCE.md` |
| `infra: improve Selenium Grid startup readiness` | Phase 9 — Grid readiness | `docs/review/PHASE_9_SELENIUM_GRID_READINESS.md` |
| `ci: improve validation diagnostics` | Phase 10 — CI classification | `docs/review/PHASE_10_CI_CD_IMPROVEMENTS.md` |
| `docs: polish GitHub portfolio` | Phase 11 — Portfolio polish | `docs/review/PHASE_11_GITHUB_PORTFOLIO_POLISH.md` |

Each review report documents the exact change, evidence, validation result, and regression confirmation.
