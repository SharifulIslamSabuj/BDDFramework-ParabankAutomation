package com.parabank.parasoft.stepdefinitions;

import com.parabank.parasoft.driver.DriverManager;
import com.parabank.parasoft.pages.LoginPage;
import com.parabank.parasoft.pages.OverviewPage;
import io.cucumber.java.en.*;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * LoginSteps - Step definitions for login feature
 * Maps Gherkin steps to page interactions
 * Supports positive and negative login scenarios
 *
 * @author Automation Team
 */
public class LoginSteps {
    private static final Logger logger = LoggerFactory.getLogger(LoginSteps.class);

    private LoginPage loginPage;
    private OverviewPage overviewPage;

    /**
     * Given: User is on the Login Page
     * Initializes the LoginPage object against the current browser window.
     * WebDriver is already on the base URL from DriverFactory initialization.
     * Default test user setup is handled once by Hooks.ensureDefaultTestUserExists().
     */
    @Given("User is on Login Page")
    public void user_on_login_page() {
        logger.info("Opening Login Page");
        WebDriver driver = DriverManager.getDriver();
        loginPage = new LoginPage(driver);
        Assert.assertNotNull(loginPage, "Login page should be initialized");
    }

    /**
     * Given: User is on the Login Page (alternative step name)
     */
    @Given("User is on the Login Page")
    public void user_is_on_login_page() {
        user_on_login_page();
    }

    /**
     * When: User enters valid credentials
     */
    @When("User enters username {string} and password {string}")
    public void user_enters_username_and_password(String username, String password) {
        logger.info("Entering credentials - Username: {}, Password: ***", username);
        loginPage.fillUsername(username)
                .fillPassword(password);
    }

    /**
     * When: User leaves username blank and enters password
     */
    @When("User leaves username blank and enters password {string}")
    public void user_leaves_username_blank_enters_password(String password) {
        logger.info("Leaving username blank, entering password");
        loginPage.fillUsername("")
                .fillPassword(password);
    }

    /**
     * When: User enters username and leaves password blank
     */
    @When("User enters username {string} and leaves password blank")
    public void user_enters_username_leaves_password_blank(String username) {
        logger.info("Entering username: {}, leaving password blank", username);
        loginPage.fillUsername(username)
                .fillPassword("");
    }

    /**
     * When: User leaves both username and password blank
     */
    @When("User leaves username and password blank")
    public void user_leaves_username_and_password_blank() {
        logger.info("Leaving both username and password blank");
        loginPage.fillUsername("")
                .fillPassword("");
    }

    /**
     * When: User clicks login button
     */
    @When("User clicks login button")
    public void user_clicks_login_button() {
        logger.info("Clicking login button");
        overviewPage = loginPage.clickLoginButton();
        if (overviewPage != null) {
            logger.info("Login successful - navigated to OverviewPage");
        } else {
            logger.warn("Login failed - remained on LoginPage");
        }
    }

    /**
     * Then: User should see logout link (positive scenario)
     */
    @Then("User should see logout link")
    public void user_should_see_logout_link() {
        logger.info("Verifying logout link is displayed");

        // Ensure overviewPage is not null and properly loaded
        Assert.assertNotNull(overviewPage, "User should be on OverviewPage after successful login");

        // Wait for logout link to be visible before assertion
        boolean logoutLinkFound = overviewPage.hasLogOutLink();
        Assert.assertTrue(logoutLinkFound, "Logout link should be visible on OverviewPage");

        logger.info("✓ Logout link verified successfully");
    }

    /**
     * Then: User should see error message
     */
    @Then("User should see error message {string}")
    public void user_should_see_error_message(String expectedMessage) {
        logger.info("Verifying error message: {}", expectedMessage);
        Assert.assertNotNull(loginPage, "User should remain on LoginPage");

        // Use text-based validation instead of boolean check
        String actualErrorMessage = loginPage.getErrorMessageText();
        logger.info("Expected: {}", expectedMessage);
        logger.info("Actual: {}", actualErrorMessage);
        Assert.assertTrue(
                actualErrorMessage.contains(expectedMessage),
                "Expected error message not found. Expected to contain: '" + expectedMessage + "' but got: '" + actualErrorMessage + "'"
        );
        logger.info("✓ Error message verified: {}", expectedMessage);
    }

    /**
     * Then: User should see any error message (generic)
     */
    @Then("User should see error")
    public void user_should_see_error() {
        logger.info("Verifying any error message is displayed");
        Assert.assertTrue(loginPage.hasErrorMessage(), "Error message should be displayed");
        String errorText = loginPage.getErrorMessageText();
        logger.info("✓ Error message displayed: {}", errorText);
    }

    // ── Business-language step definitions (delegate to existing methods) ──

    @Given("a registered customer is on the sign-in page")
    public void a_registered_customer_is_on_the_sign_in_page() {
        user_on_login_page();
    }

    @When("the customer signs in with username {string} and password {string}")
    public void the_customer_signs_in_with_username_and_password(String username, String password) {
        user_enters_username_and_password(username, password);
    }

    @When("the customer submits their login credentials")
    public void the_customer_submits_their_login_credentials() {
        user_clicks_login_button();
    }

    @Then("the customer is successfully signed in")
    public void the_customer_is_successfully_signed_in() {
        user_should_see_logout_link();
    }

    @Then("the customer is shown the sign-in error {string}")
    public void the_customer_is_shown_the_sign_in_error(String expectedMessage) {
        user_should_see_error_message(expectedMessage);
    }
}