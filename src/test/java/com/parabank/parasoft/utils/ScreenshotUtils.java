package com.parabank.parasoft.utils;

import com.parabank.parasoft.constants.PathConstants;
import com.parabank.parasoft.constants.ReportConstants;
import com.parabank.parasoft.exceptions.FrameworkException;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ScreenshotUtils - Screenshot capture and management utilities
 * Provides methods for capturing screenshots in various formats
 *
 * @author Automation Team
 * @version 1.0
 */
public class ScreenshotUtils {
    private static final Logger logger = LoggerFactory.getLogger(ScreenshotUtils.class);
    private static final String SCREENSHOT_DIR = PathConstants.SCREENSHOTS_PATH;
    private static final String SCREENSHOT_EXTENSION = ".png";

    private ScreenshotUtils() {
        // Prevent instantiation
    }

    static {
        // Create screenshot directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));
            logger.info("Screenshot directory created/verified: {}", SCREENSHOT_DIR);
        } catch (IOException e) {
            logger.error("Failed to create screenshot directory: {}", SCREENSHOT_DIR, e);
        }
    }

    /**
     * Captures a screenshot and saves it with timestamp
     *
     * @param driver       the WebDriver instance
     * @param testName     the name of the test
     * @return the path to the saved screenshot
     */
    public static String captureScreenshot(WebDriver driver, String testName) {
        logger.info("Capturing screenshot for test: {}", testName);
        try {
            String timestamp = new SimpleDateFormat(ReportConstants.TIMESTAMP_FORMAT).format(new Date());
            String fileName = testName + "_" + timestamp + SCREENSHOT_EXTENSION;
            String filePath = SCREENSHOT_DIR + File.separator + fileName;

            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File(filePath));

            logger.info("Screenshot captured successfully: {}", filePath);
            return filePath;
        } catch (IOException e) {
            logger.error("Failed to capture screenshot", e);
            throw new FrameworkException("Screenshot capture failed", e);
        }
    }

    /**
     * Captures a screenshot with a specific file name
     *
     * @param driver       the WebDriver instance
     * @param fileName     the file name (without extension)
     * @return the path to the saved screenshot
     */
    public static String captureScreenshotWithFileName(WebDriver driver, String fileName) {
        logger.info("Capturing screenshot with custom name: {}", fileName);
        try {
            String filePath = SCREENSHOT_DIR + File.separator + fileName + SCREENSHOT_EXTENSION;

            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File(filePath));

            logger.info("Screenshot saved: {}", filePath);
            return filePath;
        } catch (IOException e) {
            logger.error("Failed to capture screenshot with custom name", e);
            throw new FrameworkException("Screenshot capture with custom name failed", e);
        }
    }

    /**
     * Captures screenshot as byte array (useful for reports)
     *
     * @param driver the WebDriver instance
     * @return byte array of the screenshot
     */
    public static byte[] captureScreenshotAsBytes(WebDriver driver) {
        logger.debug("Capturing screenshot as bytes");
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            logger.error("Failed to capture screenshot as bytes", e);
            throw new FrameworkException("Screenshot capture as bytes failed", e);
        }
    }

    /**
     * Captures screenshot as Base64 string (useful for JSON/XML reports)
     *
     * @param driver the WebDriver instance
     * @return Base64 encoded screenshot string
     */
    public static String captureScreenshotAsBase64(WebDriver driver) {
        logger.debug("Capturing screenshot as Base64");
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            logger.error("Failed to capture screenshot as Base64", e);
            throw new FrameworkException("Screenshot capture as Base64 failed", e);
        }
    }

    /**
     * Gets screenshot directory path
     *
     * @return the screenshot directory path
     */
    public static String getScreenshotDirectory() {
        return SCREENSHOT_DIR;
    }

    /**
     * Cleans old screenshots — useful in CI pipelines to prevent build artifact bloat.
     *
     * @param maxAgeInMillis maximum age of screenshots to keep (in milliseconds)
     */
    public static void cleanOldScreenshots(long maxAgeInMillis) {
        logger.info("Cleaning old screenshots older than {} ms", maxAgeInMillis);
        try {
            File directory = new File(SCREENSHOT_DIR);
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    long currentTime = System.currentTimeMillis();
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(SCREENSHOT_EXTENSION)) {
                            long fileAge = currentTime - file.lastModified();
                            if (fileAge > maxAgeInMillis) {
                                file.delete();
                                logger.debug("Deleted old screenshot: {}", file.getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to clean old screenshots", e);
        }
    }
}

