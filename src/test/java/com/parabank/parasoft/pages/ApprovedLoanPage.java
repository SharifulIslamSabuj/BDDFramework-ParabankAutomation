package com.parabank.parasoft.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * ApprovedLoanPage - Page object displayed after successful loan approval
 *
 * @author Automation Team
 */
public class ApprovedLoanPage extends BasePage {

    // ===== Private Locators =====
    private static final By LOAN_ID = By.id("newAccountId");
    private static final By SUCCESS_MESSAGE = By.xpath("//div[contains(text(), 'Congratulations')]");

    /**
     * Constructor
     *
     * @param driver the WebDriver instance
     */
    public ApprovedLoanPage(WebDriver driver) {
        super(driver);
        logger.info("ApprovedLoanPage initialized");
    }

    /**
     * Checks if loan approval ID is displayed
     *
     * @return true if loan ID is visible
     */
    public boolean isApprovedLoanId() {
        logger.debug("Checking if approved loan ID is displayed");
        try {
            return isPresent(LOAN_ID);
        } catch (Exception e) {
            logger.debug("Loan ID not displayed");
            return false;
        }
    }

    /**
     * Gets the approved loan ID
     *
     * @return the loan ID text
     */
    public String getLoanId() {
        logger.debug("Getting loan ID");
        try {
            return getText(LOAN_ID);
        } catch (Exception e) {
            logger.error("Failed to get loan ID", e);
            return "";
        }
    }

    /**
     * Checks if success/congratulations message is displayed
     *
     * @return true if message visible
     */
    public boolean hasSuccessMessage() {
        logger.debug("Checking for success message");
        try {
            return isDisplayed(SUCCESS_MESSAGE);
        } catch (Exception e) {
            logger.debug("Success message not found");
            return false;
        }
    }
}
