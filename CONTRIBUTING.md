# Contributing

Contributions are welcome — bug fixes, new test scenarios, new page objects, and documentation
improvements. Read this document before starting work.

**Detailed guidance:**
- [docs/FRAMEWORK_EXTENSION_GUIDE.md](docs/FRAMEWORK_EXTENSION_GUIDE.md) — architecture, layer rules, code standards, anti-patterns
- [docs/PULL_REQUEST_CHECKLIST.md](docs/PULL_REQUEST_CHECKLIST.md) — submission checklist with validation evidence template
- [docs/KNOWN_AUT_LIMITATIONS.md](docs/KNOWN_AUT_LIMITATIONS.md) — the 6 expected test failures and why they exist
- [docs/QUALITY_RISK_ASSESSMENT.md](docs/QUALITY_RISK_ASSESSMENT.md) — risk register and technical/test debt
- [docs/CI_CD_GUIDE.md](docs/CI_CD_GUIDE.md) — CI pipeline stages and result classification

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 17 or later |
| Google Chrome | Latest stable |
| Git | 2.x |

ChromeDriver is managed automatically by WebDriverManager. No manual download needed.

---

## Setup

```bash
git clone https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation.git
cd BDDFramework-ParabankAutomation

# Verify baseline before making any changes
./gradlew clean test
```

**Expected baseline:** 30 total tests — 12 production-safety tests (all passed) + 18 Cucumber
scenarios (12 passed, 6 known AUT failures). Gradle exits with `BUILD FAILED` because the 6
known failures are active and intentional. This is the correct starting state.

See [docs/KNOWN_AUT_LIMITATIONS.md](docs/KNOWN_AUT_LIMITATIONS.md) for why those 6 fail.

---

## Branch Workflow

- Do not commit directly to `main`.
- Create a branch for every contribution:

```bash
git checkout -b feature/<short-description>   # new capability
git checkout -b fix/<short-description>        # bug fix
git checkout -b refactor/<short-description>   # internal improvement
git checkout -b docs/<short-description>       # documentation only
git checkout -b test/<short-description>       # test additions
git checkout -b chore/<short-description>      # housekeeping
```

- Keep one logical concern per branch.
- Compile and run the baseline before starting (`./gradlew clean compileTestJava`) so you know
  the repository was clean when you began.

---

## Commit Messages

Use the same format as the project history:

```
feat: add account overview coverage
fix: correct RegisterPage error message locator
test: add transfer validation scenarios
refactor: simplify page interaction flow
docs: update framework extension guidance
chore: improve repository hygiene
```

Keep commits small and reviewable. Avoid mixing code, coverage, infrastructure, and
documentation changes in a single commit without justification.

---

## Architecture in One Page

```
Feature files  →  Step definitions  →  Page objects  →  ElementActions/WaitUtils  →  WebDriver
```

| Layer | Rule |
|---|---|
| Feature files | Business-readable Gherkin; no locators; no Java; no timings |
| Step definitions | Map steps to Java; delegate browser work to page objects; assert in `@Then` only |
| Page objects | Locators + browser actions; fluent API; extend `BasePage` |
| `ElementActions` / `WaitUtils` | Selenium wrappers; do not duplicate |
| Hooks | Lifecycle only; production guard must remain before all write paths |
| `ConfigManager` | All config access; no direct `System.getProperty()` in other layers |
| `ParaBankApiClient` | HTTP setup operations; no assertions; no sensitive logging |

For detailed layer rules, locator standards, wait standards, anti-patterns, and a step-by-step
scenario addition workflow, see [docs/FRAMEWORK_EXTENSION_GUIDE.md](docs/FRAMEWORK_EXTENSION_GUIDE.md).

---

## Active Tags

| Tag | Applied at | Purpose |
|---|---|---|
| `@regression` | Feature | Full regression suite |
| `@smoke` | Scenario | Critical happy-path — fast sanity check |
| `@positive` | Scenario | Expected successful behaviour |
| `@negative` | Scenario | Invalid input, boundary, rejection |
| `@validation` | Scenario | Input and business-rule validation |
| `@security` | Scenario | Injection and scripting probe scenarios |

Do not use `@hardcoded`, `@ipsum`, `@ddt`, or `@fail` — these were removed in Phase 5 and have
no active scenarios. Any new tag must be documented before use.

---

## Validation Commands

Run these in order before submitting a pull request:

```bash
# 1. Compile
./gradlew clean compileTestJava

# 2. Production safety tests only (fast — no browser)
./gradlew test --tests "*ProductionSafetyGuardTest"

# 3. Focused test — your change only
./gradlew clean test -Dcucumber.filter.tags="@<your-tag>"

# 4. Full regression — must match the accepted baseline
./gradlew clean test
```

---

## Baseline and Failure Classification

**Accepted baseline:** 18 Cucumber executions — 12 passed, 6 known AUT failures, 0 unexpected.

**CI classification:** GitHub Actions classifies each run automatically. A run matching exactly the 6 known failures is `VALIDATED_BASELINE` and the job is **green**. Any deviation — extra failure, missing failure, or count mismatch — is `UNEXPECTED_REGRESSION` and the job is **red**. The job summary panel in the Actions UI always shows the classification result. See [docs/CI_CD_GUIDE.md](docs/CI_CD_GUIDE.md) for details.

If the full regression shows more than 6 failures:

1. Preserve the first result.
2. Check whether the failing scenario is in the known-failure list in [docs/KNOWN_AUT_LIMITATIONS.md](docs/KNOWN_AUT_LIMITATIONS.md).
3. If it is a new failure, run once more to check for transient AUT instability.
4. If it reproduces on the second run, **block merge and investigate**. Do not keep rerunning until green.
5. Record both run results in the pull request.

Do not report only `BUILD FAILED` — classify every failure by category:

| Category | Description |
|---|---|
| Known AUT failure | Listed in `KNOWN_AUT_LIMITATIONS.md` — expected, not a regression |
| Transient AUT instability | Resolved on rerun without source changes — document both runs |
| New stable regression | Reproducible across two runs — block merge |
| Test-data collision | Scenario depends on shared server state from another run |
| Configuration failure | Environment, property, or credential mismatch |
| Infrastructure failure | WebDriver, network, or Grid issue |

---

## Production Safety

The production write guard prevents automatic test-user registration from running against
production environments.

Rules that must not be broken:
- `ConfigManager.guardAgainstProductionWrite()` must remain the first call inside the
  synchronized block in `Hooks.ensureDefaultTestUserExists()`, before both write paths.
- Any new HTTP write operation in `ParaBankApiClient` must be preceded by an equivalent guard.
- Do not add write operations to page objects, step definitions, or utility classes that call
  the network without a production check.
- `prod` and `production` (case-insensitive) are blocked for automatic writes. Do not weaken
  this by adding new aliases without a safety analysis.

---

## Security Rules

- Never commit credentials to any source file. Use `TEST_USERNAME` / `TEST_PASSWORD`
  environment variables for non-public credentials.
- Never log passwords — only usernames may appear in log output.
- No real names, SSNs, addresses, or phone numbers in test data — use the `LoremIpsum` generator
  or Excel-supplied values.
- Do not commit the `.env` file.

---

## Documentation Obligations

Every pull request that changes user-visible behaviour, execution counts, tags, or architecture
must update the corresponding documentation before merge:

| Change | Update required |
|---|---|
| New scenario(s) | README Quick Start count; TEST_STRATEGY baseline; CONTRIBUTING baseline |
| New tag | README tag table; TEST_STRATEGY tag table; CONTRIBUTING active tags |
| New page object or utility | README Project Structure |
| New configuration property | README Configuration section |
| Architecture change | README Architecture; TEST_STRATEGY |
| New risk or mitigation | QUALITY_RISK_ASSESSMENT.md |

Do not modify files in `docs/review/` — they are point-in-time historical evidence records.

---

## Before Submitting

Copy [docs/PULL_REQUEST_CHECKLIST.md](docs/PULL_REQUEST_CHECKLIST.md) into your pull request
description and complete every item, including the validation evidence table with actual
command output.
