# Phase 9 — Selenium Grid Readiness

**Repository:** https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation  
**Branch:** `main`  
**Starting commit:** `4151aa8 — docs: add contributor governance and extension guidance`  
**Date:** 2026-07-18

---

## 1. Objective

Make Selenium Grid execution predictable by ensuring the Grid Hub and Chrome Node are both
ready before tests begin. Validate the complete Grid execution path — startup, readiness,
remote session creation, full regression, and teardown. Confirm local execution is unaffected.

---

## 2. Repository State

```
git status --short  → (clean)
git branch          → main
git rev-parse HEAD  → 4151aa8
git log -3          → 4151aa8 / 01b054f / 96fa5a7
```

Working tree was clean before any Phase 9 changes.

---

## 3. Local Pre-Change Baseline

### Production-safety tests
```
./gradlew test --tests "*ProductionSafetyGuardTest"
ProductionSafetyGuardTest: 12/12 PASSED — BUILD SUCCESSFUL
```

### Full local regression
```
./gradlew clean test
30 tests completed, 6 failed
  Safety tests:           12/12
  Cucumber executions:    18
  Passed:                 12
  Known AUT failures:     6  (runScenario[9,10,11,13,14,15])
  Unexpected failures:    0
BUILD FAILED (expected)
```

Local baseline confirmed stable.

---

## 4. Grid Asset Inventory

| Area | Current implementation | Evidence | Issue |
|---|---|---|---|
| Hub image | `selenium/hub:4.25.0` | `docker-compose.grid.yml:27` | None |
| Browser node | `selenium/node-chrome:4.25.0`, 2 slots | `docker-compose.grid.yml:44` | None |
| Hub ports | 4444 (WebDriver + UI), 4442/4443 (event bus) | `docker-compose.grid.yml:30-32` | None |
| Hub health check | `curl -f http://localhost:4444/wd/hub/status \| grep '"ready":true'` | `docker-compose.grid.yml:34` | **Deadlock**: Selenium 4 Hub reports `ready: false` until a node registers — but node can't start until hub is healthy |
| Node health check | None | `docker-compose.grid.yml` | **Gap**: node started with `service_started` condition only |
| Node registration | Via event bus (4442/4443) | `SE_EVENT_BUS_HOST=selenium-hub` | None |
| Startup dependency | `selenium-tests` depends on `chrome-node` by start order only | `docker-compose.grid.yml:68-69` | **Gap**: tests may start before node registers |
| Remote URL | `http://selenium-hub:4444/wd/hub` (container) / `http://localhost:4444/wd/hub` (host) | `docker-compose.grid.yml:76`, `ConfigManager` | None |
| Remote toggle | `-DseleniumGridEnabled=true` (system property) or `seleniumGridEnabled=true` (properties file) | `ConfigManager.isSeleniumGridEnabled()` | None |
| Browser support | Chrome, Firefox, Edge (Grid); chrome-node image provides Chrome only | `DriverFactory.initializeRemoteDriver()` | Chrome only on node — documented |
| Teardown | `docker compose down` | README | Volume cleanup required `-v` flag |
| Diagnostics | Hub logs, node logs only | README | No readiness script existed |
| Host-side scripts | None | `find scripts/` | **Gap**: no readiness mechanism for host-side execution |

---

## 5. Docker Availability

```
Docker version 29.4.0, build 9d7ad9f
Docker Compose version v5.1.1
```

Docker fully available. `docker compose` (Compose v2 plugin) used throughout.

---

## 6. Compose Validation

```
docker compose -f docker-compose.grid.yml config
```

Result: YAML fully resolved — services, environment variables, ports, and dependency declarations
all valid. No unsupported fields. Image names correct for Selenium 4.25.0.

---

## 7. Remote Driver Flow

```
-DseleniumGridEnabled=true        → ConfigManager.isSeleniumGridEnabled() → true
-DgridUrl=http://localhost:4444/wd/hub → ConfigManager.getRemoteGridUrl() → "http://localhost:4444/wd/hub"
  → DriverFactory.createDriver()
    → DriverFactory.initializeRemoteDriver("chrome")
      → new URL(gridUrl) → gridEndpoint
      → new RemoteWebDriver(gridEndpoint, buildChromeOptions())
        → Chrome options: --disable-gpu, --no-sandbox, --disable-dev-shm-usage, --start-maximized
        → NO headless flag (qa.properties headless=false)
  → configureDriver(driver)
    → driver.manage().timeouts().pageLoadTimeout(45s)
    → driver.manage().window().maximize()
    → driver.get("https://parabank.parasoft.com/parabank/")
```

Key findings:
- `seleniumGridEnabled=false` in `qa.properties` — local execution is unaffected by Grid properties
- Grid URL uses `/wd/hub` suffix — confirmed correct for Selenium 4 Grid 4.25.0
- Browser options are shared between local and remote paths — no special remote options needed
- No connection timeout set on `RemoteWebDriver` constructor — uses JVM default socket timeout
- `MalformedURLException` thrown cleanly on invalid URL; `DriverInitializationException` thrown when URL is empty

---

## 8. Readiness Gap

Before Phase 9, the startup guarantee was:

```
Hub container started → chrome-node container started → selenium-tests container started
```

This is **container start** only, not Grid readiness. The actual problem had two components:

**Problem 1 — Hub health check deadlock:**
The hub health check used `curl -fsSL http://localhost:4444/wd/hub/status | grep -q '"ready":true'`.
In Selenium 4 Grid, the Hub reports `"ready": false` until at least one node registers.
But `chrome-node` cannot start until `selenium-hub: condition: service_healthy`.
Result: deadlock — the hub could never become healthy because no node could register.

**Problem 2 — Node health-check absence:**
`chrome-node` had no health check. `selenium-tests` depended on it with `condition: service_started`
(start order only). If tests started before the node completed registration, they would fail
with "no available nodes."

---

## 9. Selected Solution and Rationale

**Chosen:** Docker health-check cascade (Preferred Option 1 from spec) + host-side readiness script (Option 2).

**Hub health check (corrected):**
Changed to `curl -f http://localhost:4444/status` — checks HTTP server liveness only (200 OK),
not Grid readiness. The Hub HTTP server starts immediately and returns 200 even before nodes register.
This breaks the deadlock: the Hub becomes healthy, chrome-node starts and registers, chrome-node
health check passes, selenium-tests starts.

**Chrome-node health check (added):**
`curl -f http://localhost:5555/status` — the Selenium node's own status endpoint at port 5555.
Returns HTTP 200 only after the node has successfully registered with the Hub. The `selenium-tests`
service dependency was upgraded from `service_started` to `service_healthy` on `chrome-node`.

**Note on grep approach:**
An earlier attempt used `grep -q '"ready":true'` on the node's `/status` response. This failed
because the node's JSON uses `"ready": true` (with space) while the previous hub check used
`"ready":true` (no space). Using `curl -f` without grep eliminates the formatting dependency.

**Host-side readiness script:**
`scripts/wait-for-grid.ps1` — polls `http://localhost:4444/status` with configurable timeout
(default 120s) and poll interval (default 5s). Exits 0 when `ready=true` AND at least one node
is registered; exits 1 with diagnostics on timeout.

---

## 10. Files Changed

| File | Change |
|---|---|
| `docker-compose.grid.yml` | Hub health check corrected (HTTP liveness); chrome-node health check added; `selenium-tests` dependency upgraded to `service_healthy` |
| `scripts/wait-for-grid.ps1` | Created — host-side Grid readiness script |
| `docs/SELENIUM_GRID_GUIDE.md` | Created — complete Grid execution reference |
| `README.md` | Grid section rewritten with corrected commands and SELENIUM_GRID_GUIDE.md link; navigation table updated |
| `docs/TEST_STRATEGY.md` | Parallelism table Docker+Grid row corrected |
| `docs/FRAMEWORK_EXTENSION_GUIDE.md` | Grid validation section added |
| `docs/QUALITY_RISK_ASSESSMENT.md` | QR-008 status updated to Mitigated; TD-004 marked Resolved |
| `docs/review/PHASE_9_SELENIUM_GRID_READINESS.md` | Created (this file) |

No Java source files, feature files, Gradle build, or CI workflows were modified.

---

## 11. Health-Check Implementation

### Hub (`selenium-hub`)

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:4444/status"]
  interval: 10s
  timeout: 5s
  retries: 12
  start_period: 20s
```

Passes when the Hub HTTP server returns 200. Typically healthy within ~10s of container start.

### Chrome-node (`chrome-node`)

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:5555/status"]
  interval: 10s
  timeout: 5s
  retries: 12
  start_period: 20s
```

Passes when the node's own status endpoint returns 200, which requires successful hub registration.
Typically healthy within ~50-60s of container start.

---

## 12. Readiness Script Implementation

`scripts/wait-for-grid.ps1` — PowerShell (Windows dev environment):

- Default URL: `http://localhost:4444/status`
- Default timeout: 120s
- Default poll interval: 5s
- Success condition: `value.ready == true` AND `value.nodes.Count > 0`
- Exit 0 on success; exit 1 on timeout or error
- Outputs remaining seconds on each poll; outputs node URI and slot count on success
- Outputs diagnostic commands on timeout

---

## 13. Grid Startup Result

```
docker compose -f docker-compose.grid.yml down -v --remove-orphans  → clean
docker compose -f docker-compose.grid.yml up -d selenium-hub chrome-node

Startup sequence:
  → selenium-hub: Creating / Started / Waiting / Healthy  (~10s)
  → selenium-chrome-node: Creating / Started  (waits for hub healthy)
  → selenium-chrome-node: health: starting
  → selenium-chrome-node: (healthy)  (~60s total from up command)

docker compose -f docker-compose.grid.yml ps:
NAME                   STATUS
selenium-hub           Up (healthy)     ports: 0.0.0.0:4442-4444->4442-4444/tcp
selenium-chrome-node   Up (healthy)     ports: 0.0.0.0:7900->7900/tcp
```

---

## 14. Status Endpoint Result

```
http://localhost:4444/status:
{
  "value": {
    "ready": true,
    "nodes": [
      {
        "uri": "http://172.19.0.3:5555",
        "slots": [
          { "stereotype": { "browserName": "chrome", "browserVersion": "130.0" } },
          { "stereotype": { "browserName": "chrome", "browserVersion": "130.0" } }
        ]
      }
    ]
  }
}
```

Grid ready: true. 1 node registered. 2 Chrome slots available.

---

## 15. Readiness Script Result

```
.\scripts\wait-for-grid.ps1 -GridStatusUrl http://localhost:4444/status -TimeoutSeconds 30
→ [OK] Grid is ready. Registered nodes: 1
→      Node: http://172.19.0.3:5555  slots: 2
→ Exit code: 0
```

Failure path (invalid URL, 15s timeout):
```
→ [WAIT] Grid not reachable (Unable to connect to the remote server)
→ [TIMEOUT] Grid did not become ready within 15s.
→ Exit code: 1
```

---

## 16. Remote Session Smoke Result (Step 14)

```
./gradlew clean test -DseleniumGridEnabled=true -DgridUrl=http://localhost:4444/wd/hub -Dcucumber.filter.tags=@smoke
→ runScenario[0]("A registered customer can sign in with valid credentials") PASSED
→ BUILD SUCCESSFUL
```

RemoteWebDriver session created, Chrome node received session, AUT loaded, assertion passed,
session closed. No orphaned sessions.

---

## 17. Focused Remote Test Result (Step 15)

```
./gradlew test --tests "*ProductionSafetyGuardTest"
→ 12/12 PASSED — BUILD SUCCESSFUL (independent of Grid)
```

---

## 18. Full Grid Regression Result

```
./gradlew clean test -DseleniumGridEnabled=true -DgridUrl=http://localhost:4444/wd/hub

30 tests completed, 6 failed
  Safety tests:           12/12 passed
  Cucumber executions:    18
  Passed:                 12
  Known AUT failures:     6  (runScenario[9,10,11,13,14,15])
  Unexpected failures:    0
BUILD FAILED (expected — 6 known AUT failures)
```

### Grid vs Local comparison

| Metric | Local baseline | Grid result | Difference |
|---|---:|---:|---:|
| Safety tests | 12 | 12 | 0 |
| Cucumber executions | 18 | 18 | 0 |
| Passed | 12 | 12 | 0 |
| Known failures | 6 | 6 | 0 |
| Unexpected failures | 0 | 0 | 0 |
| Total Gradle tests | 30 | 30 | 0 |

Grid result is identical to local baseline.

---

## 19. Parallelism and Capacity

| Setting | Default (build.gradle) | Grid Compose override |
|---|---|---|
| `maxParallelForks` | 2 | 1 (in docker-compose.grid.yml) |
| `dataproviderthreadcount` | 2 | 1 (in docker-compose.grid.yml) |
| Chrome-node max sessions | — | 2 (`SE_NODE_MAX_SESSIONS=2`) |

Host-side execution uses build.gradle defaults (2/2), which matches the node capacity (2 slots).
Sessions queue at the Grid if concurrent demand exceeds 2. No interference observed in full regression.

---

## 20. Failure Diagnostic Validation

**Readiness script against stopped Grid (port 9999 / invalid):**
```
Exit code: 1
[WAIT] Grid not reachable (Unable to connect to the remote server) — 11s remaining
[TIMEOUT] Grid did not become ready within 15s.
→ diagnostic commands printed
```

Non-zero exit, useful diagnostic output, no test execution started. Confirmed.

---

## 21. Teardown Result

```
docker compose -f docker-compose.grid.yml down -v --remove-orphans
  → selenium-chrome-node: Stopped / Removed
  → selenium-hub: Stopped / Removed
  → Network bddframework-parabankautomation_default: Removed

docker compose -f docker-compose.grid.yml ps → empty table (no containers)
```

Clean teardown confirmed. Port 4444 released.

---

## 22. Local Post-Change Regression

```
./gradlew clean test

30 tests completed, 6 failed
  Safety tests:           12/12
  Cucumber executions:    18
  Passed:                 12
  Known AUT failures:     6
  Unexpected failures:    0
BUILD FAILED (expected)
```

Local execution unchanged. No regression introduced by Grid changes.

---

## 23. Documentation Changes

| Document | Change |
|---|---|
| `docker-compose.grid.yml` | Health checks — infrastructure change (not documentation only) |
| `docs/SELENIUM_GRID_GUIDE.md` | Created — complete Grid guide |
| `README.md` | Grid section rewritten with correct commands; navigation table updated |
| `docs/TEST_STRATEGY.md` | Parallelism row corrected |
| `docs/FRAMEWORK_EXTENSION_GUIDE.md` | Grid validation section added |
| `docs/QUALITY_RISK_ASSESSMENT.md` | QR-008 mitigated; TD-004 resolved |

---

## 24. Risk and Debt Status Updates

| Item | Before Phase 9 | After Phase 9 |
|---|---|---|
| QR-008 — Grid chrome-node no health check | Deferred | **Mitigated** |
| TD-004 — Grid chrome-node no health-check gate | Open (Medium) | **Resolved** |
| Mitigation roadmap "Future: Grid readiness" | Open | **Completed** |

No new risks introduced.

---

## 25. Remaining Limitations

| Limitation | Classification | Status |
|---|---|---|
| Chrome-only Grid node (no Firefox/Edge node) | Test debt — TestD-001 | Deferred |
| No Grid integration test in CI pipeline | Test debt — TestD-002 | Deferred |
| Single chrome-node — no load balancing | Architectural scope | Out of scope |
| `RemoteWebDriver` has no explicit connection timeout | Low residual | Accepted |

---

## 26. Validation Summary

| Validation | Expected | Result | Status |
|---|---|---|---|
| Compose config | Valid | Valid — all fields resolved | PASS |
| Hub health check | Fixed | HTTP liveness check — no deadlock | PASS |
| Chrome-node health check | Added | `curl -f http://localhost:5555/status` | PASS |
| Grid readiness (hub + node) | Ready | `ready: true`, 1 node, 2 slots | PASS |
| Required browser registered | Yes | Chrome 130.0, 2 slots | PASS |
| Readiness script success path | Exit 0 | Confirmed | PASS |
| Readiness script failure path | Exit 1 + diagnostics | Confirmed | PASS |
| Remote session creation | Successful | `@smoke` — 1/1 PASSED | PASS |
| Safety tests | 12/12 | 12/12 | PASS |
| Grid Cucumber executions | 18 | 18 | PASS |
| Grid passed | 12 | 12 | PASS |
| Grid known failures | 6 | 6 | PASS |
| Grid unexpected failures | 0 | 0 | PASS |
| Local post-change baseline | 18/12/6 | 18/12/6 | PASS |
| Clean teardown | Yes | Confirmed — no containers remain | PASS |

---

## 27. Final Recommendation

> **PHASE 9 COMPLETE — READY FOR CI/CD IMPROVEMENTS**
