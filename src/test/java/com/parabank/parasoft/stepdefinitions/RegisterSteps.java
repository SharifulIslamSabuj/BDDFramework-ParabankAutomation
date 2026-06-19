package com.parabank.parasoft.stepdefinitions;

import com.parabank.parasoft.driver.DriverManager;
import com.parabank.parasoft.pages.LoginPage;
import com.parabank.parasoft.pages.OverviewPage;
import com.parabank.parasoft.pages.RegisterPage;
import com.parabank.parasoft.constants.PathConstants;
import com.parabank.parasoft.utils.ExcelDataProvider;
import com.thedeanda.lorem.LoremIpsum;
import io.cucumber.java.en.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

public class RegisterSteps {
    private static final Logger logger = LoggerFactory.getLogger(RegisterSteps.class);

    private LoginPage loginPage;
    private RegisterPage registerPage;
    private OverviewPage overviewPage;

    @Given("User navigates to Register page")
    public void user_navigates_to_register_page() {
        logger.info("Navigating to Register page");
        loginPage = new LoginPage(DriverManager.getDriver());
        registerPage = loginPage.clickRegisterLink();
        logger.info("RegisterPage loaded successfully");
    }

    @When("User fills registration form with hard data")
    public void user_fills_registration_form_with_hard_data() {
        try {
            logger.info("Filling registration form with generated data");
            String username = LoremIpsum.getInstance().getName().replaceAll("[^a-zA-Z0-9]", "") + System.currentTimeMillis();
            String password = username;
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
            logger.info("✓ Registration form filled successfully");
        } catch (Exception e) {
            logger.error("Failed to fill registration form", e);
            throw new RuntimeException("Failed to fill registration form", e);
        }
    }

    @When("User fills registration form with ipsum data")
    public void user_fills_registration_form_with_ipsum_data() {
        try {
            logger.info("Filling registration form with Lorem Ipsum data");
            String username = LoremIpsum.getInstance().getName().replaceAll(" ", "") + System.currentTimeMillis();
            registerPage
                    .fillFirstName(LoremIpsum.getInstance().getFirstName())
                    .fillLastName(LoremIpsum.getInstance().getLastName())
                    .fillAddress(LoremIpsum.getInstance().getTitle(5))
                    .fillCity(LoremIpsum.getInstance().getCity())
                    .fillState(LoremIpsum.getInstance().getStateAbbr())
                    .fillZipCode(LoremIpsum.getInstance().getZipCode())
                    .fillPhone(LoremIpsum.getInstance().getPhone())
                    .fillSSN(LoremIpsum.getInstance().getPhone())
                    .fillUsername(username)
                    .fillPassword(username)
                    .fillConfirmPassword(username);
            logger.info("✓ Registration form filled successfully with Lorem Ipsum data");
        } catch (Exception e) {
            logger.error("Failed to fill registration form with ipsum data", e);
            throw new RuntimeException("Failed to fill registration form with ipsum data", e);
        }
    }

    @When("User fills registration form with excel data")
    public void user_fills_registration_form_with_excel_data() {
        try {
            logger.info("Filling registration form with Excel data");
            // skipHeader=true: row 0 of the sheet is the column-name header
            Object[][] data = ExcelDataProvider.getTestData(PathConstants.TEST_DATA_SHEET, true);
            if (data == null || data.length == 0) {
                throw new RuntimeException("No test data rows found in Excel file (after skipping header)");
            }
            // Use the first data row; columns beyond index 2 fall back to generated values
            Object[] row = data[0];
            String firstName = cellValue(row, 0, LoremIpsum.getInstance().getFirstName());
            String lastName  = cellValue(row, 1, LoremIpsum.getInstance().getLastName());
            String address   = cellValue(row, 2, LoremIpsum.getInstance().getTitle(3));
            String username  = LoremIpsum.getInstance().getName().replaceAll("[^a-zA-Z0-9]", "") + System.currentTimeMillis();
            String password  = username;

            logger.info("Excel data row 0: firstName='{}', lastName='{}', address='{}'",
                    firstName, lastName, address);

            registerPage
                    .fillFirstName(firstName)
                    .fillLastName(lastName)
                    .fillAddress(address)
                    .fillCity(LoremIpsum.getInstance().getCity())
                    .fillState(LoremIpsum.getInstance().getStateAbbr())
                    .fillZipCode(LoremIpsum.getInstance().getZipCode())
                    .fillPhone(LoremIpsum.getInstance().getPhone())
                    .fillSSN(LoremIpsum.getInstance().getPhone())
                    .fillUsername(username)
                    .fillPassword(password)
                    .fillConfirmPassword(password);
            logger.info("✓ Registration form filled with Excel data ({} total rows available)", data.length);
        } catch (Exception e) {
            logger.error("Failed to fill registration form with Excel data", e);
            throw new RuntimeException("Failed to fill registration form with Excel data", e);
        }
    }

    /** Returns the cell value at {@code index} as a non-blank String, or {@code fallback}. */
    private String cellValue(Object[] row, int index, String fallback) {
        if (row == null || index >= row.length) return fallback;
        String val = row[index] != null ? row[index].toString().trim() : "";
        return val.isEmpty() ? fallback : val;
    }

    @When("User fills registration form without username")
    public void user_fills_registration_form_without_username() {
        logger.info("Filling registration form without username (negative test)");
        String password = LoremIpsum.getInstance().getWords(2).replaceAll("[^a-zA-Z0-9]", "");
        registerPage
                .fillFirstName(LoremIpsum.getInstance().getFirstName())
                .fillLastName(LoremIpsum.getInstance().getLastName())
                .fillAddress(LoremIpsum.getInstance().getTitle(3))
                .fillCity(LoremIpsum.getInstance().getCity())
                .fillState(LoremIpsum.getInstance().getStateAbbr())
                .fillZipCode(LoremIpsum.getInstance().getZipCode())
                .fillPhone(LoremIpsum.getInstance().getPhone())
                .fillPassword(password)
                .fillConfirmPassword(password);
        logger.info("✓ Registration form filled without username");
    }

    @When("User submits registration expecting failure")
    public void user_submits_registration_expecting_failure() {
        logger.info("Submitting registration form expecting failure");
        registerPage.clickRegisterButtonWithFail();
        logger.info("✓ Registration form submitted (failed as expected)");
    }

    @When("User submits registration")
    public void user_submits_registration() {
        logger.info("Submitting registration form");
        overviewPage = registerPage.clickRegisterButtonWithSuccess();
        logger.info("✓ Registration submitted successfully");
    }

    @Then("User account should be created")
    public void user_account_should_be_created() {
        logger.info("Verifying user account was created");
        Assert.assertNotNull(overviewPage, "OverviewPage should not be null after successful registration");

        // Robust validation: check that logout link is displayed on OverviewPage
        boolean hasLogout = overviewPage.hasLogOutLink();
        Assert.assertTrue(hasLogout, "Registration failed - logout link not found on OverviewPage. Registration may have failed due to duplicate username or validation error.");

        logger.info("✓ User account created successfully - logout link verified on OverviewPage");
    }

    @Then("User should see error message")
    public void user_should_see_error_message() {
        logger.info("Verifying error message is displayed");
        
        // Robust validation: check for error messages on register page
        boolean hasError = registerPage.hasErrorMessage();
        Assert.assertTrue(hasError, "At least one error message should be displayed on RegisterPage");
        
        // Log the actual error text for debugging
        String errorText = registerPage.getErrorMessageText();
        logger.info("Error message text: {}", errorText);
        Assert.assertFalse(errorText.isEmpty(), "Error message should not be empty");
        
        logger.info("✓ Error message verified: {}", errorText);
    }
}