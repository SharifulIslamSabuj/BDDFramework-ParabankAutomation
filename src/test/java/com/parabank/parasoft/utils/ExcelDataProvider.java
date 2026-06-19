package com.parabank.parasoft.utils;

import com.parabank.parasoft.constants.PathConstants;
import com.parabank.parasoft.exceptions.FrameworkException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * ExcelDataProvider — reads test data from the project's Excel workbook and
 * exposes it both as a plain {@code Object[][]} (for Cucumber step definitions)
 * and as a TestNG {@code @DataProvider} (for direct TestNG data-driven tests).
 *
 * <p>Usage from a Cucumber step definition:
 * <pre>
 *     Object[][] data = ExcelDataProvider.getTestData("register");
 *     // data[0] = first data row, data[1] = second row, …
 * </pre>
 *
 * <p>Usage from a TestNG test class:
 * <pre>
 *     {@literal @}Test(dataProvider = "registrationData",
 *           dataProviderClass = ExcelDataProvider.class)
 *     public void testRegistration(String firstName, String lastName, String address) { … }
 * </pre>
 */
public class ExcelDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(ExcelDataProvider.class);

    private static final String DATA_FILE_PATH =
            PathConstants.DATA_PATH + File.separator + PathConstants.TEST_DATA_FILE;

    private ExcelDataProvider() {}

    // =========================================================================
    // Public API — plain Object[][] (used by Cucumber step definitions)
    // =========================================================================

    /**
     * Returns all rows from {@code sheetName} (or the first sheet if not found)
     * as a 2-D {@code Object[][]} array.  Row 0 in the returned array is the
     * first row of the sheet (typically a header row — callers decide whether to
     * skip it).
     *
     * @param sheetName the Excel sheet to read
     * @return all rows × columns; never {@code null}
     * @throws FrameworkException if the data file is missing or unreadable
     */
    public static Object[][] getTestData(String sheetName) {
        return getTestData(sheetName, false);
    }

    /**
     * Returns rows from {@code sheetName}, optionally skipping the first
     * (header) row.
     *
     * @param sheetName  the Excel sheet to read
     * @param skipHeader when {@code true}, row 0 of the sheet is omitted
     * @return data rows as {@code Object[][]}; never {@code null}
     */
    public static Object[][] getTestData(String sheetName, boolean skipHeader) {
        logger.info("Reading Excel data — sheet: '{}', skipHeader: {}", sheetName, skipHeader);
        assertFileExists();

        try (FileInputStream fis = new FileInputStream(DATA_FILE_PATH);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = resolveSheet(workbook, sheetName);
            return extractRows(sheet, skipHeader);

        } catch (IOException e) {
            logger.error("Failed to read Excel file: {}", DATA_FILE_PATH, e);
            throw new FrameworkException("Failed to read Excel test data: " + DATA_FILE_PATH, e);
        }
    }

    /**
     * Returns a single row by zero-based index.
     *
     * @param sheetName the Excel sheet to read
     * @param rowIndex  zero-based row index (0 = first row, which may be a header)
     * @return a single-row {@code Object[]} or an empty array if the index is out of range
     */
    public static Object[] getTestDataRow(String sheetName, int rowIndex) {
        Object[][] all = getTestData(sheetName, false);
        if (rowIndex < 0 || rowIndex >= all.length) {
            logger.warn("Row index {} out of range (total rows: {}). Returning empty row.", rowIndex, all.length);
            return new Object[0];
        }
        return all[rowIndex];
    }

    /**
     * Returns the number of data rows in the sheet (excluding the header when
     * {@code skipHeader} is {@code true}).
     */
    public static int getRowCount(String sheetName, boolean skipHeader) {
        return getTestData(sheetName, skipHeader).length;
    }

    // =========================================================================
    // TestNG @DataProvider — usable from any TestNG test class via
    //   @Test(dataProvider = "registrationData", dataProviderClass = ExcelDataProvider.class)
    // =========================================================================

    /**
     * TestNG DataProvider that supplies registration test data from the default
     * sheet ({@value PathConstants#TEST_DATA_SHEET}).  Row 0 of the sheet is
     * treated as a header and is skipped automatically.
     *
     * <p>Each yielded {@code Object[]} represents one registration candidate.
     * Expected column layout (columns may be extended without breaking callers):
     * <ol>
     *   <li>col 0 — first name</li>
     *   <li>col 1 — last name</li>
     *   <li>col 2 — address</li>
     * </ol>
     */
    @DataProvider(name = "registrationData", parallel = false)
    public static Object[][] registrationData() {
        return getTestData(PathConstants.TEST_DATA_SHEET, true);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static void assertFileExists() {
        if (!Files.exists(Paths.get(DATA_FILE_PATH))) {
            logger.error("Excel data file not found: {}", DATA_FILE_PATH);
            throw new FrameworkException("Excel test data file not found: " + DATA_FILE_PATH);
        }
    }

    private static Sheet resolveSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
            logger.warn("Sheet '{}' not found — using first sheet: '{}'", sheetName, sheet.getSheetName());
        }
        return sheet;
    }

    /**
     * Extracts all rows from {@code sheet} into a 2-D array.
     * Empty rows in the middle of the sheet are preserved as empty {@code Object[]}.
     * Trailing empty rows reported by Apache POI are included (callers should
     * validate their data before use).
     */
    private static Object[][] extractRows(Sheet sheet, boolean skipHeader) {
        int firstRow = skipHeader ? 1 : 0;
        int lastRow  = sheet.getLastRowNum();

        if (lastRow < firstRow) {
            logger.warn("Sheet '{}' contains no data rows (lastRowNum={}, skipHeader={}).",
                    sheet.getSheetName(), lastRow, skipHeader);
            return new Object[0][0];
        }

        int colCount = sheet.getRow(0) != null ? sheet.getRow(0).getLastCellNum() : 0;
        List<Object[]> rows = new ArrayList<>();

        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            Object[] rowData = new Object[colCount];
            for (int c = 0; c < colCount; c++) {
                if (row != null) {
                    Cell cell = row.getCell(c);
                    rowData[c] = (cell != null) ? cell.toString() : "";
                } else {
                    rowData[c] = "";
                }
            }
            rows.add(rowData);
            logger.debug("Row {}: {}", r, java.util.Arrays.toString(rowData));
        }

        logger.info("Extracted {} data rows × {} columns from sheet '{}'",
                rows.size(), colCount, sheet.getSheetName());
        return rows.toArray(new Object[0][0]);
    }
}
