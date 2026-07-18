# ============================================================
# wait-for-grid.ps1
# Polls the Selenium Grid status endpoint until the Grid is
# ready with at least one registered browser node, or until
# the timeout is reached.
#
# Usage:
#   .\scripts\wait-for-grid.ps1
#   .\scripts\wait-for-grid.ps1 -GridStatusUrl http://localhost:4444/status -TimeoutSeconds 120
#   .\scripts\wait-for-grid.ps1 -GridStatusUrl http://localhost:4444/status -TimeoutSeconds 90 -PollIntervalSeconds 5
#
# Exit codes:
#   0  - Grid ready and at least one node registered
#   1  - Timeout reached or unrecoverable error
# ============================================================

param(
    [string] $GridStatusUrl      = "http://localhost:4444/status",
    [int]    $TimeoutSeconds     = 120,
    [int]    $PollIntervalSeconds = 5
)

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

Write-Host "============================================================"
Write-Host "  Selenium Grid Readiness Check"
Write-Host "  Status URL   : $GridStatusUrl"
Write-Host "  Timeout      : ${TimeoutSeconds}s"
Write-Host "  Poll interval: ${PollIntervalSeconds}s"
Write-Host "============================================================"

while ((Get-Date) -lt $deadline) {
    $response = $null
    $errorMsg = $null

    try {
        $response = Invoke-RestMethod -Uri $GridStatusUrl -Method Get -TimeoutSec 5 -ErrorAction Stop
    } catch {
        $errorMsg = $_.Exception.Message
    }

    if ($response -ne $null) {
        $ready     = $response.value.ready
        $nodes     = $response.value.nodes
        $nodeCount = 0
        if ($nodes -ne $null) { $nodeCount = @($nodes).Count }

        if ($ready -eq $true -and $nodeCount -gt 0) {
            Write-Host ""
            Write-Host "[OK] Grid is ready. Registered nodes: $nodeCount"
            foreach ($n in $nodes) {
                $slotCount = 0
                if ($n.slots -ne $null) { $slotCount = @($n.slots).Count }
                Write-Host "     Node: $($n.uri)  slots: $slotCount"
            }
            Write-Host "============================================================"
            exit 0
        }

        $remaining = [int](($deadline - (Get-Date)).TotalSeconds)
        Write-Host "[WAIT] Grid ready=$ready nodes=$nodeCount - ${remaining}s remaining"
    } else {
        $remaining = [int](($deadline - (Get-Date)).TotalSeconds)
        Write-Host "[WAIT] Grid not reachable ($errorMsg) - ${remaining}s remaining"
    }

    Start-Sleep -Seconds $PollIntervalSeconds
}

Write-Host ""
Write-Host "[TIMEOUT] Grid did not become ready within ${TimeoutSeconds}s."
Write-Host "  Verify containers: docker compose -f docker-compose.grid.yml ps"
Write-Host "  Hub logs:          docker compose -f docker-compose.grid.yml logs selenium-hub"
Write-Host "  Node logs:         docker compose -f docker-compose.grid.yml logs chrome-node"
Write-Host "============================================================"
exit 1
