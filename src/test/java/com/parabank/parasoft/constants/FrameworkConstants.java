package com.parabank.parasoft.constants;

/**
 * FrameworkConstants — ParaBank application-specific constants.
 *
 * Infrastructure constants (browser names, timeouts, paths, report config,
 * environment names, Cucumber tags) have been moved to dedicated classes:
 *   BrowserConstants  — browser identifiers
 *   TimeoutConstants  — timeout and polling values
 *   PathConstants     — file-system paths and file names
 *   ReportConstants   — report configuration and formats
 *   TestDataConstants — environment names, tags, selectors
 *
 * This class retains only values that are specific to the ParaBank application
 * under test: URLs, page titles, and expected error messages.
 */
public final class FrameworkConstants {

    private FrameworkConstants() {}

    // ===== Application URLs =====
    public static final String PARABANK_BASE_URL = "https://parabank.parasoft.com/parabank/";
    public static final String LOGIN_PAGE_URL    = PARABANK_BASE_URL + "login.htm";
    public static final String REGISTER_PAGE_URL = PARABANK_BASE_URL + "register.htm";

    // ===== Page titles =====
    public static final String PARABANK_TITLE       = "ParaBank | Welcome | Online Banking";
    public static final String LOGIN_PAGE_TITLE     = "ParaBank | Welcome | Online Banking";
    public static final String OVERVIEW_PAGE_TITLE  = "ParaBank | Accounts | Overview";

    // ===== Expected application error messages =====
    // Verified against the live ParaBank demo server (parabank.parasoft.com).
    // The demo server returns two distinct validation messages:
    //   - Blank credentials  → "Please enter a username and password."
    //   - All other failures → "The username and password could not be verified."
    public static final String ERROR_INVALID_CREDENTIALS    = "The username and password could not be verified.";
    public static final String ERROR_EMPTY_USERNAME         = "Please enter a username and password.";
    public static final String ERROR_EMPTY_PASSWORD         = "Please enter a username and password.";
    public static final String ERROR_EMPTY_BOTH             = "Please enter a username and password.";
    public static final String ERROR_USERNAME_LENGTH        = "The username and password could not be verified.";
    public static final String ERROR_PASSWORD_LENGTH        = "The username and password could not be verified.";
    public static final String ERROR_INVALID_USERNAME_FORMAT = "The username and password could not be verified.";
    public static final String ERROR_INVALID_PASSWORD       = "The username and password could not be verified.";
    public static final String ERROR_LOCKED_ACCOUNT         = "The username and password could not be verified.";

    // ===== Default configuration booleans =====
    public static final boolean DEFAULT_HEADLESS_MODE          = false;
    public static final boolean DEFAULT_SCREENSHOTS_ON_FAILURE = true;
    public static final boolean DEFAULT_SCREENSHOT_ON_PASS     = false;
}
