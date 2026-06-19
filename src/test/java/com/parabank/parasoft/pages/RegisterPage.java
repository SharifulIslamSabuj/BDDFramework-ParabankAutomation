package com.parabank.parasoft.pages;

import com.parabank.parasoft.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * RegisterPage - Page object for user registration
 * Uses fluent API and private final locators
 *
 * @author Automation Team
 */
public class RegisterPage extends BasePage {

    // ===== Private Locators =====
    private static final By FIRST_NAME = By.id("customer.firstName");
    private static final By LAST_NAME = By.id("customer.lastName");
    private static final By ADDRESS = By.id("customer.address.street");
    private static final By CITY = By.id("customer.address.city");
    private static final By STATE = By.id("customer.address.state");
    private static final By ZIP_CODE = By.id("customer.address.zipCode");
    private static final By PHONE = By.id("customer.phoneNumber");
    private static final By SSN = By.id("customer.ssn");
    private static final By USERNAME = By.id("customer.username");
    private static final By PASSWORD = By.id("customer.password");
    private static final By CONFIRM_PASSWORD = By.id("repeatedPassword");
    private static final By REGISTER_BUTTON = By.cssSelector("input[value='Register']");
    private static final By ERROR_MESSAGES = By.cssSelector("span[class='error']");

    /**
     * Constructor
     *
     * @param driver the WebDriver instance
     */
    public RegisterPage(WebDriver driver) {
        super(driver);
        logger.info("RegisterPage initialized");
    }

    /**
     * Fills first name
     *
     * @param firstName the first name
     * @return this for fluent chaining
     */
    public RegisterPage fillFirstName(String firstName) {
        logger.info("Filling first name: {}", firstName);
        sendKeys(FIRST_NAME, firstName);
        return this;
    }

    /**
     * Fills last name
     *
     * @param lastName the last name
     * @return this for fluent chaining
     */
    public RegisterPage fillLastName(String lastName) {
        logger.info("Filling last name: {}", lastName);
        sendKeys(LAST_NAME, lastName);
        return this;
    }

    /**
     * Fills address
     *
     * @param address the address
     * @return this for fluent chaining
     */
    public RegisterPage fillAddress(String address) {
        logger.info("Filling address");
        sendKeys(ADDRESS, address);
        return this;
    }

    /**
     * Fills city
     *
     * @param city the city
     * @return this for fluent chaining
     */
    public RegisterPage fillCity(String city) {
        logger.info("Filling city: {}", city);
        sendKeys(CITY, city);
        return this;
    }

    /**
     * Fills state
     *
     * @param state the state
     * @return this for fluent chaining
     */
    public RegisterPage fillState(String state) {
        logger.info("Filling state: {}", state);
        sendKeys(STATE, state);
        return this;
    }

    /**
     * Fills zip code
     *
     * @param zipCode the zip code
     * @return this for fluent chaining
     */
    public RegisterPage fillZipCode(String zipCode) {
        logger.info("Filling zip code");
        sendKeys(ZIP_CODE, zipCode);
        return this;
    }

    /**
     * Fills phone number
     *
     * @param phone the phone number
     * @return this for fluent chaining
     */
    public RegisterPage fillPhone(String phone) {
        logger.info("Filling phone");
        sendKeys(PHONE, phone);
        return this;
    }

    /**
     * Fills SSN
     *
     * @param ssn the SSN
     * @return this for fluent chaining
     */
    public RegisterPage fillSSN(String ssn) {
        logger.info("Filling SSN");
        sendKeys(SSN, ssn);
        return this;
    }

    /**
     * Fills username
     *
     * @param username the username
     * @return this for fluent chaining
     */
    public RegisterPage fillUsername(String username) {
        logger.info("Filling username: {}", username);
        sendKeys(USERNAME, username);
        return this;
    }

    /**
     * Fills password
     *
     * @param password the password
     * @return this for fluent chaining
     */
    public RegisterPage fillPassword(String password) {
        logger.info("Filling password");
        sendKeys(PASSWORD, password);
        return this;
    }

    /**
     * Fills confirm password
     *
     * @param confirmPassword the confirm password
     * @return this for fluent chaining
     */
    public RegisterPage fillConfirmPassword(String confirmPassword) {
        logger.info("Filling confirm password");
        sendKeys(CONFIRM_PASSWORD, confirmPassword);
        return this;
    }

    /**
     * Clicks register button and navigates to OverviewPage on success
     * Waits for logout link to be visible before returning page object
     *
     * @return OverviewPage instance
     */
    public OverviewPage clickRegisterButtonWithSuccess() {
        logger.info("Clicking register button - expecting success");
        try {
            click(REGISTER_BUTTON);
            WaitUtils.waitForPageLoad(driver);
            logger.info("Page load completed after register button click");
        } catch (Exception e) {
            logger.warn("Page load wait timed out or failed: {}", e.getMessage());
        }

        // Navigate to OverviewPage and verify it loaded (WaitException propagates on failure)
        try {
            OverviewPage overviewPage = createPage(OverviewPage.class).waitUntilLoaded();
            logger.info("✓ OverviewPage loaded successfully after registration");
            return overviewPage;
        } catch (Exception e) {
            logger.error("OverviewPage did not load after registration — logout link not found: {}", e.getMessage());
            throw new RuntimeException("Registration may have failed - logout link not found on OverviewPage", e);
        }
    }

    /**
     * Clicks register button, expecting failure (remains on RegisterPage)
     *
     * @return this RegisterPage
     */
    public RegisterPage clickRegisterButtonWithFail() {
        logger.info("Clicking register button - expecting failure");
        click(REGISTER_BUTTON);
        WaitUtils.waitForPageLoad(driver);
        return this;
    }

    /**
     * Checks if error message is visible above certain number
     *
     * @param errorNumber the expected minimum number of errors
     * @return true if error count meets threshold
     */
    public boolean hasErrorMessage(int errorNumber) {
        logger.debug("Checking for at least {} error messages", errorNumber);
        return getElementCount(ERROR_MESSAGES) >= errorNumber;
    }

    /**
     * Checks if any error message is visible
     *
     * @return true if error messages exist
     */
    public boolean hasErrorMessage() {
        logger.debug("Checking for error messages");
        return getElementCount(ERROR_MESSAGES) > 0;
    }

    /**
     * Gets error message text
     *
     * @return error message text
     */
    public String getErrorMessageText() {
        logger.debug("Getting error message text");
        try {
            return getText(ERROR_MESSAGES);
        } catch (Exception e) {
            logger.error("Failed to get error message text", e);
            return "";
        }
    }
}
