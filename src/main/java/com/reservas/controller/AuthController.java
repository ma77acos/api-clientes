// src/main/java/com/reservas/controller/AuthController.java
package com.reservas.controller;

import com.reservas.dto.request.ComplexRegistrationRequest;
import com.reservas.dto.request.LoginRequest;
import com.reservas.dto.request.RefreshTokenRequest;
import com.reservas.dto.response.AuthResponse;
import com.reservas.dto.response.ComplexRegistrationResponse;
import com.reservas.dto.response.TokenRefreshResponse;
import com.reservas.service.AuthService;
import com.reservas.service.ComplexRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}