package com.expense.tracking.exception;

/**
 * Exception thrown when Google Sheets operations fail
 */
public class GoogleSheetsException extends RuntimeException {

    public GoogleSheetsException(String message) {
        super(message);
    }

    public GoogleSheetsException(String message, Throwable cause) {
        super(message, cause);
    }
}