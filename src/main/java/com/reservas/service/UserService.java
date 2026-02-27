// src/main/java/com/reservas/service/UserService.java
package com.reservas.service;

import com.reservas.dto.response.UserResponse;
import com.reservas.entity.User;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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