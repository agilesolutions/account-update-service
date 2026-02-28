package com.carddemo.exception;

import com.carddemo.model.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global Exception Handler
 * Maps to COBOL error handling paragraphs:
 * - 9999-ABEND-PROGRAM → Internal server error
 * - RESP handling in EXEC CICS commands → HTTP status codes
 * - WS-ERR-MSG population → Error response messages
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle CardNotFoundException
     * Maps to COBOL: WHEN DFHRESP(NOTFND)
     */
    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCardNotFoundException(
            CardNotFoundException ex) {
        log.error("Card not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage(), generateErrorId()));
    }

    /**
     * Handle CardUpdateException
     * Maps to COBOL: MOVE 'Update failed' TO WS-ERR-MSG
     */
    @ExceptionHandler(CardUpdateException.class)
    public ResponseEntity<ApiResponse<Void>> handleCardUpdateException(
            CardUpdateException ex) {
        log.error("Card update error: {}", ex.getMessage());

        String message = ex.getValidationErrors() != null
                ? "Validation failed: " + ex.getValidationErrors()
                : ex.getMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message, generateErrorId()));
    }

    /**
     * Handle Bean Validation errors (@Valid)
     * Maps to COBOL: Field-level validation in 1400-SEND-EDIT-ERRMSG
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Bean validation errors: {}", errors);

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .status(400)
                .message("Validation failed")
                .data(errors)
                .transactionId(generateErrorId())
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle OptimisticLockingFailureException
     * Maps to COBOL: RESP = DFHRESP(DUPKEY) concurrent update scenario
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockException(
            OptimisticLockingFailureException ex) {
        log.error("Concurrent update conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409,
                        "Record was modified by another process. Please retry.",
                        generateErrorId()));
    }

    /**
     * Handle generic exceptions
     * Maps to COBOL: 9999-ABEND-PROGRAM
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500,
                        "An unexpected error occurred. Please contact support.",
                        generateErrorId()));
    }

    private String generateErrorId() {
        return "ERR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}