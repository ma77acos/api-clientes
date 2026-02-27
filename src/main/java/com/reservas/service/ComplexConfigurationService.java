// src/main/java/com/reservas/service/ComplexConfigurationService.java
package com.reservas.service;

import com.reservas.dto.request.ComplexConfigurationRequest;
import com.reservas.dto.response.ComplexConfigurationResponse;
import com.reservas.entity.Complex;
import com.reservas.entity.ComplexConfiguration;
import com.reservas.entity.User;
import com.reservas.enums.Role;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.ComplexConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplexConfigurationService {

    private final ComplexConfigurationRepository configRepository;

    /**
     * Obtener configuración del complejo actual
     */
    @Transactional(readOnly = true)
    public ComplexConfigurationResponse getConfiguration() {
        User currentUser = getCurrentUser();
        validateAccess(currentUser);

        ComplexConfiguration config = configRepository
                .findByComplexId(currentUser.getComplex().getId())
                .orElse(getDefaultConfiguration(currentUser.getComplex()));

        return mapToResponse(config);
    }

    /**
     * Guardar configuración
     */
    @Transactional
    public ComplexConfigurationResponse saveConfiguration(ComplexConfigurationRequest request) {
        User currentUser = getCurrentUser();
        validateAccess(currentUser);

        // Validar que hora inicio < hora fin
        if (request.getSlotStartTime().isAfter(request.getSlotEndTime())) {
            throw new BadRequestException("La hora de inicio debe ser anterior a la hora de fin");
        }

        ComplexConfiguration config = configRepository
                .findByComplexId(currentUser.getComplex().getId())
                .orElse(ComplexConfiguration.builder()
                        .complex(currentUser.getComplex())
                        .build());

        config.setSlotStartTime(request.getSlotStartTime());
        config.setSlotEndTime(request.getSlotEndTime());
        config.setSlotDurationMinutes(request.getSlotDurationMinutes());

        if (request.getDaysAdvanceBooking() != null) {
            config.setDaysAdvanceBooking(request.getDaysAdvanceBooking());
        }
        if (request.getCancellationHours() != null) {
            config.setCancellationHours(request.getCancellationHours());
        }

        config = configRepository.save(config);

        return mapToResponse(config);
    }

    public List<LocalTime> getAvailableSlotTimes() {
        User currentUser = getCurrentUser();

        if (currentUser.getComplex() == null) {
            return getDefaultSlots();
        }

        return getAvailableSlotTimes(currentUser.getComplex().getId());
    }

    /**
     * Generar lista de slots según configuración
     */
    @Transactional(readOnly = true)
    public List<LocalTime> getAvailableSlotTimes(Long complexId) {
        ComplexConfiguration config = configRepository
                .findByComplexId(complexId)
                .orElse(null);

        if (config == null) {
            // Slots por defecto
            return getDefaultSlots();
        }

        List<LocalTime> slots = new ArrayList<>();
        LocalTime current = config.getSlotStartTime();

        while (current.plusMinutes(config.getSlotDurationMinutes()).compareTo(config.getSlotEndTime()) <= 0
                || current.isBefore(config.getSlotEndTime())) {
            slots.add(current);
            current = current.plusMinutes(config.getSlotDurationMinutes());

            // Evitar loop infinito si pasa medianoche
            if (current.isBefore(config.getSlotStartTime())) {
                break;
            }
        }

        return slots;
    }

    /**
     * Slots por defecto (cuando no hay configuración)
     */
    private List<LocalTime> getDefaultSlots() {
        return List.of(
                LocalTime.of(14, 0),
                LocalTime.of(15, 30),
                LocalTime.of(17, 0),
                LocalTime.of(18, 30),
                LocalTime.of(20, 0),
                LocalTime.of(21, 30),
                LocalTime.of(23, 0)
        );
    }

    private ComplexConfiguration getDefaultConfiguration(Complex complex) {
        return ComplexConfiguration.builder()
                .complex(complex)
                .slotStartTime(LocalTime.of(14, 0))
                .slotEndTime(LocalTime.of(23, 0))
                .slotDurationMinutes(90)
                .daysAdvanceBooking(7)
                .cancellationHours(24)
                .build();
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private void validateAccess(User user) {
        if (user.getRole() != Role.BUSINESS && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos");
        }
        if (user.getComplex() == null) {
            throw new BadRequestException("No tienes un complejo asignado");
        }
    }

    private ComplexConfigurationResponse mapToResponse(ComplexConfiguration config) {
        return ComplexConfigurationResponse.builder()
                .id(config.getId())
                .complexId(config.getComplex().getId())
                .slotStartTime(config.getSlotStartTime())
                .slotEndTime(config.getSlotEndTime())
                .slotDurationMinutes(config.getSlotDurationMinutes())
                .daysAdvanceBooking(config.getDaysAdvanceBooking())
                .cancellationHours(config.getCancellationHours())
                .build();
    }
}