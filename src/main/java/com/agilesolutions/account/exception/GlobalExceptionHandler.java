// exception/GlobalExceptionHandler.java
package com.agilesolutions.account.exception;

import com.agilesolutions.account.domain.dto.ApiResponseDto;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler
 *
 * Maps Java exceptions back to COBOL error paragraph equivalents:
 *   AccountNotFoundException     -> COBOL FILE-STATUS '23' path
 *   BusinessValidationException  -> COBOL SEND-ERRMSG paragraph
 *   ConstraintViolationException -> COBOL EDIT-ACCOUNT-DATA error flags
 *   AccessDeniedException        -> COBOL EXEC CICS CHECK ACEE RESP(NOTAUTH)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ─── COBOL: FILE STATUS '23' - Record Not Found ──────────────────────────
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAccountNotFound(
            AccountNotFoundException ex) {
        log.warn("Account not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.error(ex.getMessage(), ex.getErrorCode()));
    }

    // ─── COBOL: SEND-ERRMSG / WS-ERROR-FLAGS path ───────────────────────────
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleBusinessValidation(
            BusinessValidationException ex) {
        log.warn("Business validation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(
                        ex.getMessage(),
                        ex.getErrorCode(),
                        ex.getValidationErrors()));
    }

    // ─── COBOL: EXEC CICS CHECK ACEE RESP(NOTAUTH) ──────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAccessDenied(
            AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error("Access denied", "ERR_FORBIDDEN"));
    }

    // ─── COBOL: EXEC CICS VERIFY PASSWORD RESP(ERROR) ───────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleBadCredentials(
            BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDto.error("Invalid username or password", "ERR_UNAUTHORIZED"));
    }

    // ─── COBOL: FILE STATUS '09' - Concurrent update ────────────────────────
    @ExceptionHandler({
            OptimisticLockException.class,
            jakarta.persistence.OptimisticLockException.class,
            org.springframework.orm.ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ApiResponseDto<Void>> handleOptimisticLock(Exception ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponseDto.error(
                        "Record was modified by another user. Please refresh and retry.",
                        "ERR_CONCURRENT_UPDATE"));
    }

    // ─── COBOL: FILE STATUS '22' - Duplicate Key ────────────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponseDto.error(
                        "Account already exists or data constraint violated",
                        "ERR_ACCOUNT_EXISTS"));
    }

    // ─── Bean Validation (@Valid) failures ───────────────────────────────────
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.warn("Validation errors: {}", errors);
        ApiResponseDto<Void> response = ApiResponseDto.error(
                "Request validation failed",
                "ERR_VALIDATION_FAILED",
                errors);

        return ResponseEntity.badRequest().body(response);
    }

    // ─── ConstraintViolation (path/query params) ─────────────────────────────
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {

        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(
                        "Constraint violation", "ERR_VALIDATION_FAILED", errors));
    }

    // ─── Catch-all ───────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                        "An unexpected error occurred", "ERR_INTERNAL"));
    }
}