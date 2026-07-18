package com.parabank.parasoft.hooks;

import com.parabank.parasoft.config.ConfigManager;
import com.parabank.parasoft.driver.DriverManager;
import com.parabank.parasoft.pages.LoginPage;
import com.parabank.parasoft.pages.RegisterPage;
import com.parabank.parasoft.utils.ParaBankApiClient;
import com.parabank.parasoft.utils.ScreenshotUtils;
import com.thedeanda.lorem.LoremIpsum;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
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
     * Ensures the default test user exists in ParaBank exactly once per suite run.
     *
     * <p>Strategy (API-first with browser fallback):
     * <ol>
     *   <li>Attempt registration via HTTP POST ({@link ParaBankApiClient}) — fast, no browser.</li>
     *   <li>If the API call fails or is unavailable, fall back to browser-based registration
     *       through the framework's page object layer (LoginPage → RegisterPage → WaitUtils).</li>
     * </ol>
     *
     * <p>Double-checked locking (AtomicBoolean + synchronized) guarantees that only one
     * thread performs setup even when scenarios run in parallel.
     */
    private void ensureDefaultTestUserExists() {
        if (DEFAULT_USER_SETUP_DONE.get()) return;

        synchronized (SETUP_LOCK) {
            if (DEFAULT_USER_SETUP_DONE.get()) return;

            ConfigManager config = ConfigManager.getInstance();

            // Safety guard: fail fast before any production write is attempted
            config.guardAgainstProductionWrite("default test user registration");

            String baseUrl    = config.getBaseUrl();
            String testUsername = config.getUsername();
            String testPassword = config.getPassword();

            logger.info("[TestData] Ensuring default test user '{}' is registered (one-time setup)", testUsername);

            boolean done = tryApiRegistration(baseUrl, testUsername, testPassword);
            if (!done) {
                tryBrowserRegistration(baseUrl, testUsername, testPassword);
            }

            DEFAULT_USER_SETUP_DONE.set(true);
        }
    }

    /**
     * Attempts user registration via HTTP POST — no browser required.
     *
     * @return true if the user was registered or already exists; false on any error
     */
    private boolean tryApiRegistration(String baseUrl, String username, String password) {
        try {
            ParaBankApiClient api = new ParaBankApiClient(baseUrl);
            boolean result = api.registerUser(
                    "QA", "Engineer", "123 Test Street", "Boston", "MA",
                    "02101", "617-555-0100", "000-00-0001",
                    username, password
            );
            if (result) {
                logger.info("[TestData] API registration succeeded for user '{}'", username);
            }
            return result;
        } catch (Exception e) {
            logger.info("[TestData] API registration unavailable: {}. Using browser fallback.", e.getMessage());
            return false;
        }
    }

    /**
     * Registers the user through the browser using the framework's page object layer.
     * All element interactions go through BasePage → ElementActions → WaitUtils.
     */
    private void tryBrowserRegistration(String baseUrl, String username, String password) {
        WebDriver driver = DriverManager.getDriver();
        logger.info("[TestData] Browser registration fallback for user '{}'", username);
        try {
            driver.get(baseUrl);
            LoginPage loginPage = new LoginPage(driver);
            RegisterPage registerPage = loginPage.clickRegisterLink();
            registerPage
                    .fillFirstName(LoremIpsum.getInstance().getFirstName())
                    .fillLastName(LoremIpsum.getInstance().getLastName())
                    .fillAddress(LoremIpsum.getInstance().getTitle(3))
                    .fillCity(LoremIpsum.getInstance().getCity())
                    .fillState(LoremIpsum.getInstance().getStateAbbr())
                    .fillZipCode(LoremIpsum.getInstance().getZipCode())
                    .fillPhone(LoremIpsum.getInstance().getPhone())
                    .fillSSN(LoremIpsum.getInstance().getPhone())
                    .fillUsername(username)
                    .fillPassword(password)
                    .fillConfirmPassword(password);
            // Does not assert success: user may already exist on the demo server.
            registerPage.clickRegisterButtonWithFail();
            logger.info("[TestData] Browser registration submitted for '{}'", username);
        } catch (Exception e) {
            logger.info("[TestData] Browser registration attempt concluded (user may already exist): {}", e.getMessage());
        } finally {
            try {
                // Navigate back to login page regardless of registration outcome.
                driver.get(baseUrl + "logout.htm");
                driver.get(baseUrl + "index.htm");
                logger.info("[TestData] Driver returned to login page after browser registration");
            } catch (Exception e) {
                logger.warn("[TestData] Could not navigate back to login page: {}", e.getMessage());
            }
        }
    }
}
