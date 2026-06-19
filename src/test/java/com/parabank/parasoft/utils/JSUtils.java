package com.parabank.parasoft.utils;

import com.parabank.parasoft.exceptions.FrameworkException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSUtils - JavaScript execution utilities
 * Provides methods for JavaScript execution, DOM manipulation, and advanced interactions
 *
 * @author Automation Team
 * @version 1.0
 */
public class JSUtils {
    private static final Logger logger = LoggerFactory.getLogger(JSUtils.class);

    private JSUtils() {
        // Prevent instantiation
    }

    /**
     * Executes JavaScript code
     *
     * @param driver the WebDriver instance
     * @param script the JavaScript code to execute
     * @return the result of script execution
     */
    public static Object executeScript(WebDriver driver, String script) {
        logger.debug("Executing JavaScript: {}", script);
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(script);
            logger.debug("JavaScript executed successfully");
            return result;
        } catch (Exception e) {
            logger.error("Failed to execute JavaScript: {}", script, e);
            throw new FrameworkException("JavaScript execution failed: " + script, e);
        }
    }

    /**
     * Executes JavaScript with arguments
     *
     * @param driver the WebDriver instance
     * @param script the JavaScript code
     * @param args   the arguments to pass
     * @return the result of script execution
     */
    public static Object executeScript(WebDriver driver, String script, Object... args) {
        logger.debug("Executing JavaScript with arguments");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            return js.executeScript(script, args);
        } catch (Exception e) {
            logger.error("Failed to execute JavaScript with arguments", e);
            throw new FrameworkException("JavaScript execution with arguments failed", e);
        }
    }

    /**
     * Executes asynchronous JavaScript
     *
     * @param driver the WebDriver instance
     * @param script the asynchronous JavaScript code
     * @return the result
     */
    public static Object executeAsyncScript(WebDriver driver, String script) {
        logger.debug("Executing asynchronous JavaScript");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            return js.executeAsyncScript(script);
        } catch (Exception e) {
            logger.error("Failed to execute async JavaScript", e);
            throw new FrameworkException("Asynchronous JavaScript execution failed", e);
        }
    }

    /**
     * Clicks on an element using JavaScript (useful for hidden elements)
     *
     * @param driver  the WebDriver instance
     * @param element the element to click
     */
    public static void clickElementByJS(WebDriver driver, WebElement element) {
        logger.debug("Clicking element using JavaScript");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", element);
            logger.debug("Element clicked using JavaScript");
        } catch (Exception e) {
            logger.error("Failed to click element using JavaScript", e);
            throw new FrameworkException("JavaScript click failed", e);
        }
    }

    /**
     * Sends text to an element using JavaScript (bypasses input restrictions)
     *
     * @param driver the WebDriver instance
     * @param element the target element
     * @param text   the text to send
     */
    public static void sendTextByJS(WebDriver driver, WebElement element, String text) {
        logger.debug("Setting text to element using JavaScript: {}", text);
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Pass text as arguments[1] — never concatenate user data into JS string literals
            js.executeScript("arguments[0].value = arguments[1];", element, text);
            logger.debug("Text set successfully using JavaScript");
        } catch (Exception e) {
            logger.error("Failed to set text using JavaScript", e);
            throw new FrameworkException("JavaScript text input failed", e);
        }
    }

    /**
     * Scrolls to an element
     *
     * @param driver  the WebDriver instance
     * @param element the element to scroll to
     */
    public static void scrollToElement(WebDriver driver, WebElement element) {
        logger.debug("Scrolling to element");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView(true);", element);
            logger.debug("Scrolled to element successfully");
        } catch (Exception e) {
            logger.error("Failed to scroll to element", e);
            throw new FrameworkException("Scroll to element failed", e);
        }
    }

    /**
     * Scrolls the page by pixel value
     *
     * @param driver the WebDriver instance
     * @param pixels the number of pixels to scroll
     */
    public static void scrollByPixels(WebDriver driver, int pixels) {
        logger.debug("Scrolling by {} pixels", pixels);
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollBy(0, " + pixels + ");");
            logger.debug("Scrolled successfully");
        } catch (Exception e) {
            logger.error("Failed to scroll", e);
            throw new FrameworkException("Scroll failed", e);
        }
    }

    /**
     * Scrolls to top of page
     *
     * @param driver the WebDriver instance
     */
    public static void scrollToTop(WebDriver driver) {
        logger.debug("Scrolling to top of page");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, 0);");
            logger.debug("Scrolled to top");
        } catch (Exception e) {
            logger.error("Failed to scroll to top", e);
            throw new FrameworkException("Scroll to top failed", e);
        }
    }

    /**
     * Scrolls to bottom of page
     *
     * @param driver the WebDriver instance
     */
    public static void scrollToBottom(WebDriver driver) {
        logger.debug("Scrolling to bottom of page");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            logger.debug("Scrolled to bottom");
        } catch (Exception e) {
            logger.error("Failed to scroll to bottom", e);
            throw new FrameworkException("Scroll to bottom failed", e);
        }
    }

    /**
     * Gets element text using JavaScript
     *
     * @param driver  the WebDriver instance
     * @param element the element to get text from
     * @return the element text
     */
    public static String getTextByJS(WebDriver driver, WebElement element) {
        logger.debug("Getting element text using JavaScript");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String text = (String) js.executeScript("return arguments[0].textContent;", element);
            logger.debug("Text retrieved: {}", text);
            return text;
        } catch (Exception e) {
            logger.error("Failed to get text using JavaScript", e);
            throw new FrameworkException("Get text failed", e);
        }
    }

    /**
     * Checks if element is visible on the page
     *
     * @param driver  the WebDriver instance
     * @param element the element to check
     * @return true if element is visible
     */
    public static boolean isElementVisible(WebDriver driver, WebElement element) {
        logger.debug("Checking if element is visible using JavaScript");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Boolean visible = (Boolean) js.executeScript(
                    "return (typeof jQuery != 'undefined') ? jQuery(arguments[0]).is(':visible') : true;",
                    element
            );
            logger.debug("Element visible: {}", visible);
            return visible != null && visible;
        } catch (Exception e) {
            logger.error("Failed to check element visibility", e);
            return false;
        }
    }

    /**
     * Removes element from DOM
     *
     * @param driver  the WebDriver instance
     * @param element the element to remove
     */
    public static void removeElement(WebDriver driver, WebElement element) {
        logger.debug("Removing element from DOM");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].remove();", element);
            logger.debug("Element removed successfully");
        } catch (Exception e) {
            logger.error("Failed to remove element", e);
            throw new FrameworkException("Remove element failed", e);
        }
    }

    /**
     * Highlights element with border
     *
     * @param driver  the WebDriver instance
     * @param element the element to highlight
     */
    public static void highlightElement(WebDriver driver, WebElement element) {
        logger.debug("Highlighting element");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].style.border='3px solid red';", element);
            logger.debug("Element highlighted");
        } catch (Exception e) {
            logger.error("Failed to highlight element", e);
            throw new FrameworkException("Highlight element failed", e);
        }
    }

    /**
     * Removes highlight from element
     *
     * @param driver  the WebDriver instance
     * @param element the element to remove highlight from
     */
    public static void removeHighlight(WebDriver driver, WebElement element) {
        logger.debug("Removing highlight from element");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].style.border='';", element);
            logger.debug("Highlight removed");
        } catch (Exception e) {
            logger.error("Failed to remove highlight", e);
            throw new FrameworkException("Remove highlight failed", e);
        }
    }

    /**
     * Gets page title using JavaScript
     *
     * @param driver the WebDriver instance
     * @return the page title
     */
    public static String getPageTitle(WebDriver driver) {
        logger.debug("Getting page title using JavaScript");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String title = (String) js.executeScript("return document.title;");
            logger.debug("Page title: {}", title);
            return title;
        } catch (Exception e) {
            logger.error("Failed to get page title", e);
            throw new FrameworkException("Get page title failed", e);
        }
    }

    /**
     * Gets page URL using JavaScript
     *
     * @param driver the WebDriver instance
     * @return the page URL
     */
    public static String getPageURL(WebDriver driver) {
        logger.debug("Getting page URL using JavaScript");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String url = (String) js.executeScript("return window.location.href;");
            logger.debug("Page URL: {}", url);
            return url;
        } catch (Exception e) {
            logger.error("Failed to get page URL", e);
            throw new FrameworkException("Get page URL failed", e);
        }
    }
}

