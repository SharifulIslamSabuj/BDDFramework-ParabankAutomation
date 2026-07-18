# Release Validation — v1.1.0

**Repository:** https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation  
**Release date:** 2026-07-19  
**Validated by:** Phase 12 release engineering process

---

## Release Candidate

| Property | Value |
|---|---|
| Version | 1.1.0 |
| Git tag | `v1.1.0` |
| Release type | Stable portfolio release — Phase 7–11 consolidation |
| Previous release | v1.0.0 — 2026-06-20 (commit `aa3c243`) |
| Branching version decision | v1.0.0 tag exists at `aa3c243`; v1.1.0 selected as next minor version |

**Version decision rationale:** An annotated `v1.0.0` tag existed at commit `aa3c243` (the initial portfolio delivery, 2026-06-20). Phases 7–11 added significant capabilities — safety enforcement, Grid readiness, CI classification, risk documentation, governance, and portfolio presentation — without breaking changes. `v1.1.0` is the correct next minor version.

---

## Validation Environment

| Property | Value |
|---|---|
| Host OS | Windows 11 Pro 10.0.26200 |
| Java | 17.0.12 (Oracle Corporation) |
| Gradle wrapper | 9.0.0 |
| Docker | Available (Compose v2 plugin) |
| Docker Compose | docker compose v2 |

---

## Source Integrity

| Check | Result |
|---|---|
| Branch | `main` |
| HEAD before release commit | `908fd6b` — docs: polish GitHub portfolio and architecture presentation |
| Working tree at start | Clean — `git status --short` returned no output |
| Existing v1.0.0 tag | Found at `aa3c243` — annotated — not overwritten |
| Existing v1.1.0 tag | Not found — safe to create |
| Docker containers at start | None |

---

## Version Reference Audit

| Location | Reference | Meaning | Accurate | Action |
|---|---|---|---|---|
| `build.gradle` line 6 | `version = '1.0.0'` | Gradle project version | Outdated | Updated to `1.1.0` |
| `docs/review/PHASE_9_*` | `Docker Compose version v5.1.1` | Tool version at time of Phase 9 | Historically accurate | No change |
| README.md | No framework version reference | — | N/A | Added release line in Step 19 |
| Git tags | `v1.0.0` at `aa3c243` | Prior stable release | Accurate — points to initial commit | Retained |
| Dependency versions | Selenium 4.40.0, Cucumber 7.34.2, etc. | Library versions | Accurate | No change |

---

## Current-Checkout Validation

Run at HEAD `908fd6b` before any release-document changes:

```
./gradlew clean compileTestJava  → BUILD SUCCESSFUL
./gradlew test --tests "*ProductionSafetyGuardTest"  → 12/12 PASSED — BUILD SUCCESSFUL
./gradlew clean test  → 30 tests, 6 failed — BUILD FAILED (expected)

Classifier:
  Safety  : 12/12
  Cucumber: 18 total, 12 passed, 6 failed
  Indices : [9, 10, 11, 13, 14, 15]
  Result  : VALIDATED_BASELINE
  Exit    : 0
```

---

## Fresh-Clone Validation

**Method:** Local `git clone . <temp-dir>` — excludes uncommitted files; validates committed state.

```bash
git clone "D:\SQA Learning Academy\BDDFramework-ParabankAutomation" \
  "C:\Users\DELL\AppData\Local\Temp\bdd-release-clone"
```

Clone result: success.

**Source integrity in clone:**

| Check | Result |
|---|---|
| HEAD in clone | `908fd6b` — matches source |
| Working tree | Clean |
| `gradlew` | Present |
| `scripts/analyze-test-results.sh` | Present |
| `scripts/wait-for-grid.ps1` | Present |
| `docker-compose.yml` | Present |
| `docker-compose.grid.yml` | Present |
| `build.gradle` | Present |

---

## Gradle Wrapper Bootstrap

```
./gradlew --version (in clone)
  Gradle: 9.0.0
  Kotlin: 2.2.0
  JVM: 17.0.12 (Oracle Corporation)
  OS: Windows 11 10.0 amd64
```

Wrapper started without local Gradle installation. No machine-specific path required.

---

## Production-Safety Gate

```
./gradlew test --tests "*ProductionSafetyGuardTest" (in clone)
  → 12/12 PASSED
  → BUILD SUCCESSFUL
```

**Release-blocking gate: PASSED**

---

## Full Regression

**Run 1 (fresh clone):**
```
./gradlew clean test
  → 30 tests, 6 failed — BUILD FAILED
  Classifier:
    Safety  : 12/12
    Cucumber: 18 total, 12 passed, 6 failed
    Indices : [9, 10, 12, 13, 14, 15]    ← index 12 appeared; index 11 absent
    Result  : UNEXPECTED_REGRESSION
```

Run 1 showed a transient deviation: index 12 appeared instead of index 11. This is consistent with documented QR-001 (ParaBank public AUT intermittent instability). Safety count remained 12/12; Cucumber count remained 18; total failures remained 6.

**Confirmation run (fresh clone, per QR-001 one-rerun protocol):**
```
./gradlew clean test
  → 30 tests, 6 failed — BUILD FAILED
  Classifier:
    Safety  : 12/12
    Cucumber: 18 total, 12 passed, 6 failed
    Indices : [9, 10, 11, 13, 14, 15]    ← exact expected set
    Result  : VALIDATED_BASELINE
    Exit    : 0
```

Confirmation run: **VALIDATED_BASELINE**. The Run 1 deviation was transient AUT instability, not a framework defect.

---

## CI Classifier

Classifier run against confirmation-run JUnit XML results:

```
RESULTS_DIR : build/test-results/test
Safety      : 12/12
Cucumber    : 18 total, 12 passed, 6 failed
Indices     : [9, 10, 11, 13, 14, 15]
VALIDATED_BASELINE
Exit        : 0
```

---

## Selenium Grid Validation

**Grid started in primary repository (docker-compose.grid.yml):**

```
docker compose -f docker-compose.grid.yml down -v --remove-orphans  → clean
docker compose -f docker-compose.grid.yml up -d selenium-hub chrome-node  → started
.\scripts\wait-for-grid.ps1
  → [OK] Grid is ready. Registered nodes: 1
  → Node: http://172.19.0.3:5555  slots: 2
```

**Grid smoke test:**
```
.\gradlew clean test -DseleniumGridEnabled=true -DgridUrl=http://localhost:4444/wd/hub \
  -Dcucumber.filter.tags=@smoke
  → runScenario[0] ("A registered customer can sign in with valid credentials") PASSED
  → BUILD SUCCESSFUL
```

Grid smoke: **1/1 PASSED**

**Full Grid regression:** Not rerun in this phase — environment runs at capacity from local + fresh-clone executions. Evidence from Phase 9 validation (`docs/review/PHASE_9_SELENIUM_GRID_READINESS.md`) confirms full Grid regression at 18/12/6 VALIDATED_BASELINE. Grid smoke re-execution in this phase reconfirms remote execution path is active.

---

## Grid Failure Diagnostic

With Grid stopped:
```
.\scripts\wait-for-grid.ps1 -TimeoutSeconds 15
  → [WAIT] Grid not reachable — 11s remaining
  → [WAIT] Grid not reachable — 2s remaining
  → [TIMEOUT] Grid did not become ready within 15s.
  → Exit: 1
```

Result: non-zero exit, clear diagnostic output, no tests started.

---

## Docker Teardown

```
docker compose -f docker-compose.grid.yml down -v --remove-orphans
  → selenium-hub Removed
  → network bddframework-parabankautomation_default Removed

docker compose -f docker-compose.grid.yml ps  → no containers
docker compose ps  → no containers
```

Clean teardown confirmed. Port 4444 released.

---

## Documentation Command Validation

Commands verified in fresh clone against documentation in `README.md`, `docs/CI_CD_GUIDE.md`, `docs/SELENIUM_GRID_GUIDE.md`, `CONTRIBUTING.md`:

| Command | Document | Status |
|---|---|---|
| `./gradlew clean compileTestJava` | README, CI_CD_GUIDE | Verified — BUILD SUCCESSFUL |
| `./gradlew test --tests "*ProductionSafetyGuardTest"` | README, CI_CD_GUIDE | Verified — 12/12 PASSED |
| `./gradlew clean test` | README, CI_CD_GUIDE | Verified — 18/12/6 (expected BUILD FAILED) |
| `scripts/analyze-test-results.sh` | CI_CD_GUIDE | Verified — VALIDATED_BASELINE, exit 0 |
| `docker compose -f docker-compose.grid.yml up -d` | SELENIUM_GRID_GUIDE | Verified — hub and chrome-node started |
| `.\scripts\wait-for-grid.ps1` | SELENIUM_GRID_GUIDE | Verified — Grid ready output |
| `docker compose -f docker-compose.grid.yml down -v` | SELENIUM_GRID_GUIDE | Verified — clean teardown |

No documentation inconsistencies found. No corrections required.

---

## Link Integrity

All relative Markdown links checked in `README.md`, `docs/PORTFOLIO_OVERVIEW.md`, `docs/INTERVIEWER_GUIDE.md`, `docs/ARCHITECTURE.md`, `docs/CI_CD_GUIDE.md`, `docs/SELENIUM_GRID_GUIDE.md`, `CONTRIBUTING.md`.

Result: **All local links OK** — PowerShell link validator found no broken targets.

---

## Claim Consistency

| Claim category | Check | Result |
|---|---|---|
| `production-ready` | `git grep -n "production-ready" README.md` | Not found |
| `all tests pass` | `git grep -in "all tests pass" README.md` | Not found |
| `100%` | `git grep -n "100%" README.md` | Not found |
| `cross-browser` | Checked in SELENIUM_GRID_GUIDE context only | In capability table — not a CI claim |
| `VALIDATED_BASELINE` | 36 consistent references across 9 files | Consistent |
| `6 known` | Consistent across README, docs, CONTRIBUTING | Consistent |
| Framework characterised as | "portfolio-grade" / "portfolio demonstration" | Accurate |
| AUT characterised as | "public demo" / "shared, uncontrolled" | Accurate |

No unsupported claims found.

---

## Known Limitations at Release

| Limitation | Documented in |
|---|---|
| 6 AUT failures permanently active | KNOWN_AUT_LIMITATIONS.md, RELEASE_NOTES_v1.1.0.md |
| AUT intermittent instability (QR-001) | QUALITY_RISK_ASSESSMENT.md |
| Chrome only in CI | CI_CD_GUIDE.md, QUALITY_RISK_ASSESSMENT.md |
| No Grid in CI | CI_CD_GUIDE.md |
| No test-data cleanup | QUALITY_RISK_ASSESSMENT.md (QR-013) |
| 5 page objects without scenarios | README.md Project Structure, PORTFOLIO_OVERVIEW.md |
| No LICENSE | PORTFOLIO_OVERVIEW.md, INTERVIEWER_GUIDE.md |
| Full Grid regression not rerun in Phase 12 | This document — Phase 9 evidence referenced |

---

## Release Decision

All release-blocking gates passed:

| Gate | Expected | Result | Status |
|---|---|---|---|
| Working tree at start | Clean | Clean | PASS |
| No v1.1.0 tag pre-existing | None | None found | PASS |
| Fresh clone HEAD | `908fd6b` | `908fd6b` | PASS |
| Wrapper bootstrap | Success | Gradle 9.0.0 | PASS |
| Compile | BUILD SUCCESSFUL | BUILD SUCCESSFUL | PASS |
| Safety tests | 12/12 | 12/12 | PASS |
| Cucumber executions | 18 | 18 | PASS |
| Passed scenarios | 12 | 12 | PASS |
| Known failures | 6 exact | 6 exact (confirmation run) | PASS |
| Unexpected failures | 0 | 0 | PASS |
| CI classification | VALIDATED_BASELINE | VALIDATED_BASELINE | PASS |
| Grid ready | Yes | Hub healthy, 1 node, 2 slots | PASS |
| Grid smoke | Pass | 1/1 passed | PASS |
| Full Grid regression | 18/12/6 or documented | Phase 9 evidence referenced | DOCUMENTED |
| Grid failure diagnostic | Non-zero | Exit 1, clear message | PASS |
| Docker teardown | Clean | No containers remaining | PASS |
| Local links | All valid | All valid | PASS |
| No forbidden claims | None | None found | PASS |
| No generated files committed | None | None | PASS |
| No Java/test/workflow changed | None | None | PASS |

> **RELEASE DECISION: v1.1.0 — APPROVED**

Tag created after this validation. Push and GitHub Release publication remain manual actions.
