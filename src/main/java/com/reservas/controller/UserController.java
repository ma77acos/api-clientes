// src/main/java/com/reservas/controller/UserController.java
package com.reservas.controller;

import com.reservas.dto.request.ChangePasswordRequest;
import com.reservas.dto.request.UpdateProfileRequest;
import com.reservas.dto.response.UserResponse;
import com.reservas.entity.User;
import com.reservas.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User currentUser) {
        log.info("GET /api/users/profile - User: {}", currentUser.getEmail());
        UserResponse response = userService.getProfile(currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("PUT /api/users/profile - User: {}", currentUser.getEmail());
        UserResponse response = userService.updateProfile(currentUser, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("POST /api/users/change-password - User: {}", currentUser.getEmail());
        userService.changePassword(currentUser, request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
    }
}