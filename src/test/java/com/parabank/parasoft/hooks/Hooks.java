package com.parabank.parasoft.hooks;

import com.parabank.parasoft.config.ConfigManager;
import com.parabank.parasoft.driver.DriverManager;
import com.parabank.parasoft.utils.ScreenshotUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hooks - Cucumber lifecycle hooks for scenario setup and teardown
 * Manages WebDriver initialization and cleanup for each scenario
 * Handles screenshot capture on failure
 *
 * @author Automation Team
 * @version 1.0
 */
public class Hooks {
    private static final Logger logger = LoggerFactory.getLogger(Hooks.class);

    // Guarantees the default test user is created exactly once across all parallel threads
    private static final AtomicBoolean DEFAULT_USER_SETUP_DONE = new AtomicBoolean(false);
    private static final Object SETUP_LOCK = new Object();

    /**
     * Before hook - Runs before each scenario
     * Initializes WebDriver for current thread.
     * On the very first scenario only, ensures the default test user exists in ParaBank.
     *
     * @param scenario the Cucumber scenario
     */
    @Before
    public void beforeScenario(Scenario scenario) {
        MDC.put("scenarioName", scenario.getName());
        logger.info("====== SCENARIO STARTED ======");
        logger.info("Scenario: {}", scenario.getName());
        logger.info("Thread ID: {}", Thread.currentThread().getId());

        // Initialize WebDriver for current thread
        DriverManager.initializeDriver();

        // Ensure default test user 'sqa' is registered once before the suite begins
        ensureDefaultTestUserExists();

        logger.info("WebDriver initialized successfully");
    }

    /**
     * After hook - Runs after each scenario
     * Captures screenshot on failure and cleans up WebDriver
     *
     * @param scenario the Cucumber scenario
     */
    @After
    public void afterScenario(Scenario scenario) {
        logger.info("====== SCENARIO COMPLETED ======");
        logger.info("Scenario Status: {}", scenario.getStatus());

        if (scenario.isFailed()) {
            logger.error("Scenario FAILED: {}", scenario.getName());

            // Capture screenshot on failure
            if (DriverManager.isDriverInitialized()) {
                try {
                    String screenshotPath = ScreenshotUtils.captureScreenshot(
                            DriverManager.getDriver(),
                            scenario.getName()
                    );
                    logger.info("Screenshot captured: {}", screenshotPath);

                    // Attach screenshot to Cucumber report
                    byte[] screenshot = ScreenshotUtils.captureScreenshotAsBytes(DriverManager.getDriver());
                    scenario.attach(screenshot, "image/png", scenario.getName() + "_failure");
                } catch (Exception e) {
                    logger.error("Failed to capture screenshot on failure", e);
                }
            }
        } else {
            logger.info("Scenario PASSED: {}", scenario.getName());
        }

        // Quit WebDriver
        DriverManager.quitDriver();
        logger.info("WebDriver quit successfully");
        MDC.remove("scenarioName");
    }

    /**
     * Registers the default test user 'sqa' in ParaBank once per test suite run.
     * Uses double-checked locking so only one thread performs registration
     * even under parallel execution. If the user already exists, the attempt
     * is silently ignored. The driver is always returned to the base URL
     * before this method returns.
     */
    private void ensureDefaultTestUserExists() {
        if (DEFAULT_USER_SETUP_DONE.get()) return;

        synchronized (SETUP_LOCK) {
            if (DEFAULT_USER_SETUP_DONE.get()) return;

            WebDriver driver = DriverManager.getDriver();
            ConfigManager config = ConfigManager.getInstance();
            String baseUrl = config.getBaseUrl();
            String testUsername = config.getUsername();
            String testPassword = config.getPassword();
            logger.info("[TestData] Registering default test user '{}' (one-time suite setup)", testUsername);
            try {
                driver.get(baseUrl + "register.htm");
                driver.findElement(By.id("customer.firstName")).sendKeys("QA");
                driver.findElement(By.id("customer.lastName")).sendKeys("Engineer");
                driver.findElement(By.id("customer.address.street")).sendKeys("123 Main St");
                driver.findElement(By.id("customer.address.city")).sendKeys("Boston");
                driver.findElement(By.id("customer.address.state")).sendKeys("MA");
                driver.findElement(By.id("customer.address.zipCode")).sendKeys("02108");
                driver.findElement(By.id("customer.phoneNumber")).sendKeys("617-555-1212");
                driver.findElement(By.id("customer.ssn")).sendKeys("999-99-9999");
                driver.findElement(By.id("customer.username")).sendKeys(testUsername);
                driver.findElement(By.id("customer.password")).sendKeys(testPassword);
                driver.findElement(By.id("repeatedPassword")).sendKeys(testPassword);
                driver.findElement(By.cssSelector("input[value='Register']")).click();
                logger.info("[TestData] Default test user '{}' registration request submitted", testUsername);
            } catch (Exception e) {
                // User may already exist — this is not a fatal error
                logger.info("[TestData] Registration attempt concluded (user may already exist): {}", e.getMessage());
            } finally {
                DEFAULT_USER_SETUP_DONE.set(true);
                try {
                    // Logout first in case registration succeeded and auto-logged the user in.
                    // This ensures the driver lands on the login form regardless of session state.
                    driver.get(baseUrl + "logout.htm");
                    driver.get(baseUrl + "index.htm");
                    logger.info("[TestData] Driver returned to login page after test data setup");
                } catch (Exception e) {
                    logger.warn("[TestData] Could not navigate back to login page: {}", e.getMessage());
                }
            }
        }
    }
}
