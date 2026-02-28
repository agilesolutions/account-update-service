// dto/ApiResponseDto.java
package com.agilesolutions.account.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unified API response envelope
 * Replaces COBOL WS-RETURN-MSG / WS-ERROR-MSG commarea fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public class ApiResponseDto<T> {

    @Schema(description = "Whether operation succeeded")
    private boolean success;

    @Schema(description = "Human-readable message (mirrors COBOL SEND-PLAIN-TEXT)")
    private String message;

    @Schema(description = "Response payload")
    private T data;

    @Schema(description = "Validation/business error list (mirrors COBOL SEND-ERRMSG list)")
    private List<String> errors;

    @Schema(description = "Error code (mirrors COBOL WS-RETURN-CODE)")
    private String errorCode;

    @Schema(description = "Response timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ─── Factory helpers ─────────────────────────────────────────────────────

    public static <T> ApiResponseDto<T> success(String message, T data) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponseDto<T> error(
            String message, String errorCode, List<String> errors) {
        return ApiResponseDto.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponseDto<T> error(String message, String errorCode) {
        return error(message, errorCode, null);
    }
}