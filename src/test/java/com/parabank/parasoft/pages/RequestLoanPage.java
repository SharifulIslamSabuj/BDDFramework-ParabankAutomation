package com.parabank.parasoft.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * RequestLoanPage - Page object for loan request
 *
 * @author Automation Team
 */
public class RequestLoanPage extends BasePage {

    // ===== Private Locators =====
    private static final By LOAN_AMOUNT = By.xpath("//input[@id='amount']");
    private static final By DOWN_PAYMENT = By.xpath("//input[@id='downPayment']");
    private static final By FROM_ACCOUNT_DROPDOWN = By.xpath("//select[@id='fromAccountId']");
    private static final By APPLY_NOW_BUTTON = By.xpath("//input[@value='Apply Now']");

    /**
     * Constructor
     *
     * @param driver the WebDriver instance
     */
    public RequestLoanPage(WebDriver driver) {
        super(driver);
        logger.info("RequestLoanPage initialized");
    }

    /**
     * Fills loan amount
     *
     * @param loanAmount the loan amount
     * @return this for fluent chaining
     */
    public RequestLoanPage fillLoanAmount(String loanAmount) {
        logger.info("Filling loan amount: {}", loanAmount);
        sendKeys(LOAN_AMOUNT, loanAmount);
        return this;
    }

    /**
     * Fills down payment amount
     *
     * @param downPaymentAmount the down payment amount
     * @return this for fluent chaining
     */
    public RequestLoanPage fillDownPayment(String downPaymentAmount) {
        logger.info("Filling down payment: {}", downPaymentAmount);
        sendKeys(DOWN_PAYMENT, downPaymentAmount);
        return this;
    }

    /**
     * Selects account by index
     *
     * @param index the account index
     * @return this for fluent chaining
     */
    public RequestLoanPage selectAccount(int index) {
        logger.info("Selecting account at index: {}", index);
        WebElement element = findElement(FROM_ACCOUNT_DROPDOWN);
        Select select = new Select(element);
        select.selectByIndex(index);
        return this;
    }

    /**
     * Selects account by account number
     *
     * @param accountNumber the account number
     * @return this for fluent chaining
     */
    public RequestLoanPage selectAccount(String accountNumber) {
        logger.info("Selecting account: {}", accountNumber);
        WebElement element = findElement(FROM_ACCOUNT_DROPDOWN);
        Select select = new Select(element);
        select.selectByVisibleText(accountNumber);
        return this;
    }

    /**
     * Clicks apply now button
     *
     * @return ApprovedLoanPage instance
     */
    public ApprovedLoanPage clickApplyNowButton() {
        logger.info("Clicking apply now button");
        click(APPLY_NOW_BUTTON);
        return createPage(ApprovedLoanPage.class);
    }

}