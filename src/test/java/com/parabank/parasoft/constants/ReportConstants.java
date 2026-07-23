package com.parabank.parasoft.constants;

/**
 * ReportConstants — the screenshot filename timestamp format consumed by
 * {@link com.parabank.parasoft.utils.ScreenshotUtils}.
 *
 * ExtentReports/Allure configuration values and logger identifiers were
 * removed in Phase 4 dead-code cleanup after repository-wide analysis
 * confirmed zero executable references — reporting is actually configured
 * via extent-config.xml and Gradle system properties. See
 * docs/quality/QUALITY_RISK_ASSESSMENT.md for the audit record.
 */
public final class ReportConstants {

    private ReportConstants() {}

    // ===== Date / timestamp formats =====
    public static final String TIMESTAMP_FORMAT      = "yyyyMMdd_HHmmss";
}
