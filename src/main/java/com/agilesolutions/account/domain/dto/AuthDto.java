// dto/AuthDto.java
package com.agilesolutions.account.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class AuthDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Login request")
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        @Schema(example = "admin")
        private String username;

        @NotBlank(message = "Password is required")
        @Schema(example = "admin123")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Login response with JWT token")
    public static class LoginResponse {
        @Schema(description = "JWT access token")
        private String accessToken;

        @Schema(description = "Token type", example = "Bearer")
        private String tokenType;

        @Schema(description = "Token expiration in milliseconds")
        private Long expiresIn;

        @Schema(description = "Authenticated username")
        private String username;
    }
}