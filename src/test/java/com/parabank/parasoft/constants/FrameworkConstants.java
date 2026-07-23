package com.parabank.parasoft.constants;

/**
 * FrameworkConstants — default configuration values consumed by {@link
 * com.parabank.parasoft.config.ConfigManager} when a property is not set.
 *
 * Application-specific values (URLs, page titles, expected error messages)
 * were removed in Phase 4 dead-code cleanup after repository-wide analysis
 * confirmed zero executable references — the framework asserts against
 * page/feature-file literals directly instead. See
 * docs/quality/QUALITY_RISK_ASSESSMENT.md for the audit record.
 */
public final class FrameworkConstants {

    private FrameworkConstants() {}

    // ===== Default configuration booleans =====
    public static final boolean DEFAULT_HEADLESS_MODE          = false;
    public static final boolean DEFAULT_SCREENSHOTS_ON_FAILURE = true;
    public static final boolean DEFAULT_SCREENSHOT_ON_PASS     = false;
}
