package com.broketogether.api.exception;

import java.util.stream.Collectors;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.broketogether.api.dto.ErrorResponse;

@RestControllerAdvice(basePackages = "com.broketogether.api.controller")
public class GlobalExceptionHandler {

  // 404 Not Found
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  // 404 Not Found — legacy checked exception used in a few service methods
  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  // 409 Conflict
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
  }

  // 403 Forbidden — our typed exception
  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  // 403 Forbidden — Spring Security's AccessDeniedException (thrown from service code)
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  // 400 Bad Request — validation on @Valid DTOs
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return build(HttpStatus.BAD_REQUEST, message);
  }

  // 400 Bad Request — explicit IllegalArgumentException
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  // 401 Unauthorized — bad credentials at login
  @ExceptionHandler(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUsernameNotFound(
      org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
    return build(HttpStatus.UNAUTHORIZED, "Invalid credentials");
  }

  // 500 — genuine unexpected errors; message is intentionally hidden from the client
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
    String path = request.getDescription(false);
    if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
      throw new RuntimeException(ex);
    }
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
  }

  // ── helper ───────────────────────────────────────────────────────────────────
  private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
    return new ResponseEntity<>(
        new ErrorResponse(status.value(), message, System.currentTimeMillis()),
        status);
  }
}
