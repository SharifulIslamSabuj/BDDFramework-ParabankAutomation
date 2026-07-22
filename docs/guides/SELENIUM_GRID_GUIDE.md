# Selenium Grid Execution Guide

**Framework:** ParaBank BDD Automation  
**Grid version:** Selenium 4.25.0  
**Last Updated:** 2026-07-18

---

## Purpose

This guide covers running the BDD suite against a local Selenium Grid 4 instead of a local
Chrome browser. Grid execution isolates the browser process in a dedicated container and
enables observation of browser sessions via noVNC.

Local execution (without Grid) remains the primary development workflow. Grid execution is
used to validate framework behaviour in a topology that mirrors production Grid deployments.

---

## Architecture

```
Host (Gradle / ./gradlew)
  │
  │  RemoteWebDriver  →  http://localhost:4444/wd/hub
  │
  ▼
selenium-hub:4444                   (Docker container)
  │  routes sessions to registered nodes
  ▼
chrome-node:5555                    (Docker container)
  │  executes Chrome, exposes noVNC at :7900
  │
  └─ Reports via event bus (4442/4443) back to hub
```

Docker Compose file: `docker-compose.grid.yml`

---

## Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin) installed and running
- `docker compose version` ≥ 2.x (the Compose v2 plugin, not legacy `docker-compose`)
- Ports 4442, 4443, 4444, 7900 available on the host

---

## Services

| Service | Image | Port | Purpose |
|---|---|---|---|
| `selenium-hub` | `selenium/hub:4.25.0` | 4444 | WebDriver endpoint + Grid console |
| `chrome-node` | `selenium/node-chrome:4.25.0` | 7900 (noVNC) | Chrome browser executor |

---

## Start Grid

```powershell
# 1. Remove any prior Grid containers (safe if none exist)
docker compose -f docker-compose.grid.yml down -v --remove-orphans

# 2. Start hub and chrome-node
docker compose -f docker-compose.grid.yml up -d selenium-hub chrome-node

# 3. Confirm both containers are healthy
docker compose -f docker-compose.grid.yml ps
```

Expected state when ready:
```
NAME                   STATUS
selenium-hub           Up (healthy)
selenium-chrome-node   Up (healthy)
```

The Compose health-check cascade ensures startup order:
1. Hub HTTP server becomes healthy (`/status` returns 200).
2. Chrome-node starts and registers with hub.
3. Chrome-node health check (`/status` on port 5555) passes only after registration.

---

## Verify Readiness

### Option A — PowerShell readiness script (recommended for host-side execution)

```powershell
.\scripts\wait-for-grid.ps1
```

With custom timeout:
```powershell
.\scripts\wait-for-grid.ps1 -GridStatusUrl http://localhost:4444/status -TimeoutSeconds 120
```

Exit 0 means Grid ready with at least one node registered. Exit 1 means timeout or unreachable.

### Option B — Direct status check

```powershell
Invoke-RestMethod -Uri http://localhost:4444/status | ConvertTo-Json -Depth 3
```

Expected response when ready:
```json
{
  "value": {
    "ready": true,
    "nodes": [ { "uri": "http://...:5555", "slots": [ ... ] } ]
  }
}
```

---

## Run Focused Remote Test

```powershell
# Smoke test — one scenario — fastest remote-session validation
.\gradlew clean test "-DseleniumGridEnabled=true" "-DgridUrl=http://localhost:4444/wd/hub" "-Dcucumber.filter.tags=@smoke"
```

Expected: 1 Cucumber execution, 1 passed, BUILD SUCCESSFUL.

---

## Run Full Remote Regression

```powershell
.\gradlew clean test "-DseleniumGridEnabled=true" "-DgridUrl=http://localhost:4444/wd/hub"
```

Expected result:
```
30 tests completed, 6 failed
  Safety tests:           12/12 passed
  Cucumber executions:    18
  Passed:                 12
  Known AUT failures:     6  (matched by Cucumber JSON scenario id — see docs/quality/KNOWN_AUT_LIMITATIONS.md)
  Unexpected failures:    0
BUILD FAILED  ← expected; caused by the 6 known AUT failures
```

The Grid result must match the local baseline. Any new failure outside the known 6 is a
regression — stop and investigate before continuing.

---

## Inspect Status and Logs

```powershell
# Grid status
Invoke-RestMethod -Uri http://localhost:4444/status | ConvertTo-Json -Depth 3

# Grid UI (browser)
# Open: http://localhost:4444/ui

# Live Chrome session view — noVNC (password: secret)
# Open: http://localhost:7900

# Container logs
docker compose -f docker-compose.grid.yml logs --no-color
docker compose -f docker-compose.grid.yml logs selenium-hub
docker compose -f docker-compose.grid.yml logs chrome-node

# Container state
docker compose -f docker-compose.grid.yml ps
```

---

## Stop and Clean Grid

```powershell
docker compose -f docker-compose.grid.yml down -v --remove-orphans
docker compose -f docker-compose.grid.yml ps
```

Expected: empty table (no containers).

---

## Configuration Reference

| System property | Default (qa.properties) | Purpose |
|---|---|---|
| `-DseleniumGridEnabled=true` | `false` | Enable RemoteWebDriver instead of local browser |
| `-DgridUrl=http://localhost:4444/wd/hub` | _(empty)_ | Selenium Grid hub endpoint |
| `-Dbrowser=chrome` | `chrome` | Browser to request from the Grid |
| `-Denv=qa` | `qa` | Environment (controls base URL and other properties) |

These properties map to `ConfigManager.isSeleniumGridEnabled()` and
`ConfigManager.getRemoteGridUrl()` in `DriverFactory.initializeRemoteDriver()`.

When `seleniumGridEnabled=false` (the default in `qa.properties`), the framework creates a
local browser regardless of what `gridUrl` is set to.

---

## Parallelism and Capacity

| Setting | Default (build.gradle) | Grid compose override |
|---|---|---|
| `maxParallelForks` | 2 | 1 (in docker-compose.grid.yml) |
| `dataproviderthreadcount` | 2 | 1 (in docker-compose.grid.yml) |
| Chrome-node max sessions | — | 2 (`SE_NODE_MAX_SESSIONS=2`) |

When running from the host with the default parallelism (`maxParallelForks=2`), up to 2
concurrent sessions are routed to the single Chrome-node (which has 2 slots). Sessions
beyond capacity queue at the Grid until a slot is free.

To override parallelism when running from the host:
```powershell
.\gradlew clean test "-DseleniumGridEnabled=true" "-DgridUrl=http://localhost:4444/wd/hub" "-DmaxParallelForks=1" "-Ddataproviderthreadcount=1"
```

---

## Failure Diagnostics

### Grid does not become ready

```powershell
docker compose -f docker-compose.grid.yml ps
docker compose -f docker-compose.grid.yml logs selenium-hub
docker compose -f docker-compose.grid.yml logs chrome-node
```

Common causes:
- Port 4444 in use — check with `netstat -ano | findstr 4444`
- Docker not running — verify with `docker info`
- Images not pulled — re-run `docker compose -f docker-compose.grid.yml up -d`

### RemoteWebDriver session fails

```
DriverInitializationException: remoteGridUrl is not configured but seleniumGridEnabled=true
```
→ `-DgridUrl` was not set or is empty.

```
MalformedURLException: Invalid Selenium Grid URL
```
→ The URL is malformed. Correct format: `http://localhost:4444/wd/hub`

```
org.openqa.selenium.SessionNotCreatedException
```
→ The Grid hub is up but no node is registered (node not yet healthy, or crashed).
Run `Invoke-RestMethod -Uri http://localhost:4444/status` and confirm `nodes.Count > 0`.

### Readiness script timeout

```
[TIMEOUT] Grid did not become ready within 120s.
```
→ Hub or node container failed to start. Check container logs.
→ If containers are healthy but script times out, check `nodes` count in `/status` response.

---

## Known Limitations

| Limitation | Classification | Status |
|---|---|---|
| Chrome-only Grid | Test debt — TestD-001 | Deferred |
| Single node (no load balancing) | Architectural scope | Out of scope |
| Docker-on-Windows path separators in volume mounts | Platform constraint | Accepted |
| No Grid integration test in CI | Test debt — TestD-002 | Deferred |
