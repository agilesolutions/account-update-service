// controller/AuthController.java
package com.agilesolutions.account.controller;

import com.agilesolutions.account.domain.dto.ApiResponseDto;
import com.agilesolutions.account.domain.dto.AuthDto;
import com.agilesolutions.account.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Auth controller - replaces COBOL CICS SIGNON / SIGNOFF commands
 * and EXEC CICS VERIFY PASSWORD(...) logic
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication",
        description = "JWT auth replacing COBOL CICS SIGNON/VERIFY PASSWORD")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    // ─── Replaces COBOL: EXEC CICS SIGNON USERID(...) PASSWORD(...) ─────────
    @PostMapping("/login")
    @Operation(
            summary     = "Authenticate user",
            description = "Replaces COBOL CICS SIGNON / EXEC CICS VERIFY PASSWORD"
    )
    public ResponseEntity<ApiResponseDto<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest loginRequest) {

        log.info("Login attempt for user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);

        AuthDto.LoginResponse response = AuthDto.LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .username(authentication.getName())
                .build();

        log.info("User authenticated successfully: {}", loginRequest.getUsername());
        return ResponseEntity.ok(ApiResponseDto.success("Login successful", response));
    }

    // ─── Replaces COBOL: EXEC CICS SIGNOFF ──────────────────────────────────
    @PostMapping("/logout")
    @Operation(
            summary     = "Logout",
            description = "Replaces COBOL EXEC CICS SIGNOFF (client discards token)"
    )
    public ResponseEntity<ApiResponseDto<Void>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponseDto.success("Logged out successfully", null));
    }
}