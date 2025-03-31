package com.mobicommServices3.exception;

public class SubscriberException extends RuntimeException {
    private final ErrorCode errorCode;

    public enum ErrorCode {
        SUBSCRIBER_NOT_FOUND,
        INVALID_EMAIL,
        EMAIL_IN_USE,
        MOBILE_NUMBER_IN_USE,
        INVALID_MOBILE_NUMBER,
        INVALID_TOKEN,
        VALIDATION_ERROR
    }

    public SubscriberException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SubscriberException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}