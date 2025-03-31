package com.mobicommServices3.exception;

public class AdminException extends RuntimeException {
    private final ErrorCode errorCode;

    public enum ErrorCode {
        PLAN_NOT_FOUND,           // When a recharge plan is not found
        CATEGORY_NOT_FOUND,       // When a category is not found
        INVALID_PRICE,            // For invalid price values
        INVALID_VALIDITY_DAYS,    // For invalid validity days
        TRANSACTION_NOT_FOUND,    // For when transactions are not found
        NOTIFICATION_FAILED,      // For notification failures
        VALIDATION_ERROR,         // For general validation errors
        CATEGORY_NAME_IN_USE      // For duplicate category names
    }

    public AdminException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AdminException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}