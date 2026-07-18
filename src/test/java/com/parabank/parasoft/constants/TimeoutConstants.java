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
    public static final int FLUENT_WAIT_POLLING_INTERVAL = 2;

    // ===== Parallel execution limits =====
    public static final int DEFAULT_THREAD_POOL_SIZE  = 2;
    public static final int MAX_THREAD_POOL_SIZE      = 4;

    // ===== Millisecond intervals =====
    public static final long POLLING_INTERVAL_MILLIS  = 500L;
}
