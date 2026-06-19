package com.parabank.parasoft.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * LoginPage - Page object for ParaBank login page
 * Uses fluent API for chaining method calls
 * All locators are private and final for encapsulation
 *
 * @author Automation Team
 */
public class LoginPage extends BasePage {

    // ===== Private Locators =====
    private static final By USERNAME_INPUT = By.cssSelector("input[name='username']");
    private static final By PASSWORD_INPUT = By.xpath("//input[@name='password']");
    private static final By LOGIN_BUTTON = By.cssSelector("input[value='Log In']");
    private static final By REGISTER_LINK = By.cssSelector("a[href='register.htm']");
    private static final By ERROR_MESSAGE = By.cssSelector("p.error");
    private static final By SUCCESS_MESSAGE = By.cssSelector("div.success");
    private static final By LOGOUT_LINK = By.cssSelector("a[href='logout.htm']");

    /**
     * Constructor
     *
     * @param driver the WebDriver instance
     */
    public LoginPage(WebDriver driver) {
        super(driver);
        logger.info("LoginPage initialized");
    }

    // ===== Fluent API Methods =====

    /**
     * Fills username field
     *
     * @param username the username to enter
     * @return this LoginPage for fluent chaining
     */
    public LoginPage fillUsername(String username) {
        logger.info("Filling username: {}", username);
        sendKeys(USERNAME_INPUT, username);
        return this;
    }

    /**
     * Fills password field
     *
     * @param password the password to enter
     * @return this LoginPage for fluent chaining
     */
    public LoginPage fillPassword(String password) {
        logger.info("Filling password");
        sendKeys(PASSWORD_INPUT, password);
        return this;
    }

    /**
     * Clicks login button and determines outcome.
     *
     * @return OverviewPage on successful login, null if login failed
     */
    public OverviewPage clickLoginButton() {
        logger.info("Clicking login button");
        click(LOGIN_BUTTON);

        // Wait for either the logout link (success) OR the error message (failure) to appear
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(configManager.getWaitTime()));
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(LOGOUT_LINK),
                ExpectedConditions.visibilityOfElementLocated(ERROR_MESSAGE)
            ));
        } catch (Exception e) {
            logger.debug("Wait for login outcome completed or timed out: {}", e.getMessage());
        }

        // Now check if we are logged in (logout link is present) or if there is an error
        if (getElementCount(LOGOUT_LINK) > 0 && isDisplayed(LOGOUT_LINK)) {
            logger.info("Login successful - navigating to OverviewPage");
            return createPage(OverviewPage.class);
        } else {
            logger.warn("Login failed - remained on LoginPage");
            return null;
        }
    }

    /**
     * Clicks register link
     *
     * @return RegisterPage instance
     */
    public RegisterPage clickRegisterLink() {
        logger.info("Clicking register link");
        click(REGISTER_LINK);
        return createPage(RegisterPage.class);
    }

    /**
     * Performs complete login flow.
     *
     * @param username the username
     * @param password the password
     * @return OverviewPage on success, null on failure
     */
    public OverviewPage doLogin(String username, String password) {
        logger.info("Performing login");
        return fillUsername(username)
                .fillPassword(password)
                .clickLoginButton();
    }

    // ===== Assertion Methods =====

    /**
     * Checks if any error message is displayed
     *
     * @return true if error message is visible
     */
    public boolean hasErrorMessage() {
        logger.debug("Checking for error messages");
        try {
            return getElementCount(ERROR_MESSAGE) > 0 && isDisplayed(ERROR_MESSAGE);
        } catch (Exception e) {
            logger.debug("No error messages found");
            return false;
        }
    }

    /**
     * Checks if specific error message text is displayed
     *
     * @param message the expected error message
     * @return true if message is displayed
     */
    public boolean hasErrorMessage(String message) {
        logger.debug("Checking for error message: {}", message);
        try {
            if (!isDisplayed(ERROR_MESSAGE)) {
                return false;
            }
            String errorText = getText(ERROR_MESSAGE);
            return errorText.contains(message);
        } catch (Exception e) {
            logger.debug("Error checking for specific message: {}", message);
            return false;
        }
    }

    /**
     * Checks if success message is displayed
     *
     * @return true if success message is visible
     */
    public boolean hasSuccessMessage() {
        logger.debug("Checking for success message");
        try {
            return isDisplayed(SUCCESS_MESSAGE);
        } catch (Exception e) {
            logger.debug("No success message found");
            return false;
        }
    }

    /**
     * Returns the visible text of the login error message element.
     * Distinguishes three outcomes and logs each clearly for debugging:
     *   - Element absent: logs WARN with locator and page URL, returns ""
     *   - Element present, text empty: logs WARN with page URL, returns ""
     *   - Element present with text: logs INFO and returns the text
     */
    public String getErrorMessageText() {
        if (getElementCount(ERROR_MESSAGE) == 0) {
            logger.warn("[LoginPage] Error message element not present | locator: {} | url: {}",
                    ERROR_MESSAGE, driver.getCurrentUrl());
            return "";
        }
        try {
            String text = driver.findElement(ERROR_MESSAGE).getText().trim();
            if (text.isEmpty()) {
                logger.warn("[LoginPage] Error message element present but contains no text | locator: {} | url: {}",
                        ERROR_MESSAGE, driver.getCurrentUrl());
            } else {
                logger.info("[LoginPage] Error message: '{}' | url: {}", text, driver.getCurrentUrl());
            }
            return text;
        } catch (Exception e) {
            logger.error("[LoginPage] Failed to read error message text | locator: {} | url: {} | cause: {}",
                    ERROR_MESSAGE, driver.getCurrentUrl(), e.getMessage());
            return "";
        }
    }
}