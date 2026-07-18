# Phase 8 — Contributor Governance

**Repository:** https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation  
**Branch:** `main`  
**Starting commit:** `01b054f — docs: add quality risk assessment and known AUT limitations`  
**Date:** 2026-07-18

---

## 1. Objective

Establish practical contributor governance and framework-extension guidance so that new
contributors can safely set up the project, add features and scenarios, run correct validation,
interpret the 18/12/6 baseline, and submit quality pull requests — without introducing
regressions, anti-patterns, or security issues.

---

## 2. Initial Working-Tree Status

```
git status --short  → (clean)
git branch          → main
git rev-parse HEAD  → 01b054f (docs: add quality risk assessment and known AUT limitations)
git log -3          → 01b054f / 96fa5a7 / e118ac9
```

Working tree was clean before any Phase 8 changes.

---

## 3. Source Files Read for Documentation Evidence

| File | Purpose |
|---|---|
| `src/test/java/com/parabank/parasoft/stepdefinitions/LoginSteps.java` | Assertion placement, DriverManager usage, page object construction patterns |
| `src/test/java/com/parabank/parasoft/pages/LoginPage.java` | Locator declarations, fluent API, navigation, no-assertion guarantee |
| `src/test/java/com/parabank/parasoft/pages/BasePage.java` | `createPage()` reflection, protected Selenium wrappers, constructor contract |
| `src/test/java/com/parabank/parasoft/stepdefinitions/RegisterSteps.java` | Unique-username generation, ExcelDataProvider usage |
| `docs/review/PHASE_7_QUALITY_RISK_ASSESSMENT.md` | Risk baseline, technical debt inventory, known AUT failure records |
| `docs/QUALITY_RISK_ASSESSMENT.md` | Risk register for documentation cross-reference |
| `docs/KNOWN_AUT_LIMITATIONS.md` | AUT limitation records for contributor guidance |

No Java source files, feature files, Gradle build, configuration values, Docker files,
or CI workflows were modified.

---

## 4. Documents Created

### 4.1 `docs/FRAMEWORK_EXTENSION_GUIDE.md` (new)

Full contributor extension reference. Sections:

| Section | Content |
|---|---|
| Architecture boundaries | 5-layer diagram + supporting layers table |
| Contributor boundary decisions | 12-row decision table (what changes where) |
| Adding features, scenarios, step definitions | Step-by-step with rules |
| Adding page objects | Locator standards, fluent API contract, `BasePage` extension rules |
| Adding API operations | `ParaBankApiClient` extension rules, production guard obligation |
| Test data and configuration | Unique-name generation, property addition, no credential commits |
| Adding tags | Active tag table, documentation obligation before use |
| Locator standards | 5-preference hierarchy (CSS selector → ID → name → link text → XPath) |
| Wait standards | Explicit-only policy; no `Thread.sleep`; no `implicitlyWait` |
| Assertion guidance | `@Then`-only policy confirmed from `LoginSteps.java`; page objects return, not assert |
| Validation workflow | 4-step sequence with exact commands |
| Baseline interpretation | 30/12/18/12/6 meaning; `BUILD FAILED` normal state |
| Anti-patterns | 11 named anti-patterns with before/after code examples |
| Documentation responsibilities | Table of change → update obligation mappings |

### 4.2 `docs/PULL_REQUEST_CHECKLIST.md` (new)

Copyable Markdown checklist. Sections:

| Section | Content |
|---|---|
| Scope | 5 items: concern isolation, no formatting noise, no generated artifacts |
| Architecture | 10 items: layer enforcement, no WebDriver in steps, production guard preservation, no `Thread.sleep`, no implicit wait, assertions in `@Then` only |
| BDD | 10 items: business-readable titles, no locators in Gherkin, tag hygiene, no step duplication, execution count change field |
| Test Data and Security | 8 items: no real PII, no committed credentials, `.env` exclusion, production safety obligation |
| Validation Evidence | Template for 4 validation levels (compile, safety tests, focused, full regression) plus conditional Run 2 |
| Failure Classification | 6-category table with Run 1/Run 2 columns |
| Documentation | 6 items: doc obligation checks, historical report preservation |
| Summary Statement | Fill-in template with execution count delta, known AUT failures confirmation, unexpected failure field, risks field |

### 4.3 `CONTRIBUTING.md` (complete rewrite)

Replaced prior multi-section document with focused entry-point. Sections:

- Prerequisites table (Java 17, Chrome, Git)
- Setup with baseline explanation (30/12/18/12/6; `BUILD FAILED` normal state)
- Branch workflow (6 naming conventions with examples)
- Commit message format (6 type prefixes with examples)
- Architecture in one page (layer → rule table, 7 rows)
- Active tags (6-row table with applied-at and purpose)
- Validation commands (4-step sequence with exact commands)
- Baseline and failure classification (6-category classification table)
- Production safety (4 rules — guard position, API write guard, no unguarded network writes, no alias weakening)
- Security rules (4 rules — no committed credentials, no password logging, no real PII, no `.env` commit)
- Documentation obligations (change → update required table)
- Before submitting (link to `docs/PULL_REQUEST_CHECKLIST.md`)

### 4.4 `README.md` (targeted edit)

Navigation table in "Quality and Risk Documentation" section expanded with two rows:

| Added link | Purpose |
|---|---|
| `docs/FRAMEWORK_EXTENSION_GUIDE.md` | Architecture boundaries, layer rules, locator/wait standards, anti-patterns, scenario workflow |
| `docs/PULL_REQUEST_CHECKLIST.md` | Copyable PR submission checklist with validation evidence template |

---

## 5. Governance Consistency Search Results (Step 21)

| Search | Result | Assessment |
|---|---|---|
| `@hardcoded` in `README.md`, `CONTRIBUTING.md`, `docs` (excl. `docs/review/`) | CONTRIBUTING.md:114 — explicitly lists as removed/prohibited | Correct — contributor is warned not to use these tags |
| `@ipsum` in same scope | Same line as above | Correct |
| `@ddt` in same scope | Same line as above | Correct |
| `Thread.sleep` in same scope | README.md — in "zero `Thread.sleep()`" prohibition statements; `FRAMEWORK_EXTENSION_GUIDE.md` — in anti-patterns with before/after | All references prohibit usage — no recommendation to use |
| `implicitWait` in same scope | `docs/QUALITY_RISK_ASSESSMENT.md` — QR-006, TD-001 (orphaned property); `FRAMEWORK_EXTENSION_GUIDE.md` — in anti-patterns | All references either document the orphaned property or prohibit the pattern |
| `production guard` / `guardAgainstProductionWrite` | `CONTRIBUTING.md` — layer rule and production safety section; `README.md` — architecture and hooks diagram; `docs/QUALITY_RISK_ASSESSMENT.md` — QR-010; `docs/TEST_STRATEGY.md` — §5 environment table | Consistent — guard presence required in all contributing guidance |

**Consistency assessment:** No contradictions. No obsolete tag guidance reintroduced. No implicit-wait recommendation. No unguarded production-write guidance. Baseline counts consistent across all active documentation.

---

## 6. Command Validation (Step 22)

### Compile
```
Command: ./gradlew clean compileTestJava
Result:  BUILD SUCCESSFUL in 2s
         3 actionable tasks: 3 executed
```

### Production safety tests
```
Command: ./gradlew test --tests "*ProductionSafetyGuardTest"
Result:  BUILD SUCCESSFUL in 6s
         All 12 ProductionSafetyGuardTest tests PASSED
```

Both validation commands in `CONTRIBUTING.md` and `docs/FRAMEWORK_EXTENSION_GUIDE.md` produce the documented results.

---

## 7. Diff Review (Step 23)

```
git status --short:
 M CONTRIBUTING.md
 M README.md
?? docs/FRAMEWORK_EXTENSION_GUIDE.md
?? docs/PULL_REQUEST_CHECKLIST.md

git diff --stat HEAD:
 CONTRIBUTING.md | 218 ++++++++++++++++++++++++++++++++++++++------------------
 README.md       |   6 +-
 2 files changed, 152 insertions(+), 72 deletions(-)
```

New files: `docs/FRAMEWORK_EXTENSION_GUIDE.md`, `docs/PULL_REQUEST_CHECKLIST.md`
Modified files: `CONTRIBUTING.md` (rewrite), `README.md` (navigation table expansion)
No Java source, feature file, Gradle build, config, Docker, or CI file was modified.

---

## 8. Full Regression Validation (Step 24)

### Focused safety tests
```
ProductionSafetyGuardTest: 12/12 PASSED
```

### Cucumber scenarios

| runScenario | Scenario | Result |
|---|---|---|
| [0] | A registered customer can sign in with valid credentials | PASSED |
| [1], [2] | Sign-in rejected — invalid credentials (×2) | PASSED |
| [3], [4], [5] | Sign-in rejected — missing fields (×3) | PASSED |
| [6], [7], [8] | Sign-in rejected — format requirements (×3) | PASSED |
| [8] | Sign-in rejected — whitespace password | PASSED |
| [9], [10], [11] | Sign-in protected against injection (×3) | **FAILED** — AUT-LIM-001 |
| [12] | Sign-in rejected — locked account | PASSED |
| [13], [14], [15] | Positive registration scenarios (×3) | **FAILED** — AUT-LIM-002 |
| [16] | Registration rejected — no username | PASSED |

### Combined Gradle totals

| Metric | Baseline | Phase 8 result | Difference |
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

## 9. Exact Files Changed

| File | Type | Change |
|---|---|---|
| `docs/FRAMEWORK_EXTENSION_GUIDE.md` | Contributor guide | Created |
| `docs/PULL_REQUEST_CHECKLIST.md` | PR checklist | Created |
| `CONTRIBUTING.md` | Entry-point documentation | Complete rewrite |
| `README.md` | Navigation documentation | 2-row navigation table expansion |
| `docs/review/PHASE_8_CONTRIBUTOR_GOVERNANCE.md` | Review evidence | Created (this file) |

---

## 10. Deferred Actions

No new risks or deferrals were introduced by Phase 8. All items deferred in Phase 7 remain
unchanged. No governance documentation introduced scope beyond Phase 8 boundaries.

---

## 11. Final Recommendation

> **PHASE 8 COMPLETE — READY FOR SELENIUM GRID READINESS**
