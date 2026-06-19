package com.parabank.parasoft.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * UpdateProfilePage - Page object for updating user profile
 *
 * @author Automation Team
 */
public class UpdateProfilePage extends BasePage {

    // ===== Private Locators =====
    private static final By FIRST_NAME = By.id("customer.firstName");
    private static final By LAST_NAME = By.id("customer.lastName");
    private static final By ADDRESS = By.id("customer.address.street");
    private static final By CITY = By.id("customer.address.city");
    private static final By STATE = By.id("customer.address.state");
    private static final By ZIP_CODE = By.id("customer.address.zipCode");
    private static final By PHONE = By.id("customer.phoneNumber");
    private static final By UPDATE_PROFILE_BUTTON = By.cssSelector("input[value='Update Profile']");
    private static final By SUCCESS_MESSAGE = By.cssSelector("div.success");

    /**
     * Constructor
     *
     * @param driver the WebDriver instance
     */
    public UpdateProfilePage(WebDriver driver) {
        super(driver);
        logger.info("UpdateProfilePage initialized");
    }

    /**
     * Fills first name
     *
     * @param firstName the first name
     * @return this for fluent chaining
     */
    public UpdateProfilePage fillFirstName(String firstName) {
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
    public UpdateProfilePage fillLastName(String lastName) {
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
    public UpdateProfilePage fillAddress(String address) {
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
    public UpdateProfilePage fillCity(String city) {
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
    public UpdateProfilePage fillState(String state) {
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
    public UpdateProfilePage fillZipCode(String zipCode) {
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
    public UpdateProfilePage fillPhone(String phone) {
        logger.info("Filling phone");
        sendKeys(PHONE, phone);
        return this;
    }

    /**
     * Clicks update profile button
     *
     * @return this for fluent chaining
     */
    public UpdateProfilePage clickUpdateProfileButton() {
        logger.info("Clicking update profile button");
        click(UPDATE_PROFILE_BUTTON);
        return this;
    }

    /**
     * Checks if success message is displayed
     *
     * @return true if success message visible
     */
    public boolean isSuccessMessageDisplayed() {
        logger.debug("Checking if success message is displayed");
        try {
            return isDisplayed(SUCCESS_MESSAGE);
        } catch (Exception e) {
            logger.debug("Success message not displayed");
            return false;
        }
    }
}
