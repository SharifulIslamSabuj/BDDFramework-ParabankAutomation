package com.parabank.parasoft.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * OverviewPage - Page object for ParaBank overview/accounts page
 * Displayed after successful login
 *
 * @author Automation Team
 */
public class OverviewPage extends BasePage {

    // ===== Private Locators =====
    private static final By LOGOUT_LINK = By.cssSelector("a[href='logout.htm']");
    private static final By OPEN_ACCOUNT_LINK = By.cssSelector("a[href='openaccount.htm']");
    private static final By REQUEST_LOAN_LINK = By.cssSelector("a[href='requestloan.htm']");
    private static final By UPDATE_PROFILE_LINK = By.cssSelector("a[href='updateprofile.htm']");

    /**
     * Constructor
     *
     * @param driver the WebDriver instance
     */
    public OverviewPage(WebDriver driver) {
        super(driver);
        logger.info("OverviewPage initialized");
    }

    /**
     * Checks if logout link is visible
     *
     * @return true if logout link is displayed
     */
    public boolean hasLogOutLink() {
        logger.debug("Checking for logout link");
        try {
            waitForElementVisible(LOGOUT_LINK);
            return isDisplayed(LOGOUT_LINK);
        } catch (Exception e) {
            logger.debug("Logout link not visible");
            return false;
        }
    }

    /**
     * Clicks on Open New Account link
     *
     * @return OpenNewAccountPage instance
     */
    public OpenNewAccountPage clickOpenNewAccountLink() {
        logger.info("Clicking open new account link");
        click(OPEN_ACCOUNT_LINK);
        return createPage(OpenNewAccountPage.class);
    }

    /**
     * Clicks on Request Loan link
     *
     * @return RequestLoanPage instance
     */
    public RequestLoanPage clickRequestLoanLink() {
        logger.info("Clicking request loan link");
        click(REQUEST_LOAN_LINK);
        return createPage(RequestLoanPage.class);
    }

    /**
     * Clicks on Update Profile link
     *
     * @return UpdateProfilePage instance
     */
    public UpdateProfilePage clickUpdateProfileLink() {
        logger.info("Clicking update profile link");
        click(UPDATE_PROFILE_LINK);
        return createPage(UpdateProfilePage.class);
    }
}
