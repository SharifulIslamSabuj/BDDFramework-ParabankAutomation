# Phase 1 — Repository Hygiene Report

## 1. Objective

Remove accidental and generated artifacts from Git tracking, strengthen `.gitignore` to
prevent re-introduction of those artifacts, and confirm that framework test behaviour remains
identical to the confirmed baseline of 18 executions / 12 passed / 6 failed.

This phase contains no test-behaviour changes, no framework-architecture changes, and no
dependency modifications.

---

## 2. Repository

| Field          | Value                                                                  |
|----------------|------------------------------------------------------------------------|
| Repository URL | https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation |
| Branch         | main                                                                   |
| HEAD at start  | `aa3c243f248e1cd476d0f8efc1db2bbe9f2d775a`                            |
| Phase 1 commit | see §14                                                                |

---

## 3. Initial Repository State

Four files were modified relative to HEAD prior to this phase (BDD Phase 2 refactoring
deliverables, carried forward from the previous session):

| File | Status |
|------|--------|
| `src/test/java/com/parabank/parasoft/stepdefinitions/LoginSteps.java` | modified |
| `src/test/java/com/parabank/parasoft/stepdefinitions/RegisterSteps.java` | modified |
| `src/test/resources/features/login.feature` | modified |
| `src/test/resources/features/register.feature` | modified |

Two untracked items were also present:

| Item | Status |
|------|--------|
| `docs/review/` | untracked (Phase 0 and 0.1 deliverables) |
| `tatus` | untracked accidental file |

---

## 4. Pre-Existing Modified Files

The four files listed above were not staged, modified, reverted, or touched during Phase 1.
They remain in exactly the same state as before Phase 1 began.

---

## 5. Files Removed (physical deletion)

| File | Reason | Method |
|------|--------|--------|
| `tatus` | Accidental shell artifact in repository root; untracked; clearly generated | `Remove-Item` (local deletion only — file was not tracked) |

---

## 6. Files Removed from Git Tracking

| File | Reason | Method | Was committed |
|------|--------|--------|--------------|
| `build.log` | Generated Gradle build log; not a source artifact; tracked accidentally in initial commit | `git rm build.log` | Yes — present in HEAD `aa3c243` |

`build.log` was 2 370 bytes and contained timestamped Gradle task output from a prior build.
It was the only tracked generated artifact found by the full `git ls-files` audit.

---

## 7. `.gitignore` Changes

### Rules added

```gitignore
############################################
# Logs
############################################

*.log
logs/


############################################
# Test runtime evidence
############################################

screenshots/
```

### Rationale

| Rule | Reason |
|------|--------|
| `*.log` | `build.log` was tracked because no `*.log` rule existed. This rule prevents any root-level log file from being tracked in future. |
| `logs/` | Defensive coverage for a standalone `logs/` directory that may appear at project root if a framework configuration is changed in future. |
| `screenshots/` | Defensive coverage for a root-level `screenshots/` directory. The `build/screenshots/` path is already covered by the existing `/build/` rule; this adds protection if screenshots are ever written outside the build directory. |

### Rules NOT added (already present)

The following were already in `.gitignore` and required no duplication:

| Already present | Covers |
|-----------------|--------|
| `/build/` | All Gradle build output including `build/logs/`, `build/screenshots/`, `build/reports/` |
| `/.gradle/` | Gradle cache |
| `/allure-results/` | Allure raw test result JSON |
| `/allure-report/` | Allure HTML report |
| `/.idea/` | IntelliJ IDEA project files |
| `*.iml` | IntelliJ module files |
| `.env`, `.env.*` | Local environment / credential files |

---

## 8. Generated-Artifact Audit (Tracked Files)

Full audit performed via `git ls-files` against the following patterns:

```
^build/  ^\.gradle/  ^\.idea/  allure-results  allure-report  logs/  screenshots/  \.log$  ^\.env$
```

**Result:** Only `build.log` matched. It was staged for removal in Step 2.
No other generated artifacts were found in the tracked file set.

Post-test-run `git status` confirmed that after `./gradlew clean test`:
- `build/` (reports, screenshots, logs) — correctly ignored by `/build/`
- `allure-results/` — correctly ignored by `/allure-results/`
- New `build.log` — correctly ignored by `*.log`
- Only `docs/review/` appeared as untracked — expected (documentation deliverables)

---

## 9. Test Execution

### Command

```
./gradlew clean test
```

No tag filters, retry options, parallel overrides, or environment overrides were applied.

### Result

| Metric              | Baseline | Phase 1 result | Difference |
|---------------------|--------:|---------------:|-----------:|
| Total executions    |      18 |             18 |          0 |
| Passed              |      12 |             12 |          0 |
| Failed              |       6 |              6 |          0 |
| Known AUT failures  |       6 |              6 |          0 |
| Unexpected failures |       0 |              0 |          0 |

Duration: 2 minutes 50 seconds.

---

## 10. Known Failure Confirmation

All 6 failures matched the confirmed baseline exactly:

**Login — security-input failures (3):**

| Scenario | Expected error | Actual response | Classification |
|----------|----------------|-----------------|----------------|
| Injection: `'; DROP TABLE users; --` in username | `"The username and password could not be verified."` | `p.error` absent — `got: ''` | Known AUT behaviour |
| Injection: `' OR '1'='1` in password | `"The username and password could not be verified."` | `p.error` absent — `got: ''` | Known AUT behaviour |
| Injection: `<script>alert('xss')</script>` in username | `"The username and password could not be verified."` | `p.error` absent — `got: ''` | Known AUT behaviour |

**Registration — session/redirect failures (3):**

| Scenario | Expected element | Actual response | Classification |
|----------|-----------------|-----------------|----------------|
| Personal information | `a[href='logout.htm']` visible | Element absent after 30 s | Known AUT behaviour |
| Freshly generated profile | `a[href='logout.htm']` visible | Element absent after 30 s | Known AUT behaviour |
| External source data | `a[href='logout.htm']` visible | Element absent after 30 s | Known AUT behaviour |

---

## 11. Unexpected Failures

**None.** Zero unexpected failures in the Phase 1 validation run.

---

## 12. Post-Run Git Tracking Verification

After `./gradlew clean test`, `git status --short` showed:

```
 M .gitignore
D  build.log
 M src/test/java/com/parabank/parasoft/stepdefinitions/LoginSteps.java
 M src/test/java/com/parabank/parasoft/stepdefinitions/RegisterSteps.java
 M src/test/resources/features/login.feature
 M src/test/resources/features/register.feature
?? docs/review/
```

Generated artifacts produced by the test run — `build/reports/`, `build/logs/test.log`,
`build/screenshots/`, `allure-results/` — did not appear in the status output.
All are correctly ignored.

---

## 13. Files Changed During Phase 1

### Modified (tracked)

| File | Change |
|------|--------|
| `.gitignore` | 15 lines added: `*.log`, `logs/`, `screenshots/` with section headers |

### Removed from Git tracking

| File | Change |
|------|--------|
| `build.log` | Staged via `git rm`; will be absent from the repository after commit |

### Deleted locally (untracked, not a Git operation)

| File | Change |
|------|--------|
| `tatus` | Deleted via `Remove-Item`; was never tracked |

### Created (new, untracked → staged for commit)

| File | Purpose |
|------|---------|
| `docs/review/BASELINE_VALIDATION.md` | Phase 0 baseline-lock evidence document |
| `docs/review/BASELINE_CHANGE_INVESTIGATION.md` | Phase 0.1 investigation report |
| `docs/review/PHASE_1_REPOSITORY_HYGIENE.md` | This document |

> The `docs/review/evidence/` subdirectory (containing copied log files, JSON reports, and
> screenshots from investigation runs) is intentionally **not staged**. It is runtime
> investigation evidence, not source documentation. It remains on disk for local reference only.

### Not changed

| Category | Status |
|----------|--------|
| Java source files (all) | Unchanged |
| Feature files (all) | Unchanged |
| Step definitions (all) | Unchanged |
| Page objects (all) | Unchanged |
| Locators (all) | Unchanged |
| Configuration properties (all) | Unchanged |
| `build.gradle` | Unchanged |
| Docker files | Unchanged |
| GitHub Actions | Unchanged |
| Four pre-existing modified files | Unchanged — carried forward as-is |

---

## 14. Risks and Unresolved Observations

1. **Four pre-existing modified files not committed:** The BDD Phase 2 refactoring deliverables
   (`login.feature`, `register.feature`, `LoginSteps.java`, `RegisterSteps.java`) remain as
   working-tree modifications. They are outside Phase 1 scope and must be committed in a
   separate, dedicated commit.

2. **`docs/review/evidence/` not committed:** The investigation evidence directory (log copies,
   JSON reports, screenshots) is not staged. It is runtime investigation output, not
   documentation. It should be deleted locally once the review cycle is complete if disk space
   is a concern, or left as-is.

3. **Known AUT failures (6) remain:** The three injection login failures and three registration
   redirect failures are pre-existing AUT limitations on the public demo server. They are not
   blockers for repository work and are not affected by Phase 1.

---

## 15. Final Recommendation

> **PHASE 1 COMPLETE — READY FOR DEPENDENCY VERIFICATION**
