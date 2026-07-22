# BDDFramework-ParabankAutomation v1.1.1

**Release date:** 2026-07-22  
**Branch:** `main`  
**Tag:** `v1.1.1`  
**Previous release:** v1.1.0 (2026-07-19)

---

## Release Summary

v1.1.1 is a patch release. It fixes a CI reliability issue in WebDriver window handling and a CI
result-classifier identity-matching defect, and completes a portfolio documentation
reorganization with terminology synchronized to the current classifier.

No application behaviour, Java source (outside the one `DriverFactory` fix), test scenarios,
scenario count, or public framework API changed since v1.1.0.

This is a portfolio demonstration release. No commercial deployment is implied.

---

## Fixed

### CI window handling

`DriverFactory` called `window().maximize()`. On GitHub-hosted Ubuntu runners (Xvfb, no window
manager), Selenium 4.x routes this through a CDP `Runtime.evaluate` call to query screen
geometry, which the runner cannot answer, throwing `unknown error: JavaScript code failed`. This
caused every scenario to fail at `@Before` in CI since the framework's initial commit.

Replaced with `window().setSize(new Dimension(1920, 1080))`, which uses the W3C Set Window Rect
command directly — no CDP, no screen-geometry query — and is consistent with the
`--window-size=1920,1080` Chrome option already configured. Works identically on local displays
and Xvfb.

### CI result classifier — stable Cucumber JSON scenario-ID matching

`scripts/analyze-test-results.sh` previously matched failed scenarios by their TestNG-generated
`runScenario[N]` name. That name is assigned by data-provider invocation order, not by scenario
content. Direct inspection of a locally generated JUnit XML file proved this is unstable: under
`dataproviderthreadcount` > 1 (Gradle's own default is 2), the same index was reused for two
different scenarios in the same run, with indices assigned out of declaration order. CI avoided
the symptom only because it forces `dataproviderthreadcount=1`/`maxParallelForks=1` — a setting
documented elsewhere as a runner CPU-contention mitigation, never as a classifier-stability
guarantee.

The classifier now identifies failed scenarios using the Cucumber JSON report's element-level
`id` (`build/reports/cucumber/cucumber-report.json`), derived from the feature name, scenario
name, and outline row ordinal. This id does not depend on TestNG thread count, execution order,
or invocation index. JUnit XML remains the source of truth for execution counts and the
production-safety gate; a material disagreement between the JUnit XML and Cucumber JSON counts is
now treated as untrustworthy (`RESULTS_UNAVAILABLE`) rather than resolved silently.

This was validated against a real parallel-execution run that reproduced the original index
collision (`runScenario[0]` assigned to two different scenarios in the same run) — the classifier
still correctly returned `VALIDATED_BASELINE` using the stable ids.

---

## Changed

- **`.github/workflows/automation-test.yml`** — Added a `cucumber-reports-<env>-run<N>` artifact
  upload step for `build/reports/cucumber/`, since the Cucumber JSON report is now the
  classifier's identity source of truth.
- **Documentation reorganized** — `docs/` restructured into `architecture/`, `guides/`, and
  `quality/` categories for a recruiter-facing structure. Internal development-history documents
  removed: the 13-file `docs/review/` directory (phase-by-phase development reports and baseline
  investigations), `docs/INTERVIEWER_GUIDE.md` (its evidence trail depended on the removed
  reports), and `docs/RELEASE_VALIDATION.md` (a one-time release-audit checklist). All internal
  cross-links and the `README.md` / `CONTRIBUTING.md` navigation tables were repaired to the new
  paths.
- **Documentation terminology synchronized** — `README.md`, `docs/PORTFOLIO_OVERVIEW.md`, and all
  four `docs/guides/*.md` files updated to describe the current `KNOWN_FAILURE_IDS` /
  `VALIDATED_BASELINE` / `UNEXPECTED_REGRESSION` classifier mechanism, replacing every reference
  to the retired `runScenario[N]` / `KNOWN_FAILURE_INDICES` mechanism.
- **`build.gradle`** — Project version updated from `1.1.0` to `1.1.1`.

---

## Validation Results

The following table reflects the CI-verified state on `main` at commit `bfdf131` (the commit
immediately preceding this release branch), from the GitHub Actions run triggered by that commit:

| Metric | Result |
|---|---|
| Production-safety tests | 12 / 12 passed |
| Cucumber executions | 18 |
| Passed scenarios | 12 |
| Known AUT failures | 6 — exact canonical scenario-ID match |
| Unexpected failures | 0 |
| Missing expected IDs | 0 |
| CI classification | `VALIDATED_BASELINE` |
| Required artifacts uploaded | 6 of 6 (test logs, screenshots, Extent report, Allure results, TestNG reports, Cucumber reports) |

**Important:** Gradle exits non-zero (`BUILD FAILED`) when any test fails — including the six
known AUT failures. This is expected. The CI classifier is the authoritative gate:
`VALIDATED_BASELINE` means the run produced exactly the accepted known-failure set (by scenario
id) and no unexpected deviation.

---

## Compatibility

- No changes to Java source code affecting test execution behaviour, except the `DriverFactory`
  window-handling fix described above.
- No changes to feature files, step definitions, page objects, locators, or scenario data.
- No changes to `Dockerfile`, `docker-compose.yml`, or `docker-compose.grid.yml`.
- No changes to dependency versions in `build.gradle`.
- Cucumber execution count is unchanged: 18.
- The classifier's public exit-code contract is unchanged (`0`/`1`/`2`/`3` for
  `VALIDATED_BASELINE`/`UNEXPECTED_REGRESSION`/`INFRASTRUCTURE_FAILURE`/`RESULTS_UNAVAILABLE`) —
  only the internal identity-matching mechanism changed.

---

## Upgrade Notes

Upgrading from v1.1.0:

1. No changes to Java source code affecting test execution behaviour, aside from the
   `DriverFactory` window-handling fix (transparent — no configuration change required).
2. `build.gradle` project version changed from `1.1.0` to `1.1.1`.
3. If you maintain a fork or local override of `scripts/analyze-test-results.sh`, note that
   `KNOWN_FAILURE_INDICES` no longer exists — it has been replaced by `KNOWN_FAILURE_IDS`, keyed
   on Cucumber JSON scenario `id` instead of TestNG `runScenario[N]` index.
4. If you reference any of the following removed documentation paths, update the reference:
   `docs/INTERVIEWER_GUIDE.md`, `docs/RELEASE_VALIDATION.md`, or anything under `docs/review/`
   — all removed. The evergreen documents formerly at the top level of `docs/` have moved to
   `docs/architecture/`, `docs/guides/`, or `docs/quality/` (see `README.md`'s documentation
   table for the current paths).

No AUT credentials, locators, or scenario structures changed.

---

## Known AUT Limitations

| Scenario | Cucumber JSON id (suffix) | Root cause |
|---|---|---|
| Sign-in protected against injection and scripting attacks (row 1) | `...;;2` | AUT sanitises injection attempts without producing an assertion-catchable error element (AUT-LIM-001) |
| Sign-in protected against injection and scripting attacks (row 2) | `...;;3` | Same AUT-LIM-001 |
| Sign-in protected against injection and scripting attacks (row 3) | `...;;4` | Same AUT-LIM-001 |
| A new customer can open a bank account with their personal information | (plain scenario) | AUT does not redirect to overview after registration in all states (AUT-LIM-002) |
| A new customer can open a bank account with a freshly generated profile | (plain scenario) | Same AUT-LIM-002 |
| A customer can open a bank account using details provided by an external source | (plain scenario) | Same AUT-LIM-002 |

Six failures are `VALIDATED_BASELINE`. Zero additional failures are expected.

See [`docs/quality/KNOWN_AUT_LIMITATIONS.md`](docs/quality/KNOWN_AUT_LIMITATIONS.md) for full
records, including AUT-LIM-003 (documented public-AUT instability and the one-rerun confirmation
protocol).

---

## Release Integrity

| Check | Result |
|---|---|
| Working tree clean before release branch created | Yes |
| Local `main` matched `origin/main` before branching | Confirmed |
| No Java code changed in this release beyond the `DriverFactory` fix (already merged pre-release) | Confirmed |
| No test logic, feature file, or scenario changed | Confirmed |
| No Docker or Gradle dependency changed | Confirmed |
| CI classification at release commit | `VALIDATED_BASELINE` (18/12/6, exact id match, 0 unexpected) |
| Production-safety tests at release commit | 12/12 passed |
| Required CI artifacts present at release commit | 6/6 |
| Documentation links verified | 83 relative links checked, 0 broken in the current documentation surface |
| Stale classifier terminology remaining | None outside explicitly marked historical records |

