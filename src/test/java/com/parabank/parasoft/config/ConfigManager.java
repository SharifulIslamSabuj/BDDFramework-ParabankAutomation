package com.parabank.parasoft.config;

import com.parabank.parasoft.constants.FrameworkConstants;
import com.parabank.parasoft.constants.TestDataConstants;
import com.parabank.parasoft.constants.TimeoutConstants;
import com.parabank.parasoft.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * ConfigManager - Centralized configuration management
 * Handles loading and accessing framework configuration with environment support
 *
 * @author Automation Team
 * @version 1.0
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    private Properties config;
    private String environment;

    private ConfigManager() {
        loadConfiguration();
    }

    /**
     * Gets the singleton instance of ConfigManager
     *
     * @return ConfigManager instance
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Loads configuration from properties file
     */
    private void loadConfiguration() {
        this.environment = System.getProperty("env", TestDataConstants.ENV_QA).toLowerCase();
        String fileName = environment + ".properties";
        Path configPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "config", fileName);
        config = new Properties();
        try (InputStream in = Files.exists(configPath) ? Files.newInputStream(configPath) : getClass().getClassLoader().getResourceAsStream("config/config.properties")) {
            if (in == null) {
                throw new ConfigurationException("No configuration file found for environment: " + environment);
            }
            config.load(in);
            logger.info("Configuration loaded successfully for environment: {} (from {})", environment, configPath.toString());
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            throw new ConfigurationException("Failed to load framework configuration", e);
        }
    }

    // ===== Base URL Configuration =====

    /**
     * Gets the base URL for the application
     *
     * @return base URL
     */
    public String getBaseUrl() {
        return get("baseUrl");
    }

    // ===== Browser Configuration =====

    /**
     * Gets the browser name from configuration
     *
     * @return browser name (chrome, firefox, edge, safari)
     */
    public String getBrowserName() {
        String browser = System.getProperty("browser", get("browserName", "chrome")).toLowerCase();
        logger.debug("Browser: {}", browser);
        return browser;
    }

    /**
     * Gets headless mode setting.
     * Resolution order: -Dheadless system property → properties file → {@link FrameworkConstants#DEFAULT_HEADLESS_MODE}.
     *
     * @return true if headless mode is enabled
     */
    public boolean isHeadless() {
        String sysProp = System.getProperty("headless");
        if (sysProp != null) return Boolean.parseBoolean(sysProp.trim());
        return getBoolean("headless", FrameworkConstants.DEFAULT_HEADLESS_MODE);
    }

    /**
     * Returns true when tests should run against a Selenium Grid instead of a local browser.
     * Resolution order: -DseleniumGridEnabled system property → properties file → false.
     */
    public boolean isSeleniumGridEnabled() {
        String sysProp = System.getProperty("seleniumGridEnabled");
        if (sysProp != null) return Boolean.parseBoolean(sysProp.trim());
        return getBoolean("seleniumGridEnabled", false);
    }

    /**
     * Gets the Selenium Grid hub URL (e.g. http://selenium-hub:4444/wd/hub).
     * Resolution order: -DgridUrl system property → properties file → empty string.
     */
    public String getRemoteGridUrl() {
        String sysProp = System.getProperty("gridUrl");
        if (sysProp != null && !sysProp.isBlank()) return sysProp.trim();
        return get("remoteGridUrl", "");
    }

    // ===== Timeout Configuration =====

    /**
     * Gets explicit wait timeout in seconds.
     * Defaults to {@link TimeoutConstants#EXPLICIT_WAIT_TIME} when not set in properties.
     *
     * @return timeout in seconds
     */
    public int getWaitTime() {
        return getInt("waitTime", TimeoutConstants.EXPLICIT_WAIT_TIME);
    }

    /**
     * Gets implicit wait timeout in seconds.
     * Note: implicit waits are intentionally disabled in this framework; all waits use explicit WebDriverWait.
     * This value is available for informational purposes and future use.
     * Defaults to {@link TimeoutConstants#IMPLICIT_WAIT_TIME} when not set in properties.
     *
     * @return timeout in seconds
     */
    public int getImplicitWait() {
        return getInt("implicitWait", TimeoutConstants.IMPLICIT_WAIT_TIME);
    }

    /**
     * Gets page load timeout in seconds.
     * Defaults to {@link TimeoutConstants#PAGE_LOAD_TIMEOUT} when not set in properties.
     *
     * @return timeout in seconds
     */
    public int getPageLoadTimeout() {
        return getInt("pageLoadTimeout", TimeoutConstants.PAGE_LOAD_TIMEOUT);
    }

    // ===== Test Data =====

    /**
     * Gets valid username for testing
     *
     * @return username
     */
    public String getUsername() {
        // Priority: -DTEST_USERNAME system property → TEST_USERNAME env var → properties file
        String sysProp = System.getProperty("TEST_USERNAME");
        if (sysProp != null && !sysProp.isBlank()) return sysProp;
        String envVar = System.getenv("TEST_USERNAME");
        if (envVar != null && !envVar.isBlank()) return envVar;
        return get("username");
    }

    /**
     * Gets valid password for testing.
     * Credentials are never printed in logs.
     *
     * @return password
     */
    public String getPassword() {
        // Priority: -DTEST_PASSWORD system property → TEST_PASSWORD env var → properties file
        String sysProp = System.getProperty("TEST_PASSWORD");
        if (sysProp != null && !sysProp.isBlank()) return sysProp;
        String envVar = System.getenv("TEST_PASSWORD");
        if (envVar != null && !envVar.isBlank()) return envVar;
        return get("password");
    }

    // ===== Environment Configuration =====

    /**
     * Gets the test environment
     *
     * @return environment name
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Checks if running on QA environment
     *
     * @return true if QA environment
     */
    public boolean isQAEnvironment() {
        return TestDataConstants.ENV_QA.equalsIgnoreCase(environment);
    }

    /**
     * Checks if running on Staging environment
     *
     * @return true if Staging environment
     */
    public boolean isStagingEnvironment() {
        return TestDataConstants.ENV_STAGING.equalsIgnoreCase(environment);
    }

    /**
     * Checks if running on Production environment
     *
     * @return true if Production environment
     */
    public boolean isProductionEnvironment() {
        return TestDataConstants.ENV_PRODUCTION.equalsIgnoreCase(environment);
    }

    // ===== Reporting Configuration =====

    /**
     * Gets screenshot on failure setting.
     * Defaults to {@link FrameworkConstants#DEFAULT_SCREENSHOTS_ON_FAILURE} when not set in properties.
     *
     * @return true if screenshots should be taken on failure
     */
    public boolean takeScreenshotOnFailure() {
        return getBoolean("screenshotOnFailure", FrameworkConstants.DEFAULT_SCREENSHOTS_ON_FAILURE);
    }

    /**
     * Gets screenshot on pass setting.
     * Defaults to {@link FrameworkConstants#DEFAULT_SCREENSHOT_ON_PASS} when not set in properties.
     *
     * @return true if screenshots should be taken on pass
     */
    public boolean takeScreenshotOnPass() {
        return getBoolean("screenshotOnPass", FrameworkConstants.DEFAULT_SCREENSHOT_ON_PASS);
    }

    /**
     * Gets parallel execution thread pool size
     *
     * @return thread count
     */
    public int getThreadPoolSize() {
        int threadCount = getInt("threadPoolSize", TimeoutConstants.DEFAULT_THREAD_POOL_SIZE);
        if (threadCount <= 0 || threadCount > TimeoutConstants.MAX_THREAD_POOL_SIZE) {
            logger.warn("Invalid thread pool size: {}. Using default: {}", threadCount, TimeoutConstants.DEFAULT_THREAD_POOL_SIZE);
            return TimeoutConstants.DEFAULT_THREAD_POOL_SIZE;
        }
        return threadCount;
    }

    /**
     * Resets the singleton instance (useful for testing)
     */
    public static void resetInstance() {
        instance = null;
    }

    // ===== Generic getters =====
    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, String defaultValue) {
        if (config == null) return defaultValue;
        return config.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid int for key {}: {}. Using default: {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }
}

