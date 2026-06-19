package com.parabank.parasoft.config;

import com.parabank.parasoft.constants.FrameworkConstants;
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
        this.environment = System.getProperty("env", FrameworkConstants.ENV_QA).toLowerCase();
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
     * Gets headless mode setting
     *
     * @return true if headless mode is enabled
     */
    public boolean isHeadless() {
        return getBoolean("headless", false);
    }

    /**
     * Returns true when tests should run against a Selenium Grid instead of a local browser.
     */
    public boolean isSeleniumGridEnabled() {
        return getBoolean("seleniumGridEnabled", false);
    }

    /**
     * Gets the Selenium Grid hub URL (e.g. http://selenium-grid:4444/wd/hub).
     */
    public String getRemoteGridUrl() {
        return get("remoteGridUrl", "");
    }

    // ===== Timeout Configuration =====

    /**
     * Gets explicit wait timeout in seconds
     *
     * @return timeout value
     */
    public int getWaitTime() {
        return getInt("waitTime", 30);
    }

    /**
     * Gets implicit wait timeout in seconds
     *
     * @return timeout in seconds
     */
    public int getImplicitWait() {
        return getInt("implicitWait", 20);
    }

    /**
     * Gets page load timeout in seconds
     *
     * @return timeout in seconds
     */
    public int getPageLoadTimeout() {
        return getInt("pageLoadTimeout", 45);
    }

    // ===== Test Data =====

    /**
     * Gets valid username for testing
     *
     * @return username
     */
    public String getUsername() {
        String envUser = System.getenv("TEST_USERNAME");
        if (envUser != null && !envUser.isBlank()) return envUser;
        return get("username");
    }

    /**
     * Gets valid password for testing
     *
     * @return password
     */
    public String getPassword() {
        String envPass = System.getenv("TEST_PASSWORD");
        if (envPass != null && !envPass.isBlank()) return envPass;
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
        return FrameworkConstants.ENV_QA.equalsIgnoreCase(environment);
    }

    /**
     * Checks if running on Staging environment
     *
     * @return true if Staging environment
     */
    public boolean isStagingEnvironment() {
        return FrameworkConstants.ENV_STAGING.equalsIgnoreCase(environment);
    }

    /**
     * Checks if running on Production environment
     *
     * @return true if Production environment
     */
    public boolean isProductionEnvironment() {
        return FrameworkConstants.ENV_PRODUCTION.equalsIgnoreCase(environment);
    }

    // ===== Reporting Configuration =====

    /**
     * Gets screenshot on failure setting
     *
     * @return true if screenshots should be taken on failure
     */
    public boolean takeScreenshotOnFailure() {
        return getBoolean("screenshotOnFailure", true);
    }

    /**
     * Gets screenshot on pass setting
     *
     * @return true if screenshots should be taken on pass
     */
    public boolean takeScreenshotOnPass() {
        return getBoolean("screenshotOnPass", false);
    }

    /**
     * Gets parallel execution thread pool size
     *
     * @return thread count
     */
    public int getThreadPoolSize() {
        int threadCount = getInt("threadPoolSize", FrameworkConstants.DEFAULT_THREAD_POOL_SIZE);
        if (threadCount <= 0 || threadCount > FrameworkConstants.MAX_THREAD_POOL_SIZE) {
            logger.warn("Invalid thread pool size: {}. Using default: {}", threadCount, FrameworkConstants.DEFAULT_THREAD_POOL_SIZE);
            return FrameworkConstants.DEFAULT_THREAD_POOL_SIZE;
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

