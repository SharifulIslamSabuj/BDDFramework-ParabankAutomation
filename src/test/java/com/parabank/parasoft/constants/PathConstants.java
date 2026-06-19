package com.parabank.parasoft.constants;

/**
 * PathConstants — all file-system paths and file names.
 * Paths are anchored to System.getProperty("user.dir"), which equals
 * the Gradle project root at runtime (set by the JVM before tests begin).
 */
public final class PathConstants {

    private PathConstants() {}

    // ===== Project root =====
    public static final String PROJECT_ROOT    = System.getProperty("user.dir");

    // ===== Source paths =====
    public static final String RESOURCES_PATH  = PROJECT_ROOT + "/src/test/resources";
    public static final String CONFIG_PATH     = RESOURCES_PATH + "/config";
    public static final String DATA_PATH       = RESOURCES_PATH + "/data";
    public static final String FEATURES_PATH   = RESOURCES_PATH + "/features";

    // ===== Build output paths =====
    public static final String SCREENSHOTS_PATH    = PROJECT_ROOT + "/build/screenshots";
    public static final String REPORTS_PATH        = PROJECT_ROOT + "/build/reports";
    public static final String EXTENT_REPORTS_PATH = REPORTS_PATH + "/extent";
    public static final String ALLURE_RESULTS_PATH = PROJECT_ROOT + "/allure-results";
    public static final String LOGS_PATH           = PROJECT_ROOT + "/build/logs";

    // ===== File names =====
    public static final String CONFIG_FILE_NAME  = "config.properties";
    public static final String EXTENT_CONFIG_FILE = "extent-config.xml";
    public static final String LOGBACK_CONFIG_FILE = "logback.xml";
    public static final String TEST_DATA_FILE    = "ddt.xlsx";
    public static final String TEST_DATA_SHEET   = "Sheet1";
}
