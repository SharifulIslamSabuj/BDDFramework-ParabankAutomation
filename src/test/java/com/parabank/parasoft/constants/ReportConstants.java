package com.parabank.parasoft.constants;

/**
 * ReportConstants — ExtentReports / Allure configuration values,
 * date/timestamp formats, and logger identifiers used across
 * reporting and utility classes.
 */
public final class ReportConstants {

    private ReportConstants() {}

    // ===== ExtentReports =====
    public static final String REPORT_NAME           = "Automated_Test_Report.html";
    public static final String REPORT_THEME          = "STANDARD";
    public static final String REPORT_ENCODING       = "utf-8";
    public static final String REPORT_TITLE          = "ParaBank Automation Test Report";
    public static final String REPORT_DOCUMENT_TITLE = "Test Execution Report";

    // ===== Logging =====
    public static final String LOGGER_NAME    = "framework-logger";
    public static final String LOG_FILE_NAME  = "test-execution.log";

    // ===== Date / timestamp formats =====
    public static final String DATE_FORMAT_DDMMYYYY  = "dd-MM-yyyy";
    public static final String DATE_FORMAT_DDMMMYYYY = "dd-MMM-yyyy";
    public static final String TIMESTAMP_FORMAT      = "yyyyMMdd_HHmmss";
}
