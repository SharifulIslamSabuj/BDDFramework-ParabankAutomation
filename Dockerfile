# ==============================================================
# ParaBank BDD Automation Framework — Docker Image
# ==============================================================
# Base  : eclipse-temurin:17-jdk-jammy  (Ubuntu 22.04 + Java 17)
# Chrome: Google Chrome stable via apt (WebDriverManager auto-manages chromedriver)
# Xvfb  : Virtual X11 display — framework qa.properties uses headless=false;
#          Xvfb provides a real display so Chrome opens normally on a headless host
# ==============================================================

FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="Automation Team" \
      description="ParaBank Selenium BDD Automation Framework" \
      framework="Cucumber + Selenium 4 + TestNG + Gradle 9"

# ── System dependencies + Chrome stable ──────────────────────────────────────
# Install in one RUN layer to minimise image layers and size.
# google-chrome-stable pulls in all its own shared-library dependencies via apt.
RUN apt-get update && apt-get install -y --no-install-recommends \
        wget \
        gnupg2 \
        ca-certificates \
        xvfb \
        fonts-liberation \
        xdg-utils \
    && wget -qO - https://dl-ssl.google.com/linux/linux_signing_key.pub \
       | gpg --dearmor -o /usr/share/keyrings/google-linux-signing-key.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-linux-signing-key.gpg] \
       http://dl.google.com/linux/chrome/deb/ stable main" \
       > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends google-chrome-stable \
    && rm -rf /var/lib/apt/lists/* \
    && google-chrome --version

# ── Working directory ─────────────────────────────────────────────────────────
# All FrameworkConstants paths derive from System.getProperty("user.dir"),
# which is the process working directory.  Setting WORKDIR=/app makes
# /app the project root inside the container.
WORKDIR /app

# ── Layer 1: Gradle wrapper ───────────────────────────────────────────────────
# Copying wrapper files before source means this layer is only invalidated
# when the Gradle version changes (not on every source edit).
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
RUN chmod +x gradlew

# ── Layer 2: Dependency manifest ─────────────────────────────────────────────
# Copying build descriptor before source enables Gradle dependency caching:
# the next RUN (dependencies download) is only re-executed when build.gradle
# or settings.gradle changes.
COPY build.gradle settings.gradle ./

# Pre-download the Gradle distribution and all project dependencies.
# The '|| true' allows the image build to succeed even if a transient
# network issue occurs; the actual test run will re-attempt if needed.
RUN ./gradlew dependencies --no-daemon --quiet 2>&1 | tail -5 || true

# ── Layer 3: Test source and resources ───────────────────────────────────────
# This layer is invalidated on any source change, but layer 2 (deps) stays
# cached — avoiding a full re-download on every code change.
COPY src/ src/

# ── Entrypoint ────────────────────────────────────────────────────────────────
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# ── JVM tuning ────────────────────────────────────────────────────────────────
# -Xmx1g caps heap so the container does not OOM on shared CI runners.
# Adjust via JAVA_OPTS in docker-compose.yml or docker run -e JAVA_OPTS=...
ENV JAVA_OPTS="-Xmx1g -Xms512m"

# ── Default env / browser (overridable via docker-compose environment: or -e) ─
ENV ENV=qa \
    BROWSER=chrome \
    MAX_PARALLEL_FORKS=1 \
    DATA_PROVIDER_THREAD_COUNT=1

ENTRYPOINT ["/docker-entrypoint.sh"]
