package com.parabank.parasoft.factory;

import com.parabank.parasoft.config.ConfigManager;
import com.parabank.parasoft.constants.BrowserConstants;
import com.parabank.parasoft.exceptions.DriverInitializationException;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

/**
 * DriverFactory - Factory pattern for WebDriver creation
 * Handles initialization of different browser drivers with centralized configuration.
 * Supports both local execution and Selenium Grid (RemoteWebDriver) via properties:
 *   seleniumGridEnabled=true/false
 *   remoteGridUrl=http://selenium-grid:4444/wd/hub
 *
 * @author Automation Team
 * @version 1.0
 */
public class DriverFactory {
    private static final Logger logger = LoggerFactory.getLogger(DriverFactory.class);
    private static final ConfigManager configManager = ConfigManager.getInstance();

    private DriverFactory() {
        // Prevent instantiation
    }

    /**
     * Creates and initializes WebDriver instance based on configuration.
     * Routes to RemoteWebDriver when seleniumGridEnabled=true, otherwise creates a local driver.
     *
     * @return initialized WebDriver
     * @throws DriverInitializationException if driver initialization fails
     */
    public static WebDriver createDriver() {
        String browserName = configManager.getBrowserName();
        logger.info("Initializing WebDriver for browser: {}", browserName);

        try {
            WebDriver driver;
            if (configManager.isSeleniumGridEnabled()) {
                driver = initializeRemoteDriver(browserName);
            } else {
                driver = initializeBrowser(browserName);
            }
            configureDriver(driver);
            logger.info("WebDriver initialized successfully");
            return driver;
        } catch (Exception e) {
            logger.error("Failed to initialize WebDriver", e);
            throw new DriverInitializationException(
                    "Failed to initialize WebDriver for browser: " + browserName, e);
        }
    }

    // ===== Local browser initialization =====

    /**
     * Initializes the appropriate local browser driver.
     */
    private static WebDriver initializeBrowser(String browserName) {
        switch (browserName.toLowerCase()) {
            case BrowserConstants.CHROME_BROWSER:
                return initializeChromeDriver();
            case BrowserConstants.FIREFOX_BROWSER:
                return initializeFirefoxDriver();
            case BrowserConstants.EDGE_BROWSER:
                return initializeEdgeDriver();
            case BrowserConstants.SAFARI_BROWSER:
                return initializeSafariDriver();
            default:
                throw new DriverInitializationException("Unsupported browser: " + browserName);
        }
    }

    private static WebDriver initializeChromeDriver() {
        logger.debug("Setting up local Chrome WebDriver");
        WebDriverManager.chromedriver().setup();
        logger.info("Chrome driver configured with options: headless={}", configManager.isHeadless());
        return new ChromeDriver(buildChromeOptions());
    }

    private static WebDriver initializeFirefoxDriver() {
        logger.debug("Setting up local Firefox WebDriver");
        WebDriverManager.firefoxdriver().setup();
        logger.info("Firefox driver configured with options: headless={}", configManager.isHeadless());
        return new FirefoxDriver(buildFirefoxOptions());
    }

    private static WebDriver initializeEdgeDriver() {
        logger.debug("Setting up local Edge WebDriver");
        WebDriverManager.edgedriver().setup();
        logger.info("Edge driver configured with options: headless={}", configManager.isHeadless());
        return new EdgeDriver(buildEdgeOptions());
    }

    private static WebDriver initializeSafariDriver() {
        logger.debug("Setting up Safari WebDriver");
        logger.info("Safari driver initialized");
        return new SafariDriver();
    }

    // ===== Selenium Grid (RemoteWebDriver) initialization =====

    /**
     * Initializes a RemoteWebDriver pointed at the configured Selenium Grid hub.
     * Reuses the same browser options as the local path (headless, window size, etc.).
     * Safari is not supported on grid nodes — use chrome, firefox, or edge.
     *
     * @param browserName the browser to request from the grid
     * @return RemoteWebDriver instance
     * @throws DriverInitializationException if URL is missing/malformed or browser unsupported
     */
    private static WebDriver initializeRemoteDriver(String browserName) {
        String gridUrl = configManager.getRemoteGridUrl();
        if (gridUrl == null || gridUrl.isBlank()) {
            throw new DriverInitializationException(
                    "remoteGridUrl is not configured but seleniumGridEnabled=true");
        }
        logger.info("Initializing RemoteWebDriver | browser: {} | grid: {}", browserName, gridUrl);
        try {
            URL gridEndpoint = new URL(gridUrl);
            switch (browserName.toLowerCase()) {
                case BrowserConstants.CHROME_BROWSER:
                    return new RemoteWebDriver(gridEndpoint, buildChromeOptions());
                case BrowserConstants.FIREFOX_BROWSER:
                    return new RemoteWebDriver(gridEndpoint, buildFirefoxOptions());
                case BrowserConstants.EDGE_BROWSER:
                    return new RemoteWebDriver(gridEndpoint, buildEdgeOptions());
                default:
                    throw new DriverInitializationException(
                            "Unsupported browser for Selenium Grid: " + browserName
                            + ". Supported: chrome, firefox, edge");
            }
        } catch (MalformedURLException e) {
            throw new DriverInitializationException(
                    "Invalid Selenium Grid URL: " + gridUrl, e);
        }
    }

    // ===== Browser options builders (shared by local and remote paths) =====

    private static ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        if (configManager.isHeadless()) {
            logger.debug("Enabling headless mode for Chrome");
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--start-maximized",
                "--disable-extensions",
                "--disable-plugins",
                "--disable-images",
                "--disable-popup-blocking",
                "--disable-notifications",
                "--disable-blink-features=AutomationControlled"
        );
        options.addArguments("--window-size=1920,1080");
        // Suppress Selenium's "Chrome is being controlled by automated software" banner
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        return options;
    }

    private static FirefoxOptions buildFirefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        if (configManager.isHeadless()) {
            logger.debug("Enabling headless mode for Firefox");
            options.addArguments("--headless");
        }
        options.addArguments("--start-maximized", "--disable-extensions");
        options.addArguments("--width=1920", "--height=1080");
        return options;
    }

    private static EdgeOptions buildEdgeOptions() {
        EdgeOptions options = new EdgeOptions();
        if (configManager.isHeadless()) {
            logger.debug("Enabling headless mode for Edge");
            options.addArguments("--headless");
        }
        options.addArguments(
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--start-maximized"
        );
        options.addArguments("--window-size=1920,1080");
        return options;
    }

    // ===== Driver configuration =====

    /**
     * Configures common WebDriver settings applied to both local and remote drivers.
     */
    private static void configureDriver(WebDriver driver) {
        logger.debug("Configuring WebDriver settings");

        // Set timeouts — implicit wait intentionally omitted; all element waits use explicit WebDriverWait via WaitUtils
        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(configManager.getPageLoadTimeout()));

        // Maximize window
        driver.manage().window().maximize();

        // Navigate to base URL
        String baseUrl = configManager.getBaseUrl();
        driver.get(baseUrl);
        logger.info("WebDriver navigated to base URL: {}", baseUrl);
    }

    /**
     * Quits the WebDriver and closes all windows.
     *
     * @param driver the WebDriver to quit
     */
    public static void quitDriver(WebDriver driver) {
        if (driver != null) {
            try {
                logger.info("Quitting WebDriver");
                driver.quit();
                logger.info("WebDriver quit successfully");
            } catch (Exception e) {
                logger.error("Error while quitting WebDriver", e);
            }
        }
    }
}
