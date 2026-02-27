// src/main/java/com/reservas/service/AdminReservationService.java
package com.reservas.service;

import com.reservas.dto.request.AdminReservationRequest;
import com.reservas.dto.response.ReservationResponse;
import com.reservas.entity.Court;
import com.reservas.entity.RecurringReservation;
import com.reservas.entity.Reservation;
import com.reservas.entity.User;
import com.reservas.enums.ReservationStatus;
import com.reservas.enums.Role;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.CourtRepository;
import com.reservas.repository.RecurringExceptionRepository;
import com.reservas.repository.RecurringReservationRepository;
import com.reservas.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final RecurringReservationRepository recurringRepository;
    private final RecurringExceptionRepository exceptionRepository;
    /**
     * Crear reserva como administrador del complejo
     * - Sin pago requerido
     * - Sin expiración
     * - Estado CONFIRMED directamente
     */
    @Transactional
    public ReservationResponse createAdminReservation(AdminReservationRequest request) {
        User currentUser = getCurrentUser();

        // Verificar que es BUSINESS o ADMIN
        if (currentUser.getRole() != Role.BUSINESS && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos para crear reservas administrativas");
        }

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", request.getCourtId()));

        // Verificar que el usuario administra este complejo
        if (currentUser.getRole() == Role.BUSINESS) {
            if (currentUser.getComplex() == null ||
                    !currentUser.getComplex().getId().equals(court.getComplex().getId())) {
                throw new UnauthorizedException("No administras este complejo");
            }
        }

        // Verificar disponibilidad
        if (reservationRepository.existsActiveReservation(
                request.getCourtId(), request.getDate(), request.getTime())) {
            throw new BadRequestException("El horario seleccionado ya está reservado");
        }

        // ✅ Crear reserva sin expiración y CONFIRMED directamente
        Reservation reservation = Reservation.builder()
                .court(court)
                .user(null) // No hay usuario cliente registrado
                .date(request.getDate())
                .time(request.getTime())
                .price(request.getPrice())
                .status(ReservationStatus.CONFIRMED) // ✅ Confirmada directamente
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .notes(request.getNotes())
                .createdByAdmin(true)
                .createdBy(currentUser)
                .expiresAt(null) // ✅ Sin expiración
                .build();

        reservation = reservationRepository.save(reservation);

        log.info("✅ Reserva admin #{} creada por {} para cliente: {}",
                reservation.getId(),
                currentUser.getEmail(),
                request.getCustomerName());

        return mapToReservationResponse(reservation);
    }


    /**
     * Obtener reservas del complejo que administra (incluye reservas fijas)
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getComplexReservations(LocalDate date) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.BUSINESS && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos para ver estas reservas");
        }

        Long complexId;
        if (currentUser.getRole() == Role.ADMIN) {
            throw new BadRequestException("Especifica el complejo");
        } else {
            if (currentUser.getComplex() == null) {
                throw new BadRequestException("No tienes un complejo asignado");
            }
            complexId = currentUser.getComplex().getId();
        }

        // 1. Reservas normales
        List<Reservation> reservations = reservationRepository
                .findByComplexIdAndDate(complexId, date);

        List<ReservationResponse> responses = new ArrayList<>(
                reservations.stream()
                        .map(this::mapToReservationResponse)
                        .toList()
        );

        // 2. Agregar reservas fijas para esta fecha
        List<ReservationResponse> recurringResponses =
                expandRecurringReservationsForDate(complexId, date);
        responses.addAll(recurringResponses);

        // 3. Ordenar por hora
        responses.sort(Comparator.comparing(ReservationResponse::getTime));

        return responses;
    }

    /**
     * Expande reservas fijas para una fecha específica
     */
    private List<ReservationResponse> expandRecurringReservationsForDate(Long complexId, LocalDate date) {
        List<RecurringReservation> activeRecurrings = recurringRepository
                .findActiveByComplexId(complexId);

        List<ReservationResponse> expanded = new ArrayList<>();

        for (RecurringReservation recurring : activeRecurrings) {
            // ¿Es el día de la semana correcto?
            if (date.getDayOfWeek() != recurring.getDayOfWeek()) {
                continue;
            }

            // ¿Está dentro del rango de la reserva fija?
            if (date.isBefore(recurring.getStartDate())) {
                continue;
            }

            if (recurring.getEndDate() != null && date.isAfter(recurring.getEndDate())) {
                continue;
            }

            // ¿No tiene excepción para esta fecha?
            boolean hasException = exceptionRepository
                    .existsByRecurringReservationIdAndExceptionDate(recurring.getId(), date);

            if (!hasException) {
                expanded.add(mapRecurringToResponse(recurring, date));
            }
        }

        return expanded;
    }

    private ReservationResponse mapRecurringToResponse(RecurringReservation recurring, LocalDate date) {
        return ReservationResponse.builder()
                .id(recurring.getId())
                .status(ReservationStatus.CONFIRMED)
                .courtId(recurring.getCourt().getId())
                .courtName(recurring.getCourt().getName())
                .complexName(recurring.getCourt().getComplex().getName())
                .date(date)
                .time(LocalTime.parse(recurring.getTime().format(DateTimeFormatter.ofPattern("HH:mm"))))
                .price(recurring.getPrice())
                .customerName(recurring.getCustomerName())
                .customerPhone(recurring.getCustomerPhone())
                .createdByAdmin(true)
                .isRecurring(true)
                .recurringId(recurring.getId())
                .createdAt(recurring.getCreatedAt())
                .build();
    }

    /**
     * Obtener todas las reservas del complejo (con filtros)
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getComplexReservations(
            LocalDate startDate,
            LocalDate endDate,
            ReservationStatus status) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.BUSINESS && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos");
        }

        if (currentUser.getComplex() == null) {
            throw new BadRequestException("No tienes un complejo asignado");
        }

        Long complexId = currentUser.getComplex().getId();

        List<Reservation> reservations = reservationRepository
                .findByComplexIdAndDateBetween(complexId, startDate, endDate);

        // Filtrar por status si se especifica
        if (status != null) {
            reservations = reservations.stream()
                    .filter(r -> r.getStatus() == status)
                    .collect(Collectors.toList());
        }

        return reservations.stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancelar reserva del complejo
     */
    @Transactional
    public void cancelReservation(Long reservationId) {
        User currentUser = getCurrentUser();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", reservationId));

        // Verificar que administra este complejo
        if (currentUser.getRole() == Role.BUSINESS) {
            if (currentUser.getComplex() == null ||
                    !currentUser.getComplex().getId().equals(reservation.getCourt().getComplex().getId())) {
                throw new UnauthorizedException("No administras este complejo");
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        log.info("❌ Reserva #{} cancelada por admin {}", reservationId, currentUser.getEmail());
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private ReservationResponse mapToReservationResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .status(reservation.getStatus())
                .courtId(reservation.getCourt().getId())
                .courtName(reservation.getCourt().getName())
                .complexName(reservation.getCourt().getComplex().getName())
                .date(reservation.getDate())
                .time(reservation.getTime())
                .price(reservation.getPrice())
                .createdAt(reservation.getCreatedAt())
                // Campos adicionales para admin
                .customerName(reservation.getCustomerName())
                .customerPhone(reservation.getCustomerPhone())
                .createdByAdmin(reservation.getCreatedByAdmin())
                .isRecurring(false)
                .build();
    }
}