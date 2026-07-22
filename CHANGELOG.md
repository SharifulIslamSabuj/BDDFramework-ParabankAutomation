# Changelog

All notable changes to this project are documented in this file.

The format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.1] — 2026-07-22

This is a patch release. It fixes a CI reliability issue and a CI classifier identity-matching
defect, and completes a portfolio documentation reorganization and terminology synchronization.
No application behaviour, test scenarios, or public framework API changed since v1.1.0.

### Fixed

- **CI window handling** — `DriverFactory` called `window().maximize()`, which on GitHub-hosted
  Ubuntu runners (Xvfb, no window manager) routes through a CDP `Runtime.evaluate` call that
  cannot be answered, throwing `unknown error: JavaScript code failed`. Replaced with
  `window().setSize(new Dimension(1920, 1080))`, which uses the W3C Set Window Rect command
  directly and works identically on local displays and Xvfb.
- **CI result classifier identity matching** — `scripts/analyze-test-results.sh` previously
  matched failed scenarios by their TestNG-generated `runScenario[N]` name, which is assigned by
  data-provider invocation order rather than scenario content. Under `dataproviderthreadcount` > 1
  (Gradle's own default is 2), the same index was directly reproduced being reused for two
  different scenarios in the same run, with indices assigned out of declaration order. The
  classifier now matches failed scenarios by the Cucumber JSON report's stable element-level
  `id` (feature + scenario name + outline row ordinal), which does not depend on TestNG thread
  count, execution order, or invocation index. JUnit XML remains the source of truth for
  execution counts and the production-safety gate; a material disagreement between the JUnit XML
  and Cucumber JSON counts is now treated as untrustworthy (`RESULTS_UNAVAILABLE`) rather than
  resolved silently.

### Changed

- **`.github/workflows/automation-test.yml`** — Added a `cucumber-reports-<env>-run<N>` artifact
  upload step for `build/reports/cucumber/`, since the Cucumber JSON report is now the classifier's
  identity source of truth and is otherwise not collected as CI evidence.
- **Documentation reorganized into a recruiter-facing structure** — `docs/` restructured into
  `architecture/`, `guides/`, and `quality/` categories (see Removed, below, for what was cleared
  out to make this a clean surface). All internal cross-links and the `README.md` / `CONTRIBUTING.md`
  navigation tables were repaired to the new paths.
- **Documentation terminology synchronized with the current classifier** — `README.md`,
  `docs/PORTFOLIO_OVERVIEW.md`, `docs/guides/FRAMEWORK_EXTENSION_GUIDE.md`,
  `docs/guides/PULL_REQUEST_CHECKLIST.md`, `docs/guides/SELENIUM_GRID_GUIDE.md`, and
  `docs/quality/TEST_STRATEGY.md` no longer describe the retired `runScenario[N]` /
  `KNOWN_FAILURE_INDICES` mechanism; all now describe `KNOWN_FAILURE_IDS` and the
  `VALIDATED_BASELINE` / `UNEXPECTED_REGRESSION` outcomes consistently.
- **`build.gradle`** — Project version updated from `1.1.0` to `1.1.1`.

### Removed

- **`docs/review/`** — all 13 point-in-time phase-by-phase development reports and baseline
  investigation records. Internal development history with no reference value to a framework user.
- **`docs/INTERVIEWER_GUIDE.md`** — its evidence trail depended entirely on the now-removed
  `docs/review/` reports and asserted stale classifier internals.
- **`docs/RELEASE_VALIDATION.md`** — a one-time v1.1.0 release-audit checklist, not evergreen
  reference material.

### Known Limitations

Unchanged from v1.1.0 — see that section below. The six known AUT failures, the public AUT's
intermittent instability (QR-001), Chrome-only CI/Grid coverage, and the absence of test-data
cleanup and a LICENSE file all remain accurate.

---

## [1.1.0] — 2026-07-19

This release consolidates the framework following the initial v1.0.0 portfolio delivery.
It adds production-write safety, Selenium Grid readiness, CI result classification, risk documentation,
contributor governance, and a complete architecture and recruiter-facing portfolio presentation.

### Added

- **Production write protection** — `ConfigManager.guardAgainstProductionWrite()` blocks automatic
  test-user creation when `env=prod` or `env=production` (case-insensitive). Both API and browser
  registration paths are guarded.
- **Production safety test suite** — `ProductionSafetyGuardTest`: 12 focused TestNG tests covering
  all `prod`/`production` aliases. No browser, no network. Runs as a required CI gate.
- **Selenium Grid readiness health checks** — Docker health-check cascade in `docker-compose.grid.yml`:
  hub liveness → chrome-node registration → test container start. Eliminates the startup race condition
  present in v1.0.0.
- **Grid readiness script** — `scripts/wait-for-grid.ps1`: PowerShell readiness poller with configurable
  timeout, clear diagnostic output, and non-zero exit on timeout.
- **CI known-failure classifier** — `scripts/analyze-test-results.sh`: Python 3 JUnit XML parser that
  classifies every run into one of four states (`VALIDATED_BASELINE` / `UNEXPECTED_REGRESSION` /
  `INFRASTRUCTURE_FAILURE` / `RESULTS_UNAVAILABLE`) and writes a structured GitHub Actions job summary.
- **CI staged gate pipeline** — Three sequential gates before classification: Compile → Safety Tests →
  Full Regression. Each gate fails the job before the next stage starts.
- **CI artifact upload with `if: always()`** — Test reports, screenshots, and Allure results are
  uploaded even when tests fail. Retention reduced to 14 days.
- **Quality risk assessment** — `docs/QUALITY_RISK_ASSESSMENT.md`: 13-risk register with detailed records,
  residual risk analysis, and technical debt inventory.
- **Known AUT limitations register** — `docs/KNOWN_AUT_LIMITATIONS.md`: detailed records for AUT-LIM-001
  (injection sanitisation) and AUT-LIM-002 (session handling), with reproduction steps.
- **Contributor governance** — `docs/FRAMEWORK_EXTENSION_GUIDE.md`: 11 named anti-patterns, layer rules,
  and a step-by-step scenario-addition workflow. `docs/PULL_REQUEST_CHECKLIST.md`.
- **Architecture documentation** — `docs/ARCHITECTURE.md`: 15-section technical reference including
  Mermaid execution-flow, provisioning-flow, and CI-classification diagrams.
- **Portfolio overview** — `docs/PORTFOLIO_OVERVIEW.md`: recruiter-oriented summary with 5 engineering
  problems solved, capability table, validation evidence, and honest limitations.
- **Interviewer guide** — `docs/INTERVIEWER_GUIDE.md`: structured 5-minute and 15-minute review paths
  with architecture, automation, CI/CD, and quality Q&A evidence pointers.
- **CI/CD guide** — `docs/CI_CD_GUIDE.md`: complete CI pipeline reference with stage table, classification
  states, local execution commands, and known limitations.
- **Phase review reports** — `docs/review/PHASE_7_*` through `docs/review/PHASE_12_*`: evidence-based
  records of each development phase with validation tables.
- **`CHANGELOG.md`** — this file.
- **`RELEASE_NOTES_v1.1.0.md`** — human-readable release summary.
- **`docs/RELEASE_VALIDATION.md`** — full release validation record.

### Changed

- **`README.md`** — Full restructure for 60–90 second recruiter comprehension: positioning statement,
  validation-status table, Mermaid architecture diagram, concise quick start, updated documentation map
  (11 entries). Removed overclaims ("enterprise-grade" → "structured"); removed ~540 lines of duplicated
  detail now covered by dedicated docs.
- **GitHub Actions workflow** — Added `permissions: contents: read`; removed `prod` from dispatch choices;
  changed `cancel-in-progress` to protect main-branch runs; reduced artifact retention from 30 to 14 days;
  added `set +e` / captured exit-code pattern on regression step; added `if: always()` classify step.
- **`docs/TEST_STRATEGY.md`** — Added Section 7 "CI Execution Stages" with four-stage table and
  classification explanation; sections renumbered.
- **`docs/QUALITY_RISK_ASSESSMENT.md`** — QR-009 status updated from Accepted to Mitigated following
  CI classification implementation.
- **`CONTRIBUTING.md`** — Added CI classification reference and baseline documentation.
- **`build.gradle`** — Project version updated from `1.0.0` to `1.1.0`.

### Fixed

- **Grid startup race condition** — Docker Compose v1.0.0 started the test container as soon as the
  Chrome Node container started, before the node had registered with the Hub. Replaced with a health-check
  cascade that waits for actual node registration.
- **CI permanently red from expected AUT failures** — v1.0.0 CI had no way to distinguish the six
  known AUT failures from genuine regressions. Every run showed a red badge. The classifier fixes this:
  runs that match exactly the accepted failure set produce a green `VALIDATED_BASELINE` result.
- **Grid readiness deadlock** — The original Grid readiness check used `grep '"ready":true'` which caused
  a deadlock on some Docker pipe configurations. Replaced with HTTP liveness check (`curl -f`).

### Security

- Automatic API and browser data writes are blocked when `env=prod` or `env=production` (case-insensitive).
  This prevents `ParaBankApiClient.registerUser()` and the browser fallback from executing against
  production aliases.
- `prod` removed from `workflow_dispatch` environment choices — CI cannot be dispatched against production.
- Workflow `permissions: contents: read` added — limits token scope to read-only.

### Known Limitations

- Six scenarios consistently fail against the public ParaBank demo AUT. These are documented server-side
  limitations (AUT-LIM-001 and AUT-LIM-002), not framework defects. The CI classifier treats them as
  `VALIDATED_BASELINE`.
- The public AUT exhibits intermittent instability beyond the six known failures (documented as QR-001).
  The accepted protocol is one confirmation rerun per transient deviation.
- Grid execution is validated with Chrome only. Firefox/Edge are not configured as Grid nodes.
- CI uses Chrome only (GitHub-hosted runners). Cross-browser CI is not implemented.
- No test-data cleanup on the public AUT. Registration accumulates.
- No LICENSE file. Reuse terms are not specified.

---

## [1.0.0] — 2026-06-20

Initial stable portfolio release. Established the BDD automation framework with Selenium WebDriver 4.40.0,
Cucumber 7.34.2, TestNG 7.12.0, Gradle 9.0.0, Java 17, Docker support, and a basic GitHub Actions
CI pipeline. Included page objects for login and registration, data-driven testing via Apache POI,
Allure and ExtentReports integration, and local browser execution via WebDriverManager.
