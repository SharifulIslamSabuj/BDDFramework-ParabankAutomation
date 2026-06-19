#!/bin/bash
# ==============================================================
# ParaBank BDD Automation — Container Entrypoint
# ==============================================================
# Wraps ./gradlew clean test in xvfb-run so that Chrome can open
# real windows on a headless Linux host (the framework's qa.properties
# sets headless=false; we provide a virtual display instead of
# modifying framework configuration).
#
# All parameters are resolved from environment variables with sensible
# defaults, and map directly to the existing Gradle -D system properties
# that ConfigManager and build.gradle already understand.
#
# Grid execution: set SELENIUM_GRID_ENABLED=true and GRID_URL.
# When using docker-compose.grid.yml these are pre-configured.
# ==============================================================

set -euo pipefail

ENV="${ENV:-qa}"
BROWSER="${BROWSER:-chrome}"
MAX_PARALLEL_FORKS="${MAX_PARALLEL_FORKS:-1}"
DATA_PROVIDER_THREAD_COUNT="${DATA_PROVIDER_THREAD_COUNT:-1}"
JAVA_OPTS="${JAVA_OPTS:--Xmx1g -Xms512m}"
SELENIUM_GRID_ENABLED="${SELENIUM_GRID_ENABLED:-false}"
GRID_URL="${GRID_URL:-}"

echo "============================================================"
echo "  ParaBank BDD Automation Framework"
echo "============================================================"
echo "  Environment            : ${ENV}"
echo "  Browser                : ${BROWSER}"
echo "  Max parallel forks     : ${MAX_PARALLEL_FORKS}"
echo "  DataProvider threads   : ${DATA_PROVIDER_THREAD_COUNT}"
echo "  JVM options            : ${JAVA_OPTS}"
echo "  Selenium Grid enabled  : ${SELENIUM_GRID_ENABLED}"
echo "  Grid URL               : ${GRID_URL:-<local browser>}"
echo "  Chrome version         : $(google-chrome --version 2>/dev/null || echo 'unknown')"
echo "  Java version           : $(java -version 2>&1 | head -1)"
echo "============================================================"

# Ensure artifact output directories exist so volume mounts
# are populated even if the framework creates them lazily.
mkdir -p build/reports/extent \
         build/screenshots \
         build/logs \
         allure-results

# Execute tests inside a 1920x1080 virtual X11 display.
# --auto-servernum avoids conflicts when multiple containers
# share the same host (each gets a unique display number).
# When SELENIUM_GRID_ENABLED=true, Chrome runs on the Grid node
# (not locally), so Xvfb is unused but harmless.
exec xvfb-run \
    --auto-servernum \
    --server-args="-screen 0 1920x1080x24" \
    ./gradlew clean test \
        -Denv="${ENV}" \
        -Dbrowser="${BROWSER}" \
        -DseleniumGridEnabled="${SELENIUM_GRID_ENABLED}" \
        -DgridUrl="${GRID_URL}" \
        -DmaxParallelForks="${MAX_PARALLEL_FORKS}" \
        -Ddataproviderthreadcount="${DATA_PROVIDER_THREAD_COUNT}" \
        --no-daemon \
        --console=plain
