package com.parabank.parasoft.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * OpenNewAccountPage - Page object for opening new account
 *
 * @author Automation Team
 */
public class OpenNewAccountPage extends BasePage {

    // ===== Private Locators =====
    private static final By ACCOUNT_TYPE_DROPDOWN = By.id("type");
    private static final By FROM_ACCOUNT_DROPDOWN = By.id("fromAccountId");
    private static final By OPEN_ACCOUNT_BUTTON   = By.cssSelector("input[value='Open New Account']");

    /**
     * Constructor
     *
     * @param driver the WebDriver instance
     */
    public OpenNewAccountPage(WebDriver driver) {
        super(driver);
        logger.info("OpenNewAccountPage initialized");
    }

    /**
     * Selects account type (CHECKING or SAVINGS)
     *
     * @param accountType the account type name
     * @return this for fluent chaining
     */
    public OpenNewAccountPage selectAccountType(String accountType) {
        logger.info("Selecting account type: {}", accountType);
        WebElement element = findElement(ACCOUNT_TYPE_DROPDOWN);
        Select select = new Select(element);
        select.selectByVisibleText(accountType);
        return this;
    }

    /**
     * Selects source account by number
     *
     * @param accountNumber the account number
     * @return this for fluent chaining
     */
    public OpenNewAccountPage selectFromAccount(String accountNumber) {
        logger.info("Selecting from account: {}", accountNumber);
        WebElement element = findElement(FROM_ACCOUNT_DROPDOWN);
        Select select = new Select(element);
        select.selectByVisibleText(accountNumber);
        return this;
    }

    /**
     * Selects first available account
     *
     * @return this for fluent chaining
     */
    public OpenNewAccountPage selectFirstAvailableAccount() {
        logger.info("Selecting first available account");
        WebElement element = findElement(FROM_ACCOUNT_DROPDOWN);
        Select select = new Select(element);
        List<WebElement> options = select.getOptions();
        if (!options.isEmpty()) {
            select.selectByIndex(0);
        }
        return this;
    }

    /**
     * Clicks open account button
     *
     * @return OpenedAccountPage instance
     */
    public OpenedAccountPage clickOpenNewAccountButton() {
        logger.info("Clicking open new account button");
        click(OPEN_ACCOUNT_BUTTON);
        return createPage(OpenedAccountPage.class);
    }

}