// src/main/java/com/reservas/controller/AuthController.java
package com.reservas.controller;

import com.reservas.dto.request.*;
import com.reservas.dto.response.AuthResponse;
import com.reservas.dto.response.ComplexRegistrationResponse;
import com.reservas.dto.response.TokenRefreshResponse;
import com.reservas.service.AuthService;
import com.reservas.service.ComplexRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ComplexRegistrationService complexRegistrationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-complex")
    public ResponseEntity<ComplexRegistrationResponse> registerComplex(
            @Valid @RequestBody ComplexRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complexRegistrationService.registerComplex(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.sendPasswordResetEmail(request);

        return ResponseEntity.ok(Map.of(
                "message", "Si el email existe, recibirás instrucciones para resetear tu contraseña"
        ));
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<Map<String, Boolean>> validateResetToken(
            @RequestParam String token) {

        boolean isValid = authService.validatePasswordResetToken(token);

        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(Map.of(
                "message", "Contraseña actualizada exitosamente"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody Map<String, String> request) {

        authService.logout(request.get("refreshToken"));

        return ResponseEntity.ok(Map.of(
                "message", "Sesión cerrada exitosamente"
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}