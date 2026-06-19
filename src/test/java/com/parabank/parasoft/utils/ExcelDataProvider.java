package com.parabank.parasoft.utils;

import com.parabank.parasoft.constants.FrameworkConstants;
import com.parabank.parasoft.exceptions.FrameworkException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ExcelDataProvider - Provides test data from Excel files
 * Replaces legacy ParaBankUtil's Excel functionality
 * Supports parameterized data-driven testing with proper error handling
 *
 * @author Automation Team
 * @version 1.0
 */
public class ExcelDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(ExcelDataProvider.class);
    private static final String DATA_FILE_PATH = Paths.get(
            System.getProperty("user.dir"),
            "src", "test", "resources", "data", FrameworkConstants.TEST_DATA_FILE
    ).toString();

    private ExcelDataProvider() {
        // Prevent instantiation
    }

    /**
     * Retrieves test data from Excel file
     *
     * @param sheetName the name of the sheet to read
     * @return 2D Object array containing the test data
     * @throws FrameworkException if file is not found or reading fails
     */
    public static Object[][] getTestData(String sheetName) {
        logger.info("Reading test data from Excel sheet: {}", sheetName);

        if (!Files.exists(Paths.get(DATA_FILE_PATH))) {
            logger.error("Excel data file not found: {}", DATA_FILE_PATH);
            throw new FrameworkException("Excel test data file not found: " + DATA_FILE_PATH);
        }

        try (FileInputStream file = new FileInputStream(DATA_FILE_PATH);
             Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheet(sheetName);

            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
                logger.warn("Sheet '{}' not found, using first sheet: '{}'", sheetName, sheet.getSheetName());
            }

            return extractDataFromSheet(sheet);
        } catch (FileNotFoundException e) {
            logger.error("Excel file not found: {}", DATA_FILE_PATH, e);
            throw new FrameworkException("Excel test data file not found: " + DATA_FILE_PATH, e);
        } catch (IOException e) {
            logger.error("Error reading Excel file: {}", DATA_FILE_PATH, e);
            throw new FrameworkException("Failed to read Excel test data file: " + DATA_FILE_PATH, e);
        }
    }

    /**
     * Extracts data from an Excel sheet
     *
     * @param sheet the Excel sheet to extract data from
     * @return 2D Object array containing the sheet data
     */
    private static Object[][] extractDataFromSheet(Sheet sheet) {
        int rowCount = sheet.getLastRowNum() + 1;
        int colCount = sheet.getRow(0) != null ? sheet.getRow(0).getLastCellNum() : 0;

        Object[][] data = new Object[rowCount][colCount];

        for (int i = 0; i < rowCount; i++) {
            var row = sheet.getRow(i);
            if (row == null) {
                // Skip null rows
                continue;
            }

            for (int j = 0; j < colCount; j++) {
                var cell = row.getCell(j);
                data[i][j] = cell != null ? cell.toString() : "";
                logger.debug("Data[{}][{}] = {}", i, j, data[i][j]);
            }
        }

        logger.info("Extracted {} rows x {} columns from sheet", rowCount, colCount);
        return data;
    }
}

