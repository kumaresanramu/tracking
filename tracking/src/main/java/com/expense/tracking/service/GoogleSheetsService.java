package com.expense.tracking.service;

import com.expense.tracking.entity.Expense;
import com.expense.tracking.exception.GoogleSheetsException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired(required = false)
    private Sheets sheetsClient;

    @Value("${google.sheets.spreadsheet.id:}")
    private String spreadsheetId;

    @Value("${google.sheets.enabled:false}")
    private boolean googleSheetsEnabled;

    /**
     * Check if Google Sheets integration is available and connected
     */
    public boolean isConnected() {
        return googleSheetsEnabled && sheetsClient != null && spreadsheetId != null && !spreadsheetId.isEmpty();
    }

    /**
     * Syncs an expense to the appropriate Google Sheets tab
     */
    @Retryable(value = {IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void syncExpenseToSheet(Expense expense) {
        if (!isConnected()) {
            logger.warn("Google Sheets integration is not available. Expense {} will not be synced.", expense.getId());
            return;
        }
        
        try {
            if (spreadsheetId == null || spreadsheetId.isEmpty()) {
                throw new GoogleSheetsException("Google Sheets spreadsheet ID not configured");
            }
            
            String sheetName = getMonthlySheetName(expense.getDate().getYear(), expense.getDate().getMonthValue());
            
            // Ensure the monthly sheet exists
            createMonthlySheetIfNotExists(sheetName);
            
            // Add the expense data
            List<Object> rowData = Arrays.asList(
                expense.getDate().format(DATE_FORMATTER),
                expense.getAmount().toString(),
                expense.getCategory() != null ? expense.getCategory().getName() : "",
                expense.getCategory() != null && expense.getCategory().getParent() != null ? 
                    expense.getCategory().getParent().getName() : "",
                expense.getDescription() != null ? expense.getDescription() : "",
                expense.getCreatedAt() != null ? expense.getCreatedAt().format(TIMESTAMP_FORMATTER) : "",
                expense.getUpdatedAt() != null ? expense.getUpdatedAt().format(TIMESTAMP_FORMATTER) : ""
            );

            ValueRange valueRange = new ValueRange()
                    .setValues(Arrays.asList(rowData));

            sheetsClient.spreadsheets().values()
                    .append(spreadsheetId, sheetName + "!A:G", valueRange)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

            logger.info("Successfully synced expense {} to Google Sheets", expense.getId());

        } catch (IOException e) {
            logger.error("Failed to sync expense {} to Google Sheets: {}", expense.getId(), e.getMessage());
            throw new GoogleSheetsException("Google Sheets sync failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing expense in Google Sheets
     */
    @Retryable(value = {IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void updateExpenseInSheet(Expense expense) {
        if (!isConnected()) {
            logger.warn("Google Sheets integration is not available. Expense {} will not be updated.", expense.getId());
            return;
        }
        
        try {
            String sheetName = getMonthlySheetName(expense.getDate().getYear(), expense.getDate().getMonthValue());
            
            // For now, we'll append the updated expense as a new row
            // In a production system, you'd want to find and update the specific row
            syncExpenseToSheet(expense);
            
            logger.info("Successfully updated expense {} in Google Sheets", expense.getId());

        } catch (Exception e) {
            logger.error("Failed to update expense {} in Google Sheets: {}", expense.getId(), e.getMessage());
            throw new GoogleSheetsException("Google Sheets update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new monthly sheet if it doesn't exist
     */
    public void createMonthlySheet(int year, int month) {
        String sheetName = getMonthlySheetName(year, month);
        createMonthlySheetIfNotExists(sheetName);
    }

    /**
     * Retrieves expenses from Google Sheets for a specific month
     */
    public List<Expense> getExpensesFromSheet(int year, int month) {
        if (!isConnected()) {
            logger.warn("Google Sheets integration is not available. Cannot retrieve expenses for {}/{}", month, year);
            return new ArrayList<>();
        }
        
        try {
            String sheetName = getMonthlySheetName(year, month);
            
            ValueRange response = sheetsClient.spreadsheets().values()
                    .get(spreadsheetId, sheetName + "!A:G")
                    .execute();

            List<List<Object>> values = response.getValues();
            List<Expense> expenses = new ArrayList<>();

            if (values != null && values.size() > 1) { // Skip header row
                for (int i = 1; i < values.size(); i++) {
                    List<Object> row = values.get(i);
                    if (row.size() >= 3) { // Minimum required columns
                        // Note: This is a simplified conversion
                        // In a real implementation, you'd need proper parsing and entity creation
                        logger.info("Retrieved expense row: {}", row);
                    }
                }
            }

            return expenses;

        } catch (IOException e) {
            logger.error("Failed to retrieve expenses from Google Sheets: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String getMonthlySheetName(int year, int month) {
        String[] monthNames = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        return monthNames[month - 1] + " " + year;
    }

    private void createMonthlySheetIfNotExists(String sheetName) {
        try {
            // Check if sheet already exists
            Spreadsheet spreadsheet = sheetsClient.spreadsheets().get(spreadsheetId).execute();
            boolean sheetExists = spreadsheet.getSheets().stream()
                    .anyMatch(sheet -> sheet.getProperties().getTitle().equals(sheetName));

            if (!sheetExists) {
                // Create new sheet
                AddSheetRequest addSheetRequest = new AddSheetRequest()
                        .setProperties(new SheetProperties().setTitle(sheetName));

                BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                        .setRequests(Arrays.asList(new Request().setAddSheet(addSheetRequest)));

                sheetsClient.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();

                // Add header row
                List<Object> headers = Arrays.asList(
                    "Date", "Amount", "Category", "Subcategory", "Description", "Created At", "Updated At"
                );

                ValueRange headerRange = new ValueRange()
                        .setValues(Arrays.asList(headers));

                sheetsClient.spreadsheets().values()
                        .update(spreadsheetId, sheetName + "!A1:G1", headerRange)
                        .setValueInputOption("RAW")
                        .execute();

                logger.info("Created new monthly sheet: {}", sheetName);
            }

        } catch (IOException e) {
            logger.error("Failed to create monthly sheet {}: {}", sheetName, e.getMessage());
            throw new RuntimeException("Failed to create monthly sheet", e);
        }
    }
}