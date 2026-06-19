package com.parabank.parasoft.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * OpenedAccountPage - Page object displayed after successful account creation
 *
 * @author Automation Team
 */
public class OpenedAccountPage extends BasePage {

    // ===== Private Locators =====
    private static final By NEW_ACCOUNT_ID = By.id("newAccountId");

    /**
     * Constructor
     *
     * @param driver the WebDriver instance
     */
    public OpenedAccountPage(WebDriver driver) {
        super(driver);
        logger.info("OpenedAccountPage initialized");
    }

    /**
     * Checks if new account ID is displayed
     *
     * @return true if new account ID is visible
     */
    public boolean hasOpenedAccountId() {
        logger.debug("Checking if opened account ID is displayed");
        try {
            return isPresent(NEW_ACCOUNT_ID);
        } catch (Exception e) {
            logger.debug("Account ID not displayed");
            return false;
        }
    }

    /**
     * Gets the newly created account ID
     *
     * @return the account ID text
     */
    public String getAccountId() {
        logger.debug("Getting account ID");
        try {
            return getText(NEW_ACCOUNT_ID);
        } catch (Exception e) {
            logger.error("Failed to get account ID", e);
            return "";
        }
    }
}