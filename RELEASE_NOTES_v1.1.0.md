# BDDFramework-ParabankAutomation v1.1.0

**Release date:** 2026-07-19  
**Branch:** `main`  
**Tag:** `v1.1.0`  
**Previous release:** v1.0.0 (2026-06-20)

---

## Release Summary

v1.1.0 consolidates the portfolio framework with production-write safety enforcement, Selenium Grid startup reliability, CI result classification, quality risk documentation, contributor governance, and a fully restructured GitHub portfolio presentation.

This is a portfolio demonstration release. No commercial deployment is implied.

---

## What This Release Demonstrates

| Engineering Concern | Implementation |
|---|---|
| Production safety | `ConfigManager.guardAgainstProductionWrite()` — 12 focused safety tests |
| CI signal accuracy | Known-failure classifier distinguishes expected from unexpected results |
| Grid reliability | Health-check cascade eliminates startup race condition |
| Risk-based thinking | 13-item quality risk register with evidence and residual risk |
| Contributor readiness | Extension guide, anti-pattern catalogue, PR checklist |
| Portfolio presentation | Architecture doc, portfolio overview, interviewer guide — all evidence-based |
| Honest limitations | Six known AUT failures disclosed and classified; no suppression |

---

## Validation Evidence

| Metric | Result |
|---|---|
| Production-safety tests | 12 / 12 passed |
| Cucumber executions | 18 |
| Passed scenarios | 12 |
| Known AUT failures | 6 (`runScenario[9,10,11,13,14,15]`) |
| Unexpected failures | 0 |
| CI classification | `VALIDATED_BASELINE` |
| Local execution | Validated at release commit |
| Selenium Grid smoke | 1 / 1 passed |
| Fresh-clone validation | VALIDATED_BASELINE (confirmation run) |

**Important:** Gradle exits non-zero (`BUILD FAILED`) when any test fails — including the six known AUT failures. This is expected. The CI classifier is the authoritative gate: `VALIDATED_BASELINE` means the run produced exactly the accepted known-failure set and no unexpected deviation.

---

## Major Engineering Improvements Since v1.0.0

### Production Write Safety (Phases 7–8)

`ConfigManager.guardAgainstProductionWrite()` is called before both write paths (API and browser registration) in `Hooks.ensureDefaultTestUserExists()`. Throws `ConfigurationException` on `prod` or `production` (case-insensitive). Validated by `ProductionSafetyGuardTest` — 12 focused tests, no browser or network dependency.

### Selenium Grid Startup Reliability (Phase 9)

The v1.0.0 Grid configuration started the test container as soon as the Chrome Node container started, before the node had registered with the Hub. This caused tests to start against a Grid with no available sessions.

v1.1.0 introduces a health-check cascade in `docker-compose.grid.yml`:
1. Hub HTTP liveness check
2. Chrome Node registration check against the Hub API
3. Test container start only after both are confirmed healthy

`scripts/wait-for-grid.ps1` provides a standalone readiness poller with configurable timeout, structured diagnostic output, and non-zero exit on timeout.

### CI Result Classification (Phase 10)

v1.0.0 CI had no way to distinguish the six known AUT failures from genuine regressions. Every run produced a red badge regardless of framework correctness.

v1.1.0 introduces `scripts/analyze-test-results.sh` — a Python 3 JUnit XML classifier that produces one of four outcomes:

| Classification | Meaning | Job result |
|---|---|---|
| `VALIDATED_BASELINE` | Exactly `{9,10,11,13,14,15}` failed | Green |
| `UNEXPECTED_REGRESSION` | Different failures observed | Red |
| `INFRASTRUCTURE_FAILURE` | Zero Cucumber tests ran | Red |
| `RESULTS_UNAVAILABLE` | No JUnit XML produced | Red |

The CI pipeline now runs as three sequential gates — Compile → Safety Tests → Full Regression — before the classifier runs with `if: always()`.

### Quality Risk and Governance (Phases 7–8)

`docs/QUALITY_RISK_ASSESSMENT.md` documents 13 risks with likelihood, impact, mitigation, and residual risk. `docs/KNOWN_AUT_LIMITATIONS.md` records AUT-LIM-001 and AUT-LIM-002 with reproduction steps. `docs/FRAMEWORK_EXTENSION_GUIDE.md` catalogues 11 named anti-patterns and a step-by-step extension workflow. `docs/PULL_REQUEST_CHECKLIST.md` provides contributor validation gates.

### Portfolio Presentation (Phase 11)

`README.md` was restructured from ~1,087 to ~500 lines with a clear positioning statement, validation-status table, Mermaid architecture diagram, and an 11-document navigation map. Three new portfolio documents were added: `docs/ARCHITECTURE.md` (15 sections, Mermaid diagrams), `docs/PORTFOLIO_OVERVIEW.md` (5 engineering problems solved), and `docs/INTERVIEWER_GUIDE.md` (Q&A with evidence pointers).

---

## Execution Options

**Local (Chrome):**
```bash
./gradlew clean compileTestJava
./gradlew test --tests "*ProductionSafetyGuardTest"
./gradlew clean test
```

**Docker (no local Java required):**
```bash
docker compose build
docker compose up
```

**Selenium Grid:**
```bash
docker compose -f docker-compose.grid.yml up -d selenium-hub chrome-node
.\scripts\wait-for-grid.ps1
.\gradlew clean test "-DseleniumGridEnabled=true" "-DgridUrl=http://localhost:4444/wd/hub"
docker compose -f docker-compose.grid.yml down -v --remove-orphans
```

---

## CI Behaviour

The GitHub Actions workflow at `.github/workflows/automation-test.yml` runs on push to `main`, pull requests to `main`, and `workflow_dispatch` (QA / staging / UAT — production is intentionally excluded).

A green CI badge means `VALIDATED_BASELINE`: exactly the six known AUT failures were observed and no other deviations exist. Any unexpected deviation fails the job regardless of Gradle exit code.

---

## Known AUT Limitations

| Scenario | Index | Root cause |
|---|---|---|
| Login with special characters | 9 | AUT sanitises injection attempts without producing an assertion-catchable error element |
| Login with SQL injection patterns | 10 | Same AUT-LIM-001 |
| Login with overlong username | 11 | Same AUT-LIM-001 |
| Registration with duplicate email | 13 | AUT server-side behaviour |
| Registration with invalid data | 14 | Same AUT-LIM-002 |
| Registration session redirect | 15 | AUT does not redirect to overview after registration in all states |

Six failures are `VALIDATED_BASELINE`. Zero additional failures are expected.

See [`docs/KNOWN_AUT_LIMITATIONS.md`](docs/KNOWN_AUT_LIMITATIONS.md) for full records.

---

## Known Technical Limitations

| Limitation | Detail |
|---|---|
| Chrome only in CI | GitHub-hosted runners support Chrome; Firefox/Edge are not in the CI pipeline |
| No Grid in CI | Selenium Grid requires Docker; GitHub-hosted runners do not run the Grid stack |
| No test-data cleanup | Registration accumulates on the shared public AUT |
| 5 page objects without scenarios | Page objects are implemented but not yet covered by feature files |
| No LICENSE file | Public reuse terms are not specified |
| AUT is uncontrolled | `parabank.parasoft.com` is a shared public demo; intermittent server instability is documented |

---

## Documentation

| Document | Purpose |
|---|---|
| [README.md](README.md) | Project overview and quick start |
| [CHANGELOG.md](CHANGELOG.md) | This changelog |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to extend the framework |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Framework design and execution flows |
| [docs/PORTFOLIO_OVERVIEW.md](docs/PORTFOLIO_OVERVIEW.md) | Recruiter and reviewer summary |
| [docs/INTERVIEWER_GUIDE.md](docs/INTERVIEWER_GUIDE.md) | Structured technical review path |
| [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md) | Test scope, tags, and known failure baseline |
| [docs/CI_CD_GUIDE.md](docs/CI_CD_GUIDE.md) | CI pipeline stages and result classification |
| [docs/SELENIUM_GRID_GUIDE.md](docs/SELENIUM_GRID_GUIDE.md) | Grid setup and execution |
| [docs/QUALITY_RISK_ASSESSMENT.md](docs/QUALITY_RISK_ASSESSMENT.md) | Risk register and engineering decisions |
| [docs/KNOWN_AUT_LIMITATIONS.md](docs/KNOWN_AUT_LIMITATIONS.md) | Why 6 scenarios consistently fail |
| [docs/FRAMEWORK_EXTENSION_GUIDE.md](docs/FRAMEWORK_EXTENSION_GUIDE.md) | Extension rules and anti-patterns |
| [docs/RELEASE_VALIDATION.md](docs/RELEASE_VALIDATION.md) | Full v1.1.0 release validation record |

---

## Upgrade Notes

Upgrading from v1.0.0:

1. No changes to Java source code affecting test execution behaviour.
2. `build.gradle` project version changed from `1.0.0` to `1.1.0`.
3. `docker-compose.grid.yml` health checks may change startup timing. Allow 60–90 seconds for Grid readiness.
4. `scripts/wait-for-grid.ps1` and `scripts/analyze-test-results.sh` are new — ensure they are executable.
5. The GitHub Actions workflow now requires `GRADLE_EXIT_CODE` to be set before the classifier; the workflow sets this automatically via `$GITHUB_OUTPUT`.

No AUT credentials, locators, or scenario structures changed.

---

## Release Integrity

| Check | Result |
|---|---|
| Working tree clean before release | Yes |
| No uncommitted local state required | Confirmed via fresh clone |
| No generated reports committed | Confirmed |
| No temporary fixture committed | Confirmed |
| No Java code changed | Confirmed |
| No test logic changed | Confirmed |
| Fresh-clone compile | BUILD SUCCESSFUL |
| Fresh-clone safety tests | 12/12 PASSED |
| Fresh-clone regression | 18/12/6 VALIDATED_BASELINE (confirmation run) |
| Grid smoke | 1/1 PASSED |
| Docker cleaned | Confirmed |
| All local links valid | Confirmed |
| No forbidden claims | Confirmed |
| Annotated tag | `v1.1.0` created after release commit |

*Full validation record: [`docs/RELEASE_VALIDATION.md`](docs/RELEASE_VALIDATION.md)*
