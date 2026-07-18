# Phase 7 — Quality Risk Assessment

**Repository:** https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation  
**Branch:** `main`  
**Starting commit:** `96fa5a7 — docs: synchronize framework documentation with implementation`  
**Date:** 2026-07-18

---

## 1. Objective

Create an evidence-based quality-risk model that distinguishes framework defects, AUT limitations,
environment risks, configuration risks, infrastructure risks, technical debt, test debt, and
accepted risks. Produce structured documentation that enables reviewers to understand what is
reliable, what is failing, why failures occur, and which risks require future action.

---

## 2. Initial Working-Tree Status

```
git status --short  → (clean)
git branch          → main
git rev-parse HEAD  → 96fa5a79833949adcc2ddd1d81c2abbc18924927
git log -3          → 96fa5a7 / e118ac9 / de08a39
```

Working tree was clean before any Phase 7 changes.

---

## 3. Pre-Change Validation

### Focused safety tests
```
ProductionSafetyGuardTest: 12/12 PASSED
```

### Cucumber scenarios
```
18 executions — 12 passed, 6 known AUT failures, 0 unexpected failures
BUILD FAILED (expected — 6 known AUT failures cause non-zero exit code)
```

Pre-change baseline confirmed identical to the accepted validated baseline.

---

## 4. Historical Evidence Reviewed

| Report | Key evidence extracted |
|---|---|
| `BASELINE_VALIDATION.md` | Initial 18/5/13 run; first identification of 6 stable failures; AUT limitation classification established |
| `BASELINE_CHANGE_INVESTIGATION.md` | Phase 3 first-run 18/5/13; confirmed as transient AUT instability; 12/6 baseline restored; stability pattern established |
| `PHASE_1_REPOSITORY_HYGIENE.md` | Repository cleanup; no functional change; confirmed clean compile |
| `PHASE_2_DEPENDENCY_VERIFICATION.md` | All dependencies pinned; no dynamic versions; no drift risk |
| `PHASE_3_LOW_RISK_CODE_IMPROVEMENTS.md` | Phase 3 transient instability event documented; all 7 extra failures resolved on rerun |
| `PHASE_4_PRODUCTION_SAFETY_GUARD.md` | Production write guard implementation; production detection gap for "production" alias closed; guard boundary at orchestration level confirmed |
| `PHASE_5_DEAD_CODE_AND_CONFIGURATION_CLEANUP.md` | Phase 5 transient instability event documented (runScenario[16]); orphaned `implicitWait` keys documented; `POLLING_INTERVAL_MILLIS` noted as outside Phase 5 scope |
| `PHASE_6_DOCUMENTATION_SYNCHRONIZATION.md` | All active documentation synchronized; no executable change; 30/12/6 baseline confirmed |

---

## 5. Risk Classification Model

| Term | Definition |
|---|---|
| **Framework defect** | Reproducible problem in framework code causing incorrect execution, false failure, or false pass |
| **AUT limitation** | Observed application behaviour differing from test assertion, reproducible without framework malfunction |
| **Environment instability** | Intermittent external behaviour changing between identical executions without source changes |
| **Technical debt** | Maintainability, configuration, or infrastructure compromise with no current functional impact |
| **Test debt** | Missing or incomplete test coverage, weak assertions, or insufficient isolation |
| **Accepted risk** | Known risk consciously retained because fixing it is outside current scope or repository control |

Likelihood values: Low / Medium / High  
Impact values: Low / Medium / High / Critical  
Rating matrix: as defined in `docs/QUALITY_RISK_ASSESSMENT.md`

---

## 6. Known AUT Failure Analysis

### AUT-LIM-001 — Login injection inputs do not produce expected error state

| Field | Value |
|---|---|
| Affected scenarios | `runScenario[9]`, `[10]`, `[11]` — "Sign-in is protected against injection and scripting attacks" |
| Input categories | SQL-like username, SQL-like password, XSS-like username |
| Expected | `p.error` element visible within 30s explicit wait |
| Actual | `p.error` absent — `WaitException → TimeoutException` |
| Assertion point | `user_should_see_error_message()` → `waitForElementToBeVisible(ERROR_MESSAGE)` |
| Login outcome | Does not redirect to overview; no visible error state returned |
| Framework responsibility | None — same assertion passes for 8 of 11 invalid-login scenarios |
| Classification | AUT behaviour — input-handling characteristic of the public demo server |
| Test value | Preserved — documents actual AUT response to security-oriented inputs; automatic pass if AUT is fixed |

### AUT-LIM-002 — Registration does not produce expected authenticated session

| Field | Value |
|---|---|
| Affected scenarios | `runScenario[13]`, `[14]`, `[15]` — three positive registration scenarios |
| Data sources | Static, LoremIpsum-generated, Excel (`ddt.xlsx`) |
| Expected | `a[href='logout.htm']` visible on OverviewPage — session authenticated after registration |
| Actual | Element absent — `TimeoutException`; page does not transition to overview |
| Assertion point | `OverviewPage.hasLogOutLink()` → `waitForElementToBeVisible(LOGOUT_LINK)` |
| Registration submission | Form submitted via page object; no driver error — AUT does not confirm via session state |
| Classification | AUT limitation — shared public demo server behaviour; likely duplicate-user or throttling condition |
| Test value | Preserved — would pass against a private, clean deployment; absence is the signal |

### AUT-LIM-003 — Transient server instability

Two documented events: Phase 3 (7 extra failures, all resolved on rerun) and Phase 5
(1 extra failure, resolved on rerun). Pattern is consistent: all affected scenarios had
previously passed, all restored without framework changes. Classified as environment instability.

---

## 7. Known Limitation Records Created

| ID | Title | Scenarios | Status |
|---|---|---|---|
| AUT-LIM-001 | Security-oriented login inputs do not produce expected error state | [9], [10], [11] | Outside repository control |
| AUT-LIM-002 | Registration does not produce expected authenticated session | [13], [14], [15] | Outside repository control |
| AUT-LIM-003 | Public ParaBank environment exhibits intermittent availability/state instability | Any beyond baseline | Outside repository control |

Full records in `docs/KNOWN_AUT_LIMITATIONS.md`.

---

## 8. Risk-Register Summary

| ID | Category | Risk | Likelihood | Impact | Rating | Status |
|---|---|---|---|---|---|---|
| QR-001 | External Dependency | ParaBank public demo instability | High | Medium | High | Outside repository control |
| QR-002 | AUT Behaviour | Login injection — no error state | High | Low | Medium | Outside repository control |
| QR-003 | AUT Behaviour | Registration — no authenticated session | High | Low | Medium | Outside repository control |
| QR-004 | Test Data | Test-data accumulation on shared public AUT | Medium | Medium | Medium | Accepted |
| QR-005 | Configuration | `production` alias has no dedicated `.properties` file | Low | Medium | Low | Accepted |
| QR-006 | Configuration | Orphaned `implicitWait` property in 5 config files | Low | Low | Low | Accepted |
| QR-007 | Configuration | `POLLING_INTERVAL_MILLIS` — declared, zero callers | Low | Low | Low | Deferred |
| QR-008 | Infrastructure | Grid `chrome-node` lacks health-check gate | Low | Medium | Low | Deferred |
| QR-009 | CI/CD | Known failures cause CI non-zero exit | High | Low | Medium | Accepted |
| QR-010 | Security | Production guard scope — setup writes only | Low | Low | Low | Accepted |
| QR-011 | Test Design | Chrome-only in CI | Medium | Low | Low | Deferred |
| QR-012 | Maintainability | Static `DEFAULT_USER_SETUP_DONE` flag | Low | Low | Low | Accepted |
| QR-013 | Test Data | No registration cleanup mechanism | Medium | Low | Low | Accepted |

Total: 13 risks identified.

---

## 9. Highest-Rated Risks

| ID | Rating | Title | Rationale |
|---|---|---|---|
| QR-001 | High | ParaBank public demo instability | Documented twice; likelihood is confirmed high; impact is medium (transient additional failures misread as regression) |
| QR-002 | Medium | Login injection — no error state | Consistent high likelihood; low impact (known, documented, not hiding a defect) |
| QR-003 | Medium | Registration — no authenticated session | Same as QR-002 |
| QR-004 | Medium | Test-data accumulation | Medium likelihood and impact; mitigated by idempotent handling |
| QR-009 | Medium | CI non-zero exit on known failures | High likelihood; low impact (accurate signal); documented baseline |

No critical risks identified. The highest framework-controlled risk is QR-004 (test data
accumulation) at Medium, currently accepted.

---

## 10. Mitigated Risks

| Risk | Mitigation | Phase |
|---|---|---|
| Automatic test-data writes in production (`prod`) | `ConfigManager.guardAgainstProductionWrite()` — throws `ConfigurationException` before any write | Phase 4 |
| `production` alias bypassing production guard | `isProductionEnvironment()` expanded to match both "prod" and "production" | Phase 4 |
| Dead constants polluting tag model | `TAG_HARDCODED`, `TAG_LOREM`, `TAG_DDT` removed | Phase 5 |
| Obsolete tag documentation misleading contributors | README, TEST_STRATEGY, CONTRIBUTING updated; all obsolete tag references removed | Phase 6 |
| Stale locator example in documentation | `PASSWORD_FIELD` code example updated to reflect Phase 3 CSS selector | Phase 6 |

---

## 11. Accepted Risks

| ID | Risk | Why accepted |
|---|---|---|
| QR-002, QR-003 | AUT limitations (6 failures) | No framework action possible; resolution requires AUT change |
| QR-004 | Test-data accumulation | Public AUT has no cleanup API; idempotent setup handles `sqa` user |
| QR-005 | No `production.properties` | Guard protects writes; one-line config change if needed |
| QR-006 | Orphaned `implicitWait` | No code reads it; harmless |
| QR-009 | CI non-zero exit on known failures | Accurate and correct CI signal |
| QR-010 | Production guard covers setup writes only | Scope is clearly documented |
| QR-012 | Static setup flag | One Cucumber suite per JVM in practice |
| QR-013 | No cleanup mechanism | Public AUT limitation |

---

## 12. Risks Outside Repository Control

| Risk | Owner |
|---|---|
| AUT-LIM-001, QR-002 | External AUT / ParaBank maintainers |
| AUT-LIM-002, QR-003 | External AUT / ParaBank maintainers |
| AUT-LIM-003, QR-001 | Outside repository control |
| Chrome browser compatibility | Browser vendor / GitHub runner |
| GitHub Actions runner availability | GitHub |

---

## 13. Technical Debt Inventory

| ID | Item | Impact | Priority |
|---|---|---|---|
| TD-001 | `implicitWait` in 5 config files (orphaned) | None — no code reads it | Low |
| TD-002 | `POLLING_INTERVAL_MILLIS` — zero callers confirmed | None — no functional impact | Low |
| TD-003 | No `production.properties` file | Low — fallback to `config.properties` | Low |
| TD-004 | Grid `chrome-node` no health-check gate | Low — documented; possible first-run Grid failure | Medium |
| TD-005 | No test-data cleanup for positive registration | Low — accumulates on public AUT only | Low |

---

## 14. Test Debt Inventory

| ID | Gap | Classification |
|---|---|---|
| TestD-001 | Cross-browser CI (Firefox, Edge) | Deferred |
| TestD-002 | Grid integration validation | Deferred |
| TestD-003 | Unknown environment name handling | Deferred |
| TestD-004, TestD-005 | Account and profile management feature coverage (5 page objects) | Deferred |
| TestD-006 | API contract/schema tests | Out of scope by design |
| TestD-007 | Accessibility testing | Out of scope by design |
| TestD-008 | Performance testing | Out of scope by design |
| TestD-009 | Reporting content verification | Deferred |
| TestD-010 | CI headless/Xvfb path validation | Not required |

---

## 15. Release Interpretation

**12 passed / 6 known AUT failures does not mean the framework is unstable.**

The accepted baseline is 18 scenario executions with zero unexpected failures.

- Known failures are visible — not suppressed or retried
- Regression = new or changed failure outside the 6-failure set, confirmed across ≥ 2 runs
- External instability must be confirmed through a second run before classification
- Gradle `BUILD FAILED` with exactly 6 failures = established baseline, not a framework defect

---

## 16. README and Test Strategy References Added

- `README.md`: Added "Quality and Risk Documentation" section with navigation table linking to
  `QUALITY_RISK_ASSESSMENT.md`, `KNOWN_AUT_LIMITATIONS.md`, `TEST_STRATEGY.md`, `CONTRIBUTING.md`
- `README.md`: Updated Table of Contents with link to new section
- `docs/TEST_STRATEGY.md`: Added "Related Documents" section linking to risk and limitation docs

---

## 17. Consistency Search Results

```
git grep "known AUT"      → 5 consistent references — all classify failures correctly
git grep "18 executions"  → 6 consistent references — all state 18 executions
git grep "12 passed"      → 4 active doc references — all consistent
git grep "6 failed"       → 5 active doc references — all consistent
```

No contradictions found in active documentation. Historical phase reports differ appropriately
for their point-in-time context.

---

## 18. Full Regression Validation

### Focused safety tests
```
ProductionSafetyGuardTest: 12/12 PASSED
```

### Cucumber scenarios

| runScenario | Scenario | Result |
|---|---|---|
| [0] | A registered customer can sign in with valid credentials | PASSED |
| [0],[1] | Sign-in rejected — invalid credentials (×2) | PASSED |
| [2],[3],[4] | Sign-in rejected — missing fields (×3) | PASSED |
| [5],[6],[7] | Sign-in rejected — format requirements (×3) | PASSED |
| [8] | Sign-in rejected — whitespace password | PASSED |
| [9],[10],[11] | Sign-in protected against injection (×3) | **FAILED** — AUT-LIM-001 |
| [12] | Sign-in rejected — locked account | PASSED |
| [13],[14],[15] | Positive registration scenarios (×3) | **FAILED** — AUT-LIM-002 |
| [16] | Registration rejected — no username | PASSED |

### Combined Gradle totals

| Metric | Baseline | Phase 7 result | Difference |
|---|---|---|---|
| Cucumber executions | 18 | 18 | 0 |
| Passed scenarios | 12 | 12 | 0 |
| Failed scenarios | 6 | 6 | 0 |
| Known AUT failures | 6 | 6 | 0 |
| Unexpected failures | 0 | 0 | 0 |
| Production safety tests passed | 12 | 12 | 0 |
| **Total Gradle tests** | **30** | **30** | **0** |

Documentation-only changes introduced no runtime regression.

---

## 19. Risk Category Summary

| Risk category | Risks identified | High/Critical | Mitigated | Accepted/Deferred |
|---|---|---|---|---|
| AUT behaviour | 2 (QR-002, QR-003) | 0 | 0 | 2 (outside control) |
| External dependency | 1 (QR-001) | 1 | 0 | 1 (outside control) |
| Configuration | 3 (QR-005, QR-006, QR-007) | 0 | 0 | 3 accepted/deferred |
| Environment | included in QR-001 | — | — | — |
| Infrastructure | 1 (QR-008) | 0 | 0 | 1 deferred |
| CI/CD | 1 (QR-009) | 0 | 0 | 1 accepted |
| Security | 1 (QR-010) | 0 | 1 (Phase 4 guard) | 1 accepted (residual scope) |
| Test design/data | 3 (QR-004, QR-011, QR-012, QR-013) | 0 | 0 | 4 accepted/deferred |
| Maintainability | 1 (QR-012) | 0 | 0 | 1 accepted |

---

## 20. Exact Files Changed

| File | Type | Change |
|---|---|---|
| `docs/QUALITY_RISK_ASSESSMENT.md` | Risk documentation | Created |
| `docs/KNOWN_AUT_LIMITATIONS.md` | AUT limitation records | Created |
| `README.md` | Documentation | Navigation table + ToC entry added |
| `docs/TEST_STRATEGY.md` | Documentation | Related Documents section added |
| `docs/review/PHASE_7_QUALITY_RISK_ASSESSMENT.md` | Review evidence | Created (this file) |

No Java source files, feature files, Gradle build, configuration values, Docker files,
or CI workflows were modified.

---

## 21. Deferred Actions

| Item | Reason deferred |
|---|---|
| Create `production.properties` | Configuration change; separate phase |
| Remove orphaned `implicitWait` property keys | Configuration value change; separate phase |
| Classify and remove `POLLING_INTERVAL_MILLIS` | Out of Phase 7 scope |
| Add `chrome-node` health check to Grid compose | Infrastructure change; separate phase |
| Expand feature coverage for 5 page objects | Test expansion; separate phase |
| Cross-browser CI matrix | CI enhancement; separate phase |

---

## 22. Final Recommendation

> **PHASE 7 COMPLETE — READY FOR CONTRIBUTOR GOVERNANCE**
