package com.parabank.parasoft.pages;

import com.parabank.parasoft.config.ConfigManager;
import com.parabank.parasoft.utils.ElementActions;
import com.parabank.parasoft.utils.ScreenshotUtils;
import com.parabank.parasoft.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BasePage - Base page class for all page objects
 * Simplified to provide only essential functionality
 *
 * @author Automation Team
 * @version 1.0
 */
public class BasePage {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final WebDriver driver;
    protected final ConfigManager configManager = ConfigManager.getInstance();

    public BasePage(WebDriver driver) {
        this.driver = driver;
    }

    // ===== ELEMENT ACTIONS =====

    protected WebElement findElement(By locator) {
        return ElementActions.findElement(driver, locator);
    }

    protected void click(By locator) {
        ElementActions.click(driver, locator);
    }

    protected void sendKeys(By locator, String text) {
        ElementActions.sendKeys(driver, locator, text);
    }

    protected String getText(By locator) {
        return ElementActions.getText(driver, locator);
    }

    protected boolean isDisplayed(By locator) {
        return ElementActions.isDisplayed(driver, locator);
    }

    protected boolean isPresent(By locator) {
        return ElementActions.isElementPresent(driver, locator);
    }

    protected int getElementCount(By locator) {
        return ElementActions.getElementCount(driver, locator);
    }

    // ===== WAIT METHODS =====

    protected WebElement waitForElementVisible(By locator) {
        return WaitUtils.waitForElementToBeVisible(driver, locator);
    }

    protected WebElement waitForElementClickable(By locator) {
        return WaitUtils.waitForElementToBeClickable(driver, locator);
    }

    protected void waitForPageLoad() {
        WaitUtils.waitForPageLoad(driver);
    }

    // ===== PAGE NAVIGATION =====

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getPageURL() {
        return driver.getCurrentUrl();
    }

    public <T extends BasePage> T createPage(Class<T> pageClass) {
        try {
            return pageClass.getDeclaredConstructor(WebDriver.class).newInstance(driver);
        } catch (Exception e) {
            logger.error("Failed to create page object: {}", pageClass.getName(), e);
            throw new RuntimeException("Failed to create page object: " + pageClass.getName(), e);
        }
    }

    // ===== SCREENSHOT =====

    protected String takeScreenshot(String testName) {
        return ScreenshotUtils.captureScreenshot(driver, testName);
    }
}
