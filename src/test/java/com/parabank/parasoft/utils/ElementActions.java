package com.parabank.parasoft.utils;

import com.parabank.parasoft.exceptions.ElementInteractionException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * ElementActions - Essential element interaction utilities
 * Simplified to only provide core functionality without unnecessary layers
 *
 * @author Automation Team
 * @version 1.0
 */
public class ElementActions {
    private static final Logger logger = LoggerFactory.getLogger(ElementActions.class);

    private ElementActions() {
        // Prevent instantiation
    }

    /**
     * Finds an element with explicit wait
     */
    public static WebElement findElement(WebDriver driver, By locator) {
        logger.debug("Finding element: {}", locator);
        try {
            WebElement element = WaitUtils.waitForElementToBePresent(driver, locator);
            logger.debug("Element found: {}", locator);
            return element;
        } catch (Exception e) {
            logger.error("Expected element not found: {}", locator);
            throw new ElementInteractionException("Element not found: " + locator, e);
        }
    }

    /**
     * Clicks on an element with clickability wait
     */
    public static void click(WebDriver driver, By locator) {
        logger.info("Clicking element: {}", locator);
        try {
            WebElement element = WaitUtils.waitForElementToBeClickable(driver, locator);
            element.click();
            logger.info("Element clicked successfully: {}", locator);
        } catch (Exception e) {
            logger.error("Failed to click element: {}", locator);
            throw new ElementInteractionException("Click action failed: " + locator, e);
        }
    }

    /**
     * Sends text to an element
     */
    public static void sendKeys(WebDriver driver, By locator, String text) {
        logger.info("Entering text into element: {}", locator);
        try {
            WebElement element = WaitUtils.waitForElementToBeVisible(driver, locator);
            element.clear();
            element.sendKeys(text);
            logger.info("Text entered successfully: {}", locator);
        } catch (Exception e) {
            logger.error("Failed to enter text into element: {}", locator);
            throw new ElementInteractionException("SendKeys action failed: " + locator, e);
        }
    }

    /**
     * Sends text to a WebElement
     */
    public static void sendKeys(WebElement element, String text) {
        logger.info("Entering text into element");
        try {
            element.clear();
            element.sendKeys(text);
            logger.info("Text entered successfully");
        } catch (Exception e) {
            logger.error("Failed to enter text");
            throw new ElementInteractionException("SendKeys action failed", e);
        }
    }

    /**
     * Gets text from an element
     */
    public static String getText(WebDriver driver, By locator) {
        logger.debug("Getting text from element: {}", locator);
        try {
            WebElement element = WaitUtils.waitForElementToBeVisible(driver, locator);
            String text = element.getText();
            logger.debug("Text retrieved: {}", text);
            return text;
        } catch (Exception e) {
            logger.error("Failed to get text from element: {}", locator);
            throw new ElementInteractionException("GetText action failed: " + locator, e);
        }
    }

    /**
     * Gets text from a WebElement
     */
    public static String getText(WebElement element) {
        logger.debug("Getting text from element");
        try {
            String text = element.getText();
            logger.debug("Text retrieved: {}", text);
            return text;
        } catch (Exception e) {
            logger.error("Failed to get text");
            throw new ElementInteractionException("GetText action failed", e);
        }
    }

    /**
     * Checks if an element is displayed
     */
    public static boolean isDisplayed(WebDriver driver, By locator) {
        logger.debug("Checking if element is displayed: {}", locator);
        try {
            List<WebElement> elements = driver.findElements(locator);
            if (elements.isEmpty()) {
                return false;
            }
            boolean displayed = elements.get(0).isDisplayed();
            logger.debug("Element displayed: {}", displayed);
            return displayed;
        } catch (Exception e) {
            logger.debug("Element is not displayed: {}", locator);
            return false;
        }
    }

    /**
     * Checks if element is present in DOM
     */
    public static boolean isElementPresent(WebDriver driver, By locator) {
        logger.debug("Checking if element is present: {}", locator);
        try {
            WaitUtils.waitForElementToBePresent(driver, locator);
            logger.debug("Element is present: {}", locator);
            return true;
        } catch (Exception e) {
            logger.debug("Element is not present: {}", locator);
            return false;
        }
    }

    /**
     * Gets element count for a locator
     */
    public static int getElementCount(WebDriver driver, By locator) {
        logger.debug("Getting element count for locator: {}", locator);
        try {
            return driver.findElements(locator).size();
        } catch (Exception e) {
            logger.error("Failed to get element count: {}", locator);
            return 0;
        }
    }
}
