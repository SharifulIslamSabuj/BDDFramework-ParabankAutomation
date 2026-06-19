package com.parabank.parasoft.constants;

/**
 * BrowserConstants — supported browser identifiers.
 * These strings are matched by DriverFactory.initializeBrowser()
 * and ConfigManager.getBrowserName() (case-insensitive).
 */
public final class BrowserConstants {

    private BrowserConstants() {}

    public static final String CHROME_BROWSER  = "chrome";
    public static final String FIREFOX_BROWSER = "firefox";
    public static final String EDGE_BROWSER    = "edge";
    public static final String SAFARI_BROWSER  = "safari";
}
