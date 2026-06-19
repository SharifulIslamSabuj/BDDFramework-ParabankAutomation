package com.parabank.parasoft.utils;

import com.parabank.parasoft.config.ConfigManager;
import com.parabank.parasoft.exceptions.WaitException;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * WaitUtils - Explicit wait utilities backed by framework configuration.
 *
 * <p>All timeouts are read from {@link ConfigManager} on every call so that
 * configuration changes (or a {@code ConfigManager.resetInstance()} in tests)
 * are reflected immediately without restarting the JVM.
 *
 * @author Automation Team
 * @version 1.0
 */
public class WaitUtils {
    private static final Logger logger = LoggerFactory.getLogger(WaitUtils.class);
    private static final ConfigManager configManager = ConfigManager.getInstance();

    private WaitUtils() {
        // Prevent instantiation
    }

    /**
     * Waits for an element to be visible on the page.
     * Timeout is controlled by the {@code waitTime} property (default: {@value com.parabank.parasoft.constants.TimeoutConstants#EXPLICIT_WAIT_TIME}s).
     */
    public static WebElement waitForElementToBeVisible(WebDriver driver, By locator) {
        int timeout = configManager.getWaitTime();
        logger.debug("Waiting for element to be visible: {}", locator);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            logger.debug("Element is visible: {}", locator);
            return element;
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for element to be visible: {}", locator);
            throw new WaitException("Element did not become visible within " + timeout + " seconds: " + locator, e);
        }
    }

    /**
     * Waits for an element to be clickable.
     * Timeout is controlled by the {@code waitTime} property (default: {@value com.parabank.parasoft.constants.TimeoutConstants#EXPLICIT_WAIT_TIME}s).
     */
    public static WebElement waitForElementToBeClickable(WebDriver driver, By locator) {
        int timeout = configManager.getWaitTime();
        logger.debug("Waiting for element to be clickable: {}", locator);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            logger.debug("Element is clickable: {}", locator);
            return element;
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for element to be clickable: {}", locator);
            throw new WaitException("Element did not become clickable within " + timeout + " seconds: " + locator, e);
        }
    }

    /**
     * Waits for an element to be present in the DOM.
     * Timeout is controlled by the {@code waitTime} property (default: {@value com.parabank.parasoft.constants.TimeoutConstants#EXPLICIT_WAIT_TIME}s).
     */
    public static WebElement waitForElementToBePresent(WebDriver driver, By locator) {
        int timeout = configManager.getWaitTime();
        logger.debug("Waiting for element to be present: {}", locator);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            logger.debug("Element is present: {}", locator);
            return element;
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for element to be present: {}", locator);
            throw new WaitException("Element was not present within " + timeout + " seconds: " + locator, e);
        }
    }

    /**
     * Waits for page to load (document ready state)
     */
    public static void waitForPageLoad(WebDriver driver) {
        logger.debug("Waiting for page to load");
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(configManager.getPageLoadTimeout()));
            wait.until(webDriver -> {
                JavascriptExecutor js = (JavascriptExecutor) webDriver;
                return js.executeScript("return document.readyState").equals("complete");
            });
            logger.debug("Page loaded successfully");
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for page load");
            throw new WaitException("Page did not load within " + configManager.getPageLoadTimeout() + " seconds", e);
        }
    }
}

