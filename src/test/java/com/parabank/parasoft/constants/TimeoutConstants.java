package com.parabank.parasoft.constants;

/**
 * TimeoutConstants — all numeric timeout and polling values used by
 * WaitUtils, ConfigManager, and DriverFactory.
 * Values are in seconds unless the field name contains "MILLIS".
 */
public final class TimeoutConstants {

    private TimeoutConstants() {}

    // ===== Explicit waits (seconds) =====
    public static final int EXPLICIT_WAIT_TIME        = 30;
    public static final int PAGE_LOAD_TIMEOUT         = 45;
}
