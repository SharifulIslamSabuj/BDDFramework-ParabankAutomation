package com.parabank.parasoft.constants;

/**
 * FrameworkConstants - Centralized constants for the entire framework
 * Provides configuration values, timeouts, paths, and other constants
 *
 * @author Automation Team
 * @version 1.0
 */
public final class FrameworkConstants {

    // Prevent instantiation
    private FrameworkConstants() {
    }

    // ===== Application URLs =====
    public static final String PARABANK_BASE_URL = "https://parabank.parasoft.com/parabank/";
    public static final String LOGIN_PAGE_URL = PARABANK_BASE_URL + "login.htm";
    public static final String REGISTER_PAGE_URL = PARABANK_BASE_URL + "register.htm";

    // ===== Timeout Values (in seconds) =====
    public static final int EXPLICIT_WAIT_TIME = 30;
    public static final int IMPLICIT_WAIT_TIME = 20;
    public static final int ELEMENT_FINDING_TIMEOUT = 15;
    public static final int PAGE_LOAD_TIMEOUT = 45;
    public static final int FLUENT_WAIT_POLLING_INTERVAL = 2;

    // ===== Browser Configuration =====
    public static final String CHROME_BROWSER = "chrome";
    public static final String FIREFOX_BROWSER = "firefox";
    public static final String EDGE_BROWSER = "edge";
    public static final String SAFARI_BROWSER = "safari";

    // ===== Environment Configuration =====
    public static final String ENV_QA = "qa";
    public static final String ENV_STAGING = "staging";
    public static final String ENV_PRODUCTION = "prod";

    // ===== Paths =====
    public static final String PROJECT_ROOT = System.getProperty("user.dir");
    public static final String RESOURCES_PATH = PROJECT_ROOT + "/src/test/resources";
    public static final String CONFIG_PATH = RESOURCES_PATH + "/config";
    public static final String DATA_PATH = RESOURCES_PATH + "/data";
    public static final String SCREENSHOTS_PATH = PROJECT_ROOT + "/build/screenshots";
    public static final String REPORTS_PATH = PROJECT_ROOT + "/build/reports";
    public static final String EXTENT_REPORTS_PATH = REPORTS_PATH + "/extent";
    public static final String ALLURE_RESULTS_PATH = PROJECT_ROOT + "/allure-results";
    public static final String LOGS_PATH = PROJECT_ROOT + "/build/logs";
    public static final String FEATURES_PATH = RESOURCES_PATH + "/features";

    // ===== File Names =====
    public static final String CONFIG_FILE_NAME = "config.properties";
    public static final String EXTENT_CONFIG_FILE = "extent-config.xml";
    public static final String LOGBACK_CONFIG_FILE = "logback.xml";
    public static final String TEST_DATA_FILE = "ddt.xlsx";
    public static final String TEST_DATA_SHEET = "Sheet1";

    // ===== Reporting =====
    public static final String REPORT_NAME = "Automated_Test_Report.html";
    public static final String REPORT_THEME = "STANDARD";
    public static final String REPORT_ENCODING = "utf-8";
    public static final String REPORT_TITLE = "ParaBank Automation Test Report";
    public static final String REPORT_DOCUMENT_TITLE = "Test Execution Report";

    // ===== Logging =====
    public static final String LOGGER_NAME = "framework-logger";
    public static final String LOG_FILE_NAME = "test-execution.log";

    // ===== Parallel Execution =====
    public static final int DEFAULT_THREAD_POOL_SIZE = 2;
    public static final int MAX_THREAD_POOL_SIZE = 4;

    // ===== Waits & Polling =====
    public static final long POLLING_INTERVAL_MILLIS = 500;
    public static final long SCREENSHOT_DELAY_MILLIS = 1000;

    // ===== Element Locators (Common) =====
    public static final String ERROR_MESSAGE_SELECTOR = "p.error";
    public static final String SUCCESS_MESSAGE_SELECTOR = "p.success";
    public static final String LOGOUT_LINK_XPATH = "//a[contains(text(), 'Logout')]";

    // ===== Test Data Constants =====
    public static final String VALID_USERNAME = "sqa";
    public static final String VALID_PASSWORD = "sqa";
    public static final String INVALID_USERNAME = "invalidUser";
    public static final String INVALID_PASSWORD = "wrongPass";

    // ===== Error Messages (Expected from App) =====
    public static final String ERROR_INVALID_CREDENTIALS = "The username and password could not be verified.";
    public static final String ERROR_EMPTY_USERNAME = "Please enter username.";
    public static final String ERROR_EMPTY_PASSWORD = "Please enter password.";
    public static final String ERROR_EMPTY_BOTH = "Please enter username and password.";
    public static final String ERROR_USERNAME_LENGTH = "Username exceeds maximum length.";
    public static final String ERROR_PASSWORD_LENGTH = "Password exceeds maximum length.";
    public static final String ERROR_INVALID_USERNAME_FORMAT = "Invalid username format.";
    public static final String ERROR_INVALID_PASSWORD = "Please enter a valid password.";
    public static final String ERROR_LOCKED_ACCOUNT = "Your account is locked. Please contact customer support.";

    // ===== Feature File Tags =====
    public static final String TAG_SMOKE = "@smoke";
    public static final String TAG_REGRESSION = "@regression";
    public static final String TAG_NEGATIVE = "@negative";
    public static final String TAG_POSITIVE = "@positive";
    public static final String TAG_HARDCODED = "@hardcoded";
    public static final String TAG_LOREM = "@ipsum";
    public static final String TAG_DDT = "@ddt";

    // ===== Default Values =====
    public static final boolean DEFAULT_HEADLESS_MODE = false;
    public static final boolean DEFAULT_SCREENSHOTS_ON_FAILURE = true;
    public static final boolean DEFAULT_SCREENSHOT_ON_PASS = false;

    // ===== Page Titles =====
    public static final String PARABANK_TITLE = "ParaBank | Welcome | Online Banking";
    public static final String LOGIN_PAGE_TITLE = "ParaBank | Welcome | Online Banking";
    public static final String OVERVIEW_PAGE_TITLE = "ParaBank | Accounts | Overview";

    // ===== Special Characters & Delimiters =====
    public static final String COMMA_SEPARATOR = ",";
    public static final String PIPE_SEPARATOR = "|";
    public static final String UNDERSCORE = "_";
    public static final String HYPHEN = "-";

    // ===== Date Format =====
    public static final String DATE_FORMAT_DDMMYYYY = "dd-MM-yyyy";
    public static final String DATE_FORMAT_DDMMMYYYY = "dd-MMM-yyyy";
    public static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
}

