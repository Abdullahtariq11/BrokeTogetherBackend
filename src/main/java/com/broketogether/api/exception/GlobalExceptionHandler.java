package com.broketogether.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.broketogether.api.dto.ErrorResponse;

@RestControllerAdvice(basePackages = "com.broketogether.api.controller")  // ← Only apply to your controllers
public class GlobalExceptionHandler {

    // 1. Handle Membership & Permission Issues (403 Forbidden)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, 
            WebRequest request) {
        
        // Skip Swagger paths
        String path = request.getDescription(false);
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
            throw ex;  // Let Spring handle Swagger errors
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("member") || ex.getMessage().contains("permission")) {
                status = HttpStatus.FORBIDDEN;
            } else if (ex.getMessage().contains("not exist") || ex.getMessage().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (ex.getMessage().contains("Invalid") || ex.getMessage().contains("must")) {
                status = HttpStatus.BAD_REQUEST;
            }
        }

        ErrorResponse error = new ErrorResponse(
            status.value(),
            ex.getMessage() != null ? ex.getMessage() : "An error occurred",
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, status);
    }

    // 2. Handle IllegalArgumentException (400 Bad Request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 3. Handle UsernameNotFoundException (401 Unauthorized)
    @ExceptionHandler(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Invalid credentials",
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 4. Handle Generic Fallback (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, 
            WebRequest request) {
        
        // Skip Swagger paths
        String path = request.getDescription(false);
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
            throw new RuntimeException(ex);  // Let Spring handle Swagger errors
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}