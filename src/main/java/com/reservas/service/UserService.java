// src/main/java/com/reservas/service/UserService.java
package com.reservas.service;

import com.reservas.dto.request.ChangePasswordRequest;
import com.reservas.dto.request.UpdateProfileRequest;
import com.reservas.dto.response.UserResponse;
import com.reservas.entity.User;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(User currentUser) {
        log.info("Obteniendo perfil para usuario: {}", currentUser.getEmail());

        // Recargar usuario para tener datos actualizados
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        log.info("Actualizando perfil para usuario: {}", currentUser.getEmail());

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        user.setDisplayName(request.getDisplayName());
        user.setPhotoUrl(request.getPhotoUrl());

        User savedUser = userRepository.save(user);
        log.info("Perfil actualizado exitosamente para: {}", savedUser.getEmail());

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        log.info("Cambiando contraseña para usuario: {}", currentUser.getEmail());

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("La contraseña actual es incorrecta");
        }

        // Verificar que la nueva sea diferente
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual");
        }

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Invalidar refresh token por seguridad
        user.setRefreshToken(null);

        userRepository.save(user);
        log.info("Contraseña cambiada exitosamente para: {}", user.getEmail());
    }

    // Actualizar mapeo en AuthService.java y UserService.java
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .photoUrl(user.getPhotoUrl())
                .complexId(user.getComplex() != null ? user.getComplex().getId() : null)
                .complexName(user.getComplex() != null ? user.getComplex().getName() : null) // ✅
                .build();
    }
}