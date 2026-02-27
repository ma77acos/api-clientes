// src/main/java/com/reservas/service/ComplexRegistrationService.java
package com.reservas.service;

import com.reservas.dto.request.ComplexRegistrationRequest;
import com.reservas.dto.response.ComplexRegistrationResponse;
import com.reservas.entity.Complex;
import com.reservas.entity.User;
import com.reservas.enums.Role;
import com.reservas.exception.BadRequestException;
import com.reservas.repository.ComplexRepository;
import com.reservas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplexRegistrationService {

    private final ComplexRepository complexRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ComplexRegistrationResponse registerComplex(ComplexRegistrationRequest request) {
        // Verificar que el email no exista
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Ya existe una cuenta con ese email");
        }

        // Crear el complejo
        Complex complex = Complex.builder()
                .name(request.getComplexName())
                .city(request.getCity())
                .address(request.getAddress())
                .phone(request.getPhone())
                .rating(BigDecimal.ZERO)
                .build();

        complex = complexRepository.save(complex);

        // Crear el usuario BUSINESS asociado al complejo
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getOwnerName())
                .role(Role.BUSINESS)
                .complex(complex)
                .build();

        user = userRepository.save(user);

        log.info("✅ Nuevo complejo registrado: {} - Usuario: {}",
                complex.getName(), user.getEmail());

        return ComplexRegistrationResponse.builder()
                .complexId(complex.getId())
                .userId(user.getId())
                .message("Complejo registrado exitosamente")
                .build();
    }
}