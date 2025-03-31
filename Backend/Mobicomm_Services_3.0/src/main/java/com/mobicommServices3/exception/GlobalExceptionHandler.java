package com.mobicommServices3.exception;

import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // Handle MobileNumberNotFoundException (New)
	@ExceptionHandler(MobileNumberNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleMobileNumberNotFoundException(MobileNumberNotFoundException ex) {
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "error");
	    response.put("message", ex.getMessage());
	    // Optionally add an errorCode if you define one in MobileNumberNotFoundException
	    log.warn("Mobile number not found: {}", ex.getMessage());
	    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

    // Handle AdminException
    @ExceptionHandler(AdminException.class)
    public ResponseEntity<Map<String, Object>> handleAdminException(AdminException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", ex.getMessage());
        response.put("errorCode", ex.getErrorCode().name());

        HttpStatus status;
        switch (ex.getErrorCode()) {
            case PLAN_NOT_FOUND:
            case CATEGORY_NOT_FOUND:
            case TRANSACTION_NOT_FOUND:
                status = HttpStatus.NOT_FOUND;
                break;
            case INVALID_PRICE:
            case INVALID_VALIDITY_DAYS:
            case VALIDATION_ERROR:
                status = HttpStatus.BAD_REQUEST;
                break;
            case CATEGORY_NAME_IN_USE:
                status = HttpStatus.CONFLICT;
                break;
            case NOTIFICATION_FAILED:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.error("Admin error [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        return new ResponseEntity<>(response, status);
    }

    // Handle SubscriberException
    @ExceptionHandler(SubscriberException.class)
    public ResponseEntity<Map<String, Object>> handleSubscriberException(SubscriberException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", ex.getMessage());
        response.put("errorCode", ex.getErrorCode().name());

        HttpStatus status;
        switch (ex.getErrorCode()) {
            case SUBSCRIBER_NOT_FOUND:
                status = HttpStatus.NOT_FOUND;
                break;
            case INVALID_MOBILE_NUMBER:
            case VALIDATION_ERROR:
                status = HttpStatus.BAD_REQUEST;
                break;
            case INVALID_TOKEN:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case EMAIL_IN_USE:
            case MOBILE_NUMBER_IN_USE:
            case INVALID_EMAIL:
                status = HttpStatus.CONFLICT;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.error("Subscriber error [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        return new ResponseEntity<>(response, status);
    }

    // Handle validation errors (e.g., @Valid failures)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Validation failed");

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        response.put("errors", errors);

        log.error("Validation error: {}", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Handle Razorpay-specific exceptions
    @ExceptionHandler(RazorpayException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleRazorpayException(RazorpayException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Payment processing failed: " + ex.getMessage());

        log.error("Razorpay error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Handle access denied exceptions (e.g., Spring Security authorization failures)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Access denied: You do not have permission to perform this action.");

        log.warn("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    // Handle generic runtime exceptions (fallback for unhandled RuntimeExceptions)
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", ex.getMessage());

        log.error("Runtime error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    

    // Handle all other unexpected exceptions
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "An unexpected error occurred. Please try again later.");

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}