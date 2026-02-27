// src/main/java/com/reservas/service/AuthService.java
package com.reservas.service;

import com.reservas.dto.request.LoginRequest;
import com.reservas.dto.request.RefreshTokenRequest;
import com.reservas.dto.response.AuthResponse;
import com.reservas.dto.response.TokenRefreshResponse;
import com.reservas.dto.response.UserResponse;
import com.reservas.entity.User;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.UserRepository;
import com.reservas.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // Guardar refresh token en BD
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return AuthResponse.builder()
                .user(mapToUserResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
    }

    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        User user = userRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        String newAccessToken = jwtTokenProvider.generateToken(user.getEmail());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
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