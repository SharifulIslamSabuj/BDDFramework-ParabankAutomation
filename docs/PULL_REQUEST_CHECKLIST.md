# Pull Request Checklist

**Framework:** ParaBank BDD Automation  
**Last Updated:** 2026-07-18

Copy this checklist into your pull request description. Complete every item before requesting review. For detailed guidance on any item, see [docs/FRAMEWORK_EXTENSION_GUIDE.md](FRAMEWORK_EXTENSION_GUIDE.md).

---

## Scope

- [ ] This PR addresses exactly one logical concern (feature, fix, refactor, docs, or chore).
- [ ] No unrelated formatting changes are included.
- [ ] No generated artifacts (`build/`, `allure-results/`, `build/screenshots/`) were committed.
- [ ] No files from other branches or merges were accidentally included.
- [ ] The change description clearly states what was added or modified and why.

---

## Architecture

- [ ] The correct framework layer was used for each change (feature → step → page object → ElementActions/WaitUtils → WebDriver).
- [ ] No `WebDriver` calls appear in step definitions — all browser interaction goes through page objects.
- [ ] No `By` locators are defined in step definitions or feature files.
- [ ] No locator is exposed outside its page-object class.
- [ ] `BasePage.createPage()` is used for page navigation, not direct `new PageName(driver)` calls in step definitions.
- [ ] Production write guard (`ConfigManager.guardAgainstProductionWrite()`) is preserved and not bypassed.
- [ ] The guard remains inside the synchronized block in `Hooks.ensureDefaultTestUserExists()`, before both write paths.
- [ ] No `Thread.sleep()` was introduced anywhere.
- [ ] No implicit wait (`driver.manage().timeouts().implicitlyWait(...)`) was introduced.
- [ ] Assertions remain in `@Then` step definitions, not inside page object actions.

---

## BDD

- [ ] Scenario titles are complete business-readable sentences.
- [ ] No CSS selectors, Java identifiers, or locator names appear in Gherkin.
- [ ] `Background` is used only when every scenario in the feature shares the same starting state.
- [ ] `Scenario Outline` has at least two `Examples` rows; single-row cases use `Scenario`.
- [ ] Every new scenario carries at least one active classification tag.
- [ ] Tags used exist in the active tag set: `@smoke`, `@regression`, `@positive`, `@negative`, `@validation`, `@security`.
- [ ] No obsolete tags were reintroduced: `@hardcoded`, `@ipsum`, `@ddt`, `@fail`.
- [ ] If a new tag was introduced, it has been added to `docs/TEST_STRATEGY.md` and `README.md`.
- [ ] No step expression duplicates an existing step definition.
- [ ] The expected Cucumber execution count after this PR: **___** (was: 18).

---

## Test Data and Security

- [ ] No real names, addresses, phone numbers, or SSNs are hardcoded in Java or feature files.
- [ ] No passwords, usernames, or API keys are committed to any source file.
- [ ] The `.env` file was not committed.
- [ ] Test credentials use `TEST_USERNAME` / `TEST_PASSWORD` environment variables or the public fallback (`sqa`).
- [ ] Passwords do not appear in any log statement.
- [ ] Unique usernames are generated per execution when a new account creation is tested (e.g., `LoremIpsum` + `System.currentTimeMillis()`).
- [ ] No scenario assumes a prior scenario left data in a specific state.
- [ ] Any new API write operation in `ParaBankApiClient` was reviewed for production-safety implications.

---

## Validation Evidence

Paste the result for each command. Do not omit commands that are relevant to your change.

### Compile
```
Command: ./gradlew clean compileTestJava
Result: [ BUILD SUCCESSFUL / BUILD FAILED — paste error ]
```

### Production safety tests
```
Command: ./gradlew test --tests "*ProductionSafetyGuardTest"
Result: [ 12/12 PASSED / paste failure ]
```

### Focused test (your change only)
```
Command: ./gradlew clean test -Dcucumber.filter.tags="@<your-tag>"
Result: [ N executions, N passed, N failed — paste summary ]
```

### Full regression — Run 1
```
Command: ./gradlew clean test
Result:
  Total tests:           ___
  Safety tests:          12/12
  Cucumber executions:   ___
  Passed:                ___
  Failed:                ___
  Known AUT failures:    6  (runScenario[9,10,11,13,14,15])
  Unexpected failures:   ___
```

### Full regression — Run 2 (required only if Run 1 showed an unexpected failure)
```
Command: ./gradlew clean test
Result:
  Total tests:           ___
  Cucumber executions:   ___
  Passed:                ___
  Failed:                ___
  Unexpected failures:   ___
  Classification:        [ Transient AUT instability / Reproducible regression ]
```

---

## Failure Classification

If any failure appeared beyond the known 6, classify it here:

| Scenario | runScenario index | Run 1 result | Run 2 result | Classification |
|---|---|---|---|---|
| | | | | |

Classification options:
- **Known AUT failure** — listed in [docs/KNOWN_AUT_LIMITATIONS.md](KNOWN_AUT_LIMITATIONS.md)
- **Transient AUT instability** — resolved on rerun without source changes (see AUT-LIM-003)
- **New stable regression** — fails across two consecutive runs; blocks merge until resolved
- **Test-data collision** — scenario depends on shared server state from another run
- **Environment/configuration failure** — unrelated to scenario logic
- **Infrastructure failure** — WebDriver, network, or Grid issue

---

## Documentation

- [ ] `README.md` was updated if a user-facing behaviour, execution count, or tag changed.
- [ ] `docs/TEST_STRATEGY.md` was updated if scope, tag model, or environment strategy changed.
- [ ] `docs/QUALITY_RISK_ASSESSMENT.md` was updated if a new risk was introduced or mitigated.
- [ ] `docs/KNOWN_AUT_LIMITATIONS.md` was updated if the known-failure set changed.
- [ ] Historical phase reports in `docs/review/` were **not modified**.
- [ ] No claim was added to documentation that cannot be supported by current source code.

---

## Summary Statement

Fill in before submitting:

> **What changed:** _(one or two sentences)_  
> **Why:** _(requirement, bug, or improvement rationale)_  
> **Execution count change:** 18 → ___  
> **Known AUT failures unchanged:** [ Yes / No — if No, explain ]  
> **Unexpected failures:** [ None / List ]  
> **Risks introduced:** [ None / Describe ]
