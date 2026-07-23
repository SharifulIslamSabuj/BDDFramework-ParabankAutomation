# Quality Risk Assessment

**Framework:** ParaBank BDD Automation  
**Application Under Test:** ParaBank Online Banking — [parabank.parasoft.com](https://parabank.parasoft.com/parabank/)  
**Assessment base commit:** `96fa5a7` (pre-Phase 7 baseline)  
**Assessment date:** 2026-07-18

---

## Executive Summary

The framework is in a validated, stable state. Across all executed phases, no unexpected
framework regression was introduced. The test suite produces 18 Cucumber scenario executions
with 12 passing and 6 consistently failing against the public ParaBank demo. All 6 failures
are confirmed AUT limitations, not framework defects.

The primary quality risks affecting this repository fall into two categories:

1. **External dependency risks** — the public ParaBank demo server is shared, uncontrolled,
   and exhibits documented intermittent instability. All 6 known failures originate here.
2. **Infrastructure and configuration risks** — several low-impact technical-debt items exist
   (orphaned configuration keys, an unused constant, no dedicated `production.properties` alias
   file) that have no current functional impact but carry low maintenance risk.

No high or critical framework risks are currently open. The production-write guard (Phase 4)
mitigates the most significant operational risk. Documentation is synchronized with the
implementation (Phase 6).

---

## Scope

This assessment covers:

- Cucumber BDD scenario execution (18 executions across 2 feature files)
- Framework test infrastructure (hooks, driver management, configuration, API client, page objects)
- Production safety controls
- Environment and configuration model
- Docker and Selenium Grid infrastructure
- GitHub Actions CI pipeline
- Test data management
- Technical and test debt

Out of scope:
- Backend API contract testing
- Database-level assertions
- Performance and load testing
- Visual regression testing
- Mobile browser testing
- The ParaBank AUT itself (not owned by this repository)

---

## Assessment Method

Evidence sources used:

- Current feature files (scenario titles, tags, assertions, examples)
- Current Java source (hooks, page objects, utilities, configuration, constants)
- Current build configuration (`build.gradle`)
- Current properties files (all 5 environment configs)
- Current Docker and Grid files
- Current GitHub Actions workflow
- Committed phase reports (Phase 0 through Phase 6)
- Validated test execution results (multiple runs, documented in phase reports)

Risk ratings follow the model defined in the Risk Rating Model section. Engineering judgement
is applied where evidence modifies the matrix-derived rating, and the rationale is stated.

---

## Risk Rating Model

| Likelihood | Impact | Suggested rating |
|---|---|---|
| Low | Low | Low |
| Low | Medium | Low |
| Low | High | Medium |
| Low | Critical | High |
| Medium | Low | Low |
| Medium | Medium | Medium |
| Medium | High | High |
| Medium | Critical | Critical |
| High | Low | Medium |
| High | Medium | High |
| High | High | Critical |
| High | Critical | Critical |

---

## Current Quality Baseline

| Metric | Value |
|---|---|
| Cucumber executions | 18 |
| Passed | 12 |
| Known AUT failures | 6 |
| Unexpected failures | 0 |
| Focused safety tests | 12/12 |
| Total Gradle tests | 30 |
| Production write guard | Implemented and tested |
| Documentation | Synchronized with implementation |
| Validation commit | `96fa5a7` |

---

## Risk Register

| ID | Category | Risk | Likelihood | Impact | Rating | Current control | Status |
|---|---|---|---|---|---|---|---|
| QR-001 | External Dependency | ParaBank public demo instability | High | Medium | High | Documented; one-rerun protocol | Outside repository control |
| QR-002 | AUT Behaviour | Login injection scenarios do not produce expected error state | High | Low | Medium | Documented as AUT-LIM-001; scenarios preserved | Outside repository control |
| QR-003 | AUT Behaviour | Registration does not produce expected authenticated session | High | Low | Medium | Documented as AUT-LIM-002; scenarios preserved | Outside repository control |
| QR-004 | Test Data | Test-data accumulation on shared public AUT | Medium | Medium | Medium | API idempotency; "already exists" treated as success | Accepted |
| QR-005 | Configuration | `production` alias has no dedicated properties file | Low | Medium | Low | Falls back to `config.properties`; guard prevents writes | Accepted |
| QR-006 | Configuration | Orphaned `implicitWait` property in 5 config files | Low | Low | Low | No loader reads it; harmless | Accepted |
| QR-007 | Configuration | ~~`POLLING_INTERVAL_MILLIS` constant — no active caller~~ | Low | Low | Low | Resolved — constant removed after repository-wide dead-code audit confirmed zero callers | Resolved |
| QR-008 | Infrastructure | Selenium Grid — `selenium-tests` depends on `chrome-node` without health-check gate | Low | Medium | Low | Health-check cascade implemented in Phase 9; `chrome-node` must be healthy before `selenium-tests` starts | Mitigated |
| QR-009 | CI/CD | Known failures cause GitHub Actions job to report failure status | High | Low | Medium | Result classifier distinguishes known baseline from unexpected regression; job is green for VALIDATED_BASELINE | Mitigated |
| QR-010 | Security | Production write guard scope — read-only scenario writes not covered | Low | Low | Low | Guard targets setup writes only; documented limitation | Accepted |
| QR-011 | Test Design | Limited browser coverage — Chrome only in CI | Medium | Low | Low | Local execution supports Firefox/Edge; CI scoped to Chrome | Deferred |
| QR-012 | Maintainability | `DEFAULT_USER_SETUP_DONE` is JVM-static; cannot reset between test-class runs in same JVM | Low | Low | Low | Only one Cucumber suite per JVM; not a parallel concern | Accepted |
| QR-013 | Test Data | Registration data written to shared server; no cleanup mechanism | Medium | Low | Low | Public AUT does not offer cleanup API; idempotent approach used | Accepted |

---

## Detailed Risk Records

### QR-001 — ParaBank Public Demo Instability

**Category:** External Dependency  
**Likelihood:** High — observed twice in validated execution history  
**Impact:** Medium — causes transient additional failures that may be mistaken for regression  
**Rating:** High (engineering-adjusted from Critical: impact is temporary and self-resolving)

**Description:**  
The public ParaBank demo at `parabank.parasoft.com` is a shared instance with no SLA. Documented
events (Phase 3 first run: 18/5/13; Phase 5 first run: extra `runScenario[16]` failure) show
that the server can enter a degraded state affecting scenarios that normally pass. In both
documented cases, the instability resolved within minutes without framework changes.

**Evidence:**  
- Phase 3 run #1: 18/5/13 — all non-empty invalid-credential scenarios produced `p.error`
  absent (not just the 3 injection probes). Confirmed transient; next run: 18/12/6.
- Phase 5 run #1: `runScenario[16]` failed. Confirmed transient; next run: 18/12/6.

**Current controls:**  
Documented rerun protocol: one additional run before classifying any extra failure as a
framework regression.

**Residual risk:**  
If the AUT enters a prolonged degraded state, CI runs will consistently fail until the server
recovers. No framework control exists for this.

**Recommended action:**  
Monitor. If extended outages become frequent, consider configuring a private ParaBank deployment
for CI use.

**Owner:** Outside repository control  
**Status:** Outside repository control

---

### QR-002 — Login Injection Scenarios Do Not Produce Expected Error State

**Category:** AUT Behaviour  
**Likelihood:** High — fails every run without exception  
**Impact:** Low — known, documented, does not hide a framework defect  
**Rating:** Medium

**Description:**  
Three executions of "Sign-in is protected against injection and scripting attacks" consistently
fail because `p.error` is not rendered by the AUT for SQL-like and XSS-like inputs. See
`docs/KNOWN_AUT_LIMITATIONS.md#AUT-LIM-001` for full analysis.

**Current controls:**  
AUT-LIM-001 documented. Failure counted in baseline. No suppression.

**Recommended action:**  
No framework action. Monitor AUT behaviour. If the AUT is updated to render the expected error
element, these scenarios will pass automatically.

**Owner:** Outside repository control  
**Status:** Outside repository control

---

### QR-003 — Registration Does Not Produce Expected Authenticated Session

**Category:** AUT Behaviour  
**Likelihood:** High — fails every run without exception  
**Impact:** Low — known, documented, does not hide a framework defect  
**Rating:** Medium

**Description:**  
Three positive registration scenarios fail because the AUT does not redirect to an authenticated
overview page after form submission on the shared public demo. See
`docs/KNOWN_AUT_LIMITATIONS.md#AUT-LIM-002` for full analysis.

**Current controls:**  
AUT-LIM-002 documented. Failure counted in baseline. No suppression.

**Recommended action:**  
No framework action. Monitor AUT behaviour. Running against a private ParaBank deployment would
eliminate shared-state issues.

**Owner:** Outside repository control  
**Status:** Outside repository control

---

### QR-004 — Test-Data Accumulation on Shared Public AUT

**Category:** Test Data  
**Likelihood:** Medium — each `Hooks.ensureDefaultTestUserExists()` run may attempt registration  
**Impact:** Medium — accumulated data could cause throttling or quota behaviour over time  
**Rating:** Medium

**Description:**  
The `tryApiRegistration()` method in `Hooks` posts to ParaBank's registration endpoint before
each suite. The `ParaBankApiClient` correctly handles "This username already exists" as a
success condition (`registerUser()` returns `true`). However, the positive registration
scenarios (AUT-LIM-002) may submit different generated usernames on each run, accumulating
accounts on the shared server with no cleanup mechanism.

**Evidence:**  
`tryBrowserRegistration()` uses `LoremIpsum`-generated unique data. The Excel DDT scenario
reads from `ddt.xlsx`. Neither path cleans up after registration.

**Current controls:**  
- `sqa` user registration is idempotent (existing account is detected)
- `ParaBankApiClient.registerUser()` uses "already exists" as success
- Positive registration scenarios are known to fail (accumulation does not affect suite outcome)

**Residual risk:**  
Over time, thousands of accounts may accumulate on the public server. If the server begins
rejecting new registrations due to quota limits, the `sqa` setup would also eventually fail.

**Recommended action:**  
Accept for now. If a private ParaBank deployment is introduced, implement cleanup or use
unique-prefix username strategies.

**Owner:** Framework maintainer (deferred to future phase)  
**Status:** Accepted

---

### QR-005 — `production` Alias Has No Dedicated Properties File

**Category:** Configuration  
**Likelihood:** Low — only triggered if `-Denv=production` is used  
**Impact:** Medium — config fallback to `config.properties` may use unexpected values  
**Rating:** Low

**Description:**  
`ConfigManager` recognises both `prod` and `production` as production aliases and the
`guardAgainstProductionWrite()` guard protects both. However, while `prod.properties` exists
and is fully configured, there is no `production.properties` file. A run with `-Denv=production`
would load `config.properties` (the fallback), which points to the same ParaBank demo URL but
may carry QA-oriented default values.

**Evidence:**  
Phase 4 report confirmed the alias gap was addressed for the guard but `production.properties`
was not created (outside minimum-change scope).

**Current controls:**  
Production write guard blocks automatic setup writes for both `prod` and `production`.

**Residual risk:**  
URL, timeout, browser, and parallel settings in `config.properties` may differ from intended
production settings if `-Denv=production` is used directly.

**Recommended action:**  
Create `production.properties` as a copy of `prod.properties`, or document clearly that
`-Denv=prod` is the supported production alias.

**Owner:** Framework maintainer  
**Status:** Accepted

---

### QR-008 — Selenium Grid: `selenium-tests` Depends on `chrome-node` Without Health Check

**Category:** Infrastructure  
**Likelihood:** Low — only affects Grid execution path  
**Impact:** Medium — tests may start before the Chrome Node is registered with the Hub  
**Rating:** Low

**Description:**  
Previously, `docker-compose.grid.yml` started `selenium-tests` on `chrome-node` container
start only (no health-check condition), creating a startup race where tests could begin before
the Node registered with the Hub.

**Phase 9 mitigation (2026-07-18):**  
Two changes were made to `docker-compose.grid.yml`:

1. **Hub health check corrected** — the original check (`/wd/hub/status | grep '"ready":true'`)
   caused a deadlock in Selenium 4: the Hub reports `ready: false` until a node registers, but
   the Node cannot start until the Hub is healthy. Fixed by checking only HTTP liveness:
   `curl -f http://localhost:4444/status` (returns 200 as soon as the Hub process starts).

2. **Chrome-node health check added** — the node's own `/status` endpoint at port 5555
   returns HTTP 200 only after successful hub registration. Health check:
   `curl -f http://localhost:5555/status`. `selenium-tests` now depends on
   `chrome-node: condition: service_healthy`, guaranteeing a registered node before tests start.

3. **`scripts/wait-for-grid.ps1` added** — for host-side execution (running `./gradlew`
   directly rather than via Docker Compose), the script polls `http://localhost:4444/status`
   until `ready: true` and at least one node is registered, or timeout (default 120s).

**Evidence:**  
- Grid started and both containers reached `(healthy)` state in ~60s
- `http://localhost:4444/status` confirmed: `ready: true`, 1 node, 2 Chrome slots
- Focused smoke test (1 scenario) passed via RemoteWebDriver
- Full Grid regression: 30 tests, 12 safety tests passed, 18 Cucumber executions,
  12 passed, 6 known AUT failures, 0 unexpected failures

**Residual risk:**  
Chrome-node health check verifies HTTP 200 from `/status`. Between the health check passing
and the first test, a node crash would produce a session error. This window is negligible
in practice and cannot be eliminated without retrying at the session level.

**Owner:** Phase 9 — implemented  
**Status:** Mitigated

---

### QR-009 — Known Failures Cause CI Job to Report Non-Zero Exit Status

**Category:** CI/CD  
**Likelihood:** High — Gradle exits non-zero on every run due to 6 known AUT failures  
**Impact:** Low — accurately reflects AUT limitations; not a false negative  
**Rating:** Medium

**Description:**  
The GitHub Actions workflow runs `./gradlew clean test`. Because 6 Cucumber scenarios fail,
TestNG reports failures and Gradle exits with a non-zero code. Before Phase 10, this caused
the CI job to permanently show a red badge regardless of whether the failures were the
expected known-AUT set or a new unexpected regression.

**Evidence:**  
Gradle exits with `BUILD FAILED in 2m 26s` and `30 tests completed, 6 failed` on every
validated run. The CI workflow uses `if: always()` for artifact upload to ensure reports are
available regardless of job outcome.

**Phase 10 mitigation — result classification (original):**  
`scripts/analyze-test-results.sh` parsed `build/test-results/test/TEST-*.xml` and compared
the failing TestNG `runScenario[N]` indices to the accepted baseline
`{runScenario[9,10,11,13,14,15]}`.

**Identity-instability finding (post-Phase 10, pre-v1.1.1):**  
Direct inspection of a locally generated JUnit XML file showed that `runScenario[N]` is
assigned by TestNG data-provider invocation order, not by scenario content. Under
`dataproviderthreadcount` > 1 (Gradle's own default is 2), the same index was observed
reused for two different scenarios, with indices assigned out of declaration order. CI
avoided the symptom only because it forces `dataproviderthreadcount=1`/`maxParallelForks=1`
— a setting documented elsewhere as a CPU-contention mitigation for 2-vCPU runners
(`docs/TEST_STRATEGY.md`), not as a guarantee of classifier stability. No prior document
recorded this as an intentional, verified property. See `docs/CI_CD_GUIDE.md` §4 for the
full analysis.

**Mitigation (current) — Cucumber JSON element `id`:**  
The classifier now identifies failed scenarios using the Cucumber JSON element-level `id`
field from `build/reports/cucumber/cucumber-report.json` (already produced by the `json:`
formatter declared in `TestRunner.java` — no Java/TestRunner/Hooks/listener/plugin change
was required). This id is derived from the Gherkin document (feature + scenario name +
outline row ordinal) and does not depend on TestNG thread count, execution order, or
invocation index. JUnit XML remains the source of truth for execution counts and the
production-safety gate; its counts are cross-checked against the Cucumber JSON element
counts, and any disagreement is treated as untrustworthy rather than resolved silently.

| Classification | CI outcome |
|---|---|
| `VALIDATED_BASELINE` — exactly the 6 known failures (by `id`) | **Job green** |
| `UNEXPECTED_REGRESSION` — different/additional failure IDs, count mismatch, or safety-test failure | **Job red** |
| `INFRASTRUCTURE_FAILURE` — Cucumber suite did not execute | **Job red** |
| `RESULTS_UNAVAILABLE` — no JUnit XML found; or Cucumber JSON missing/malformed/incomplete; or JSON/XML counts disagree | **Job red** |

The classifier is the final step in the pipeline. It writes a structured summary to the
GitHub Actions job summary panel on every run, including expected/actual/unexpected/missing
IDs with scenario name and feature `uri` for diagnostics.

**Residual risk:**  
The classifier reads the accepted failure set as a hard-coded `frozenset` of `id` strings.
If one of the six tracked scenarios' own name text is edited, or its outline rows are
reordered, the `id` changes and the baseline must be updated. Unrelated edits elsewhere in
a feature file (new scenarios, comments, reordering *other* scenarios) do not affect these
IDs.

**Recommended action:**  
When the expected Cucumber execution count or the known-failure set changes, update
`KNOWN_FAILURE_IDS` and `EXPECTED_CUCUMBER` in `scripts/analyze-test-results.sh`, following
the revalidation guidance in `docs/CI_CD_GUIDE.md` §4.

**Owner:** Framework maintainer  
**Status:** Mitigated (Phase 10; identity mechanism replaced pre-v1.1.1)

---

## Accepted Risks

| ID | Risk | Why accepted | Current impact | Trigger for reassessment |
|---|---|---|---|---|
| QR-002 | Login injection — no error state | AUT limitation; no framework action possible | 3 consistent failures in baseline | AUT updated to render error element |
| QR-003 | Registration — no authenticated session | AUT limitation; no framework action possible | 3 consistent failures in baseline | AUT updated or private deployment used |
| QR-004 | Test data accumulation | No cleanup API on public AUT; idempotent registration handles sqa user | No current functional impact | Server quota or throttling observed |
| QR-005 | `production` alias — no `.properties` file | Guard protects writes; one-line config change if needed | Low probability of use | `-Denv=production` is used in CI or production workflow |
| QR-006 | Orphaned `implicitWait` property | No code reads it; harmless; removing 5 files offered zero benefit | None | Another review phase targets configuration files |
| QR-009 | CI red badge on known failures | **Mitigated** — result classifier distinguishes known from unexpected using stable Cucumber JSON element `id` matching (replaced unstable `runScenario[N]` index matching pre-v1.1.1); job is green for VALIDATED_BASELINE | No unexpected regression masked | `KNOWN_FAILURE_IDS` updated when one of the 6 tracked scenarios' name/rows changes or the baseline changes |
| QR-010 | Production guard scope (setup writes only) | Scenario-step writes to AUT are intentional test actions; guard targets automated setup | Clearly documented scope | Guard expansion required by new write-capable hooks |
| QR-012 | Static `DEFAULT_USER_SETUP_DONE` flag | One Cucumber suite per JVM; not a parallel or multi-suite concern | No observed issue | Multiple suite classes in same JVM introduced |
| QR-013 | No registration cleanup | Public AUT has no cleanup API; idempotent handling in place | No observed impact | Server-side quota or rejection observed |

---

## Technical Debt

| ID | Item | Evidence | Current impact | Priority | Proposed phase |
|---|---|---|---|---|---|
| TD-001 | Orphaned `implicitWait` property in 5 config files | Phase 5 report; `getImplicitWait()` removed but keys retained | None — no code reads the key | Low | Future configuration cleanup phase |
| TD-002 | ~~`POLLING_INTERVAL_MILLIS` — declared, zero callers confirmed~~ | Resolved — removed from `TimeoutConstants.java` after a repository-wide dead-code audit confirmed zero callers | None | — | Resolved |
| TD-003 | `production` alias has no dedicated `.properties` file | Phase 4 report; `production.properties` not created (outside Phase 4 scope) | Low — falls back to `config.properties` | Low | Dedicated configuration file phase |
| TD-004 | ~~Selenium Grid `chrome-node` has no health-check gate~~ | Resolved in Phase 9 — health-check cascade implemented; `scripts/wait-for-grid.ps1` added | None | — | Resolved |
| TD-005 | No test-data cleanup for positive registration scenarios | `Hooks.tryBrowserRegistration()` and `ExcelDataProvider` generate unique usernames with no delete operation | Low — accumulates on public AUT only; no private deployment | Low | Private deployment or cleanup phase |

---

## Test Debt

| ID | Gap | Classification | Rationale |
|---|---|---|---|
| TestD-001 | Cross-browser execution (Firefox, Edge) in CI | Deferred | Local execution supports Firefox/Edge; CI scoped to Chrome; multi-browser CI requires matrix and additional runner cost |
| TestD-002 | Grid-specific integration test (validates RemoteWebDriver path) | Deferred | Grid execution validated manually in Docker; automated Grid health test would require a running Hub in CI |
| TestD-003 | Unknown/invalid environment name handling | Deferred | `ConfigManager` falls back to `config.properties` for unknown env names; behaviour is predictable; formal test not implemented |
| TestD-004 | Account management feature coverage | Deferred | `OpenNewAccountPage`, `OpenedAccountPage`, `RequestLoanPage`, `ApprovedLoanPage`, `UpdateProfilePage` — page objects complete; feature files not yet written |
| TestD-005 | Profile management feature coverage | Deferred | `UpdateProfilePage` page object complete; feature file not yet written |
| TestD-006 | API contract/schema tests | Out of scope by design | Framework uses HTTP POST for setup only; formal REST Assured or schema validation not in scope |
| TestD-007 | Accessibility testing | Out of scope by design | ParaBank is a demo application; accessibility compliance is not a current scope item |
| TestD-008 | Performance and load testing | Out of scope by design | Not a BDD framework concern |
| TestD-009 | Reporting path and content verification | Deferred | ExtentReports and Allure content validated manually; automated assertion of report content not implemented |
| TestD-010 | CI-specific execution validation (headless + Xvfb) | Not required | CI pipeline runs on merge; Xvfb path validated by passing CI runs; no dedicated test needed |

---

## Risks Outside Repository Control

| Risk | Description | Owner |
|---|---|---|
| ParaBank AUT availability | Public demo may be unavailable, degraded, or changed at any time | ParaBank maintainers |
| AUT input-handling behaviour (AUT-LIM-001) | Server does not render error element for specific inputs | ParaBank maintainers |
| AUT session behaviour after registration (AUT-LIM-002) | Server does not establish authenticated session on shared demo | ParaBank maintainers |
| Chrome browser compatibility | WebDriverManager handles driver matching; Chrome binary updates are outside repository control | Browser vendor / GitHub runner |
| GitHub Actions runner availability | CI runs depend on GitHub-hosted runner availability | GitHub |

---

## Mitigation Roadmap

| Phase | Focus | Risks addressed |
|---|---|---|
| Phase 7 (current) | Quality risk documentation | All risks identified and classified |
| Future: Configuration cleanup | Remove orphaned `implicitWait` keys; create `production.properties` | TD-001, TD-003, QR-005 |
| ~~Future: Dead code review~~ | ~~Classify and remove `POLLING_INTERVAL_MILLIS` if confirmed dead~~ | Completed — TD-002 resolved, QR-007 resolved |
| ~~Future: Grid readiness~~ | ~~Add Chrome Node health check in `docker-compose.grid.yml`~~ | Completed in Phase 9 — TD-004 resolved, QR-008 mitigated |
| Future: Coverage expansion | Feature files for 5 implemented page objects | TestD-004, TestD-005 |
| Future: Private deployment | Eliminate shared-server data accumulation and AUT-LIM-002 | QR-004, TD-005, QR-003 |

---

## Release Interpretation

### What 12 passed / 6 failed means

A run showing exactly 18 executions with 12 passed and 6 failed is a **successful framework
validation result**, not an indication of framework instability.

The 6 failures represent:
- The AUT's input-handling behaviour for security-oriented login inputs (3 scenarios)
- The AUT's session handling after registration on the shared public demo (3 scenarios)

Neither group indicates a framework defect.

### What constitutes a regression

A regression is defined as: **a new or changed failure outside the accepted 6-failure set**,
confirmed across at least two consecutive runs, traceable to a framework source change.

Specific indicators:
- A scenario that previously passed now fails AND a framework source change exists
- A scenario that previously passed now fails AND the failure pattern does not match AUT-LIM-003

Specific non-indicators (not regressions):
- All 6 known failures failing (baseline)
- A 7th transient failure that resolves on rerun (AUT-LIM-003)
- Gradle reporting `BUILD FAILED` with exactly 6 failures

### Build exit behaviour

Gradle exits with a non-zero code (`BUILD FAILED`) when test failures exist. This is accurate
and correct behaviour. The 6 known failures will always produce a non-zero exit code unless:
1. The AUT is fixed or a private deployment is used, OR
2. Known failures are suppressed (explicitly not recommended in this repository)

CI consumers should interpret a Gradle failure with exactly 6 reported failures as the
established baseline, not as a framework defect.

---

## Review and Reassessment Guidance

This assessment should be reviewed when:

| Event | Action |
|---|---|
| AUT behaviour changes (more or fewer than 6 failures) | Update AUT limitation records; reassess QR-002, QR-003 |
| New framework phase introduces code changes | Re-run full regression; compare result to baseline |
| Docker/Grid path is used in CI | Reassess QR-008; validate Chrome Node health-check behaviour |
| Private ParaBank deployment introduced | Close QR-001, QR-003, QR-004; revise TD-005 |
| Cross-browser CI matrix added | Close TestD-001 |
| Additional hooks or setup operations introduced | Re-evaluate QR-010 (production guard scope) |
