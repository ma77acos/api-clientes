// src/main/java/com/reservas/service/AdminCourtService.java
package com.reservas.service;

import com.reservas.dto.request.CourtRequest;
import com.reservas.dto.response.CourtResponse;
import com.reservas.entity.Court;
import com.reservas.entity.RecurringReservation;
import com.reservas.entity.User;
import com.reservas.enums.RecurringStatus;
import com.reservas.enums.Role;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.CourtRepository;
import com.reservas.repository.RecurringReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCourtService {

    private final CourtRepository courtRepository;
    private final RecurringReservationRepository recurringRepository;

    @Transactional(readOnly = true)
    public List<CourtResponse> getComplexCourts() {
        User currentUser = getCurrentUser();
        validateAccess(currentUser);

        List<Court> courts = courtRepository.findByComplexId(currentUser.getComplex().getId());

        return courts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CourtResponse createCourt(CourtRequest request) {
        User currentUser = getCurrentUser();
        validateAccess(currentUser);

        Court court = Court.builder()
                .name(request.getName())
                .covered(request.getCovered())
                .surface(request.getSurface())
                .price(request.getPrice())
                .complex(currentUser.getComplex())
                .build();

        court = courtRepository.save(court);

        return mapToResponse(court);
    }

    @Transactional
    public CourtResponse updateCourt(Long id, CourtRequest request) {
        User currentUser = getCurrentUser();
        validateAccess(currentUser);

        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", id));

        if (!court.getComplex().getId().equals(currentUser.getComplex().getId())) {
            throw new UnauthorizedException("No tienes permiso para editar esta cancha");
        }

        // Guardar precio anterior para comparar
        BigDecimal oldPrice = court.getPrice();
        BigDecimal newPrice = request.getPrice();

        // Actualizar la cancha
        court.setName(request.getName());
        court.setCovered(request.getCovered());
        court.setSurface(request.getSurface());
        court.setPrice(newPrice);

        court = courtRepository.save(court);

        // ✅ Si cambió el precio, actualizar reservas fijas activas
        if (oldPrice.compareTo(newPrice) != 0) {
            updateRecurringReservationsPrice(court.getId(), newPrice);
        }

        return mapToResponse(court);
    }

    /**
     * Actualiza el precio de las reservas fijas activas de una cancha
     */
    private void updateRecurringReservationsPrice(Long courtId, BigDecimal newPrice) {
        List<RecurringReservation> activeRecurrings = recurringRepository
                .findByCourtIdAndStatus(courtId, RecurringStatus.ACTIVE);

        for (RecurringReservation recurring : activeRecurrings) {
            recurring.setPrice(newPrice);
        }

        recurringRepository.saveAll(activeRecurrings);
    }

    @Transactional
    public void deleteCourt(Long id) {
        User currentUser = getCurrentUser();
        validateAccess(currentUser);

        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", id));

        if (!court.getComplex().getId().equals(currentUser.getComplex().getId())) {
            throw new UnauthorizedException("No tienes permiso para eliminar esta cancha");
        }

        courtRepository.delete(court);
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

    private CourtResponse mapToResponse(Court court) {
        return CourtResponse.builder()
                .id(court.getId())
                .name(court.getName())
                .covered(court.getCovered())
                .surface(court.getSurface())
                .price(court.getPrice())
                .build();
    }
}