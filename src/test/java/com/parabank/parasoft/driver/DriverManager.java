package com.parabank.parasoft.driver;

import com.parabank.parasoft.factory.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DriverManager - Thread-safe WebDriver management using ThreadLocal
 * Enables parallel test execution with isolated driver instances per thread
 *
 * @author Automation Team
 * @version 1.0
 */
public class DriverManager {
    private static final Logger logger = LoggerFactory.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    private DriverManager() {
        // Prevent instantiation
    }

    /**
     * Gets the WebDriver instance for the current thread
     *
     * @return WebDriver instance
     */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver == null) {
            logger.warn("WebDriver not initialized for thread: {}", Thread.currentThread().getId());
        }
        return driver;
    }

    /**
     * Sets the WebDriver instance for the current thread
     *
     * @param driver the WebDriver to set
     */
    public static void setDriver(WebDriver driver) {
        DRIVER_THREAD_LOCAL.set(driver);
        logger.info("WebDriver set for thread: {}", Thread.currentThread().getId());
    }

    /**
     * Initializes WebDriver for current thread using DriverFactory
     */
    public static void initializeDriver() {
        if (DRIVER_THREAD_LOCAL.get() == null) {
            logger.debug("Initializing WebDriver for thread: {}", Thread.currentThread().getId());
            WebDriver driver = DriverFactory.createDriver();
            setDriver(driver);
        }
    }

    /**
     * Quits the WebDriver for current thread
     */
    public static void quitDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver != null) {
            logger.info("Quitting WebDriver for thread: {}", Thread.currentThread().getId());
            DriverFactory.quitDriver(driver);
            DRIVER_THREAD_LOCAL.remove();
            logger.info("WebDriver removed from ThreadLocal");
        }
    }

    /**
     * Checks if WebDriver is initialized for current thread
     *
     * @return true if WebDriver is initialized
     */
    public static boolean isDriverInitialized() {
        return DRIVER_THREAD_LOCAL.get() != null;
    }

    /**
     * Clears the WebDriver ThreadLocal (used for cleanup)
     */
    public static void removeDriver() {
        DRIVER_THREAD_LOCAL.remove();
        logger.debug("WebDriver removed from ThreadLocal for thread: {}", Thread.currentThread().getId());
    }
}