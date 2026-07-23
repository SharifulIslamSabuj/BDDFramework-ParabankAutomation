package com.parabank.parasoft.constants;

/**
 * TestDataConstants — environment identifiers consumed by {@link
 * com.parabank.parasoft.config.ConfigManager} for environment-name comparisons.
 *
 * Cucumber tag strings and the default tag expression were removed in Phase 4
 * dead-code cleanup after repository-wide analysis confirmed zero executable
 * references — tag filtering is driven directly by {@code build.gradle}'s
 * {@code cucumber.filter.tags} system property instead. See
 * docs/quality/QUALITY_RISK_ASSESSMENT.md for the audit record.
 */
public final class TestDataConstants {

    private TestDataConstants() {}

    // ===== Environment names =====
    public static final String ENV_QA         = "qa";
    public static final String ENV_STAGING    = "staging";
}
