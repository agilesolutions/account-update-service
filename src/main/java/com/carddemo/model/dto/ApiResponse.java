package com.carddemo.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "HTTP status code")
    private int status;

    @Schema(description = "Response message")
    private String message;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Transaction ID for tracking")
    private String transactionId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "Success flag")
    private boolean success;

    public static <T> ApiResponse<T> success(T data, String message, String transactionId) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .transactionId(transactionId)
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, String transactionId) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .transactionId(transactionId)
                .timestamp(LocalDateTime.now())
                .success(false)
                .build();
    }
}