// src/main/java/com/reservas/service/AuthService.java
package com.reservas.service;

import com.reservas.dto.request.*;
import com.reservas.dto.response.AuthResponse;
import com.reservas.dto.response.TokenRefreshResponse;
import com.reservas.dto.response.UserResponse;
import com.reservas.entity.Complex;
import com.reservas.entity.User;
import com.reservas.enums.Role;
import com.reservas.exception.UnauthorizedException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.repository.ComplexRepository;
import com.reservas.repository.UserRepository;
import com.reservas.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ComplexRepository complexRepository;

    // Tiempo de expiración del token de reset (30 minutos)
    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 30;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Intentando autenticar usuario: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();

        // Verificar que el usuario esté activo
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Usuario desactivado");
        }

        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // Guardar refresh token en BD con timestamp
        user.setRefreshToken(refreshToken);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Usuario autenticado exitosamente: {}", user.getEmail());

        return AuthResponse.builder()
                .user(mapToUserResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
    }

    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        log.info("Intentando refrescar token");

        // Validar que el refresh token no esté vacío
        if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            throw new UnauthorizedException("Refresh token requerido");
        }

        // Validar el refresh token
        if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Refresh token inválido o expirado");
        }

        // Buscar usuario por refresh token
        User user = userRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token no encontrado"));

        // Verificar que el usuario esté activo
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Usuario desactivado");
        }

        // Generar nuevo access token
        String newAccessToken = jwtTokenProvider.generateTokenFromUser(user);

        // Rotar el refresh token (más seguro)
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        log.info("Token refrescado exitosamente para usuario: {}", user.getEmail());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        log.info("Intentando cerrar sesión");

        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            userRepository.findByRefreshToken(refreshToken)
                    .ifPresent(user -> {
                        user.setRefreshToken(null);
                        userRepository.save(user);
                        log.info("Sesión cerrada para usuario: {}", user.getEmail());
                    });
        }
    }

    /**
     * ✅ NUEVO: Envía email con link para resetear contraseña
     */
    @Transactional
    public void sendPasswordResetEmail(ForgotPasswordRequest request) {
        log.info("Solicitud de reset de contraseña para: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un usuario con el email: " + request.getEmail()
                ));

        // Generar token de reset único
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRATION_MINUTES);

        // Guardar token en el usuario
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(expiryDate);
        userRepository.save(user);

        // Enviar email con el token
        emailService.sendPasswordResetEmail(user.getEmail(), user.getDisplayName(), resetToken);

        log.info("Email de reset enviado exitosamente a: {}", user.getEmail());
    }

    /**
     * ✅ NUEVO: Valida el token de reset de contraseña
     */
    @Transactional(readOnly = true)
    public boolean validatePasswordResetToken(String token) {
        log.info("Validando token de reset de contraseña");

        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        return userRepository.findByPasswordResetToken(token)
                .map(user -> {
                    // Verificar que el token no haya expirado
                    boolean isValid = user.getPasswordResetTokenExpiry() != null &&
                            user.getPasswordResetTokenExpiry().isAfter(LocalDateTime.now());

                    if (!isValid) {
                        log.warn("Token de reset expirado para usuario: {}", user.getEmail());
                    }

                    return isValid;
                })
                .orElse(false);
    }

    /**
     * ✅ NUEVO: Resetea la contraseña usando el token
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Intentando resetear contraseña con token: {}", request.getToken());

        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new UnauthorizedException("Token de reset inválido"));

        log.info("Usuario encontrado: {}", user.getEmail());

        // Verificar que el token no haya expirado
        if (user.getPasswordResetTokenExpiry() == null ||
                user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Token de reset expirado");
        }

        // ✅ DEBUG: Verificar que passwordEncoder está inyectado
        log.info("Password encoder: {}", passwordEncoder.getClass().getName());

        // ✅ DEBUG: Log de la contraseña antes y después
        log.info("Password actual (hash): {}", user.getPassword().substring(0, 20) + "...");

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        log.info("Nuevo password (hash): {}", newPasswordHash.substring(0, 20) + "...");

        // Actualizar contraseña
        user.setPassword(newPasswordHash);

        // Limpiar tokens
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setRefreshToken(null);

        // ✅ Guardar explícitamente
        User savedUser = userRepository.save(user);

        // ✅ DEBUG: Verificar que se guardó
        log.info("Usuario guardado. Password actualizado: {}",
                savedUser.getPassword().substring(0, 20) + "...");

        // Verificar en BD
        User verifyUser = userRepository.findById(user.getId()).orElse(null);
        if (verifyUser != null) {
            log.info("Verificación BD - Password: {}",
                    verifyUser.getPassword().substring(0, 20) + "...");
        }

        log.info("✅ Contraseña reseteada exitosamente para usuario: {}", user.getEmail());
    }

    /**
     * ✅ NUEVO: Registro de usuarios
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Intentando registrar nuevo usuario: {}", request.getEmail());

        // 1. Verificar que el email no esté registrado
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // 2. Crear nuevo usuario
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .role(Role.PLAYER) // Por defecto es PLAYER
                .build();

        // 3. Si se proporciona complexId, verificar que exista y asociar
        if (request.getComplexId() != null) {
            Complex complex = complexRepository.findById(request.getComplexId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Complejo no encontrado con ID: " + request.getComplexId()
                    ));

            user.setComplex(complex);
            user.setRole(Role.BUSINESS); // Si tiene complejo, es BUSINESS
        }

        // 4. Guardar usuario
        User savedUser = userRepository.save(user);

        // 5. Generar tokens
        String accessToken = jwtTokenProvider.generateTokenFromUser(savedUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // 6. Guardar refresh token
        savedUser.setRefreshToken(refreshToken);
        savedUser.setLastLogin(LocalDateTime.now());
        userRepository.save(savedUser);

        log.info("✅ Usuario registrado exitosamente: {}", savedUser.getEmail());

        // 7. Opcional: Enviar email de bienvenida
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getDisplayName());
        } catch (Exception e) {
            log.warn("No se pudo enviar email de bienvenida: {}", e.getMessage());
        }

        // 8. Retornar respuesta igual que login
        return AuthResponse.builder()
                .user(mapToUserResponse(savedUser))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .photoUrl(user.getPhotoUrl())
                .complexId(user.getComplex() != null ? user.getComplex().getId() : null)
                .complexName(user.getComplex() != null ? user.getComplex().getName() : null)
                .build();
    }
}