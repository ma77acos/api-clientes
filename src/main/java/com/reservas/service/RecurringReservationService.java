// src/main/java/com/reservas/service/RecurringReservationService.java
package com.reservas.service;

import com.reservas.dto.request.RecurringExceptionRequest;
import com.reservas.dto.request.RecurringReservationRequest;
import com.reservas.dto.response.RecurringReservationResponse;
import com.reservas.dto.response.ReservationResponse;
import com.reservas.entity.*;
import com.reservas.enums.RecurringStatus;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringReservationService {

    private final RecurringReservationRepository recurringRepository;
    private final RecurringExceptionRepository exceptionRepository;
    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Crear reserva fija (mensual/semanal)
     */
    @Transactional
    public RecurringReservationResponse createRecurringReservation(RecurringReservationRequest request) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", request.getCourtId()));

        validateComplexAccess(currentUser, court);

        // 1️⃣ Verificar que no exista otra reserva FIJA para ese slot
        if (recurringRepository.existsActiveForSlot(
                request.getCourtId(),
                request.getDayOfWeek(),
                request.getTime(),
                request.getStartDate())) {
            throw new BadRequestException("Ya existe una reserva fija para ese horario");
        }

        // 2️⃣ Verificar que no exista una reserva NORMAL activa para la primera fecha
        LocalDate firstOccurrence = findFirstOccurrence(request.getStartDate(), request.getDayOfWeek());
        if (reservationRepository.existsActiveReservation(
                request.getCourtId(),
                firstOccurrence,
                request.getTime())) {
            throw new BadRequestException("Ya existe una reserva para el " + firstOccurrence + " a las " + request.getTime());
        }

        RecurringReservation recurring = RecurringReservation.builder()
                .court(court)
                .dayOfWeek(request.getDayOfWeek())
                .time(request.getTime())
                .price(request.getPrice())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .notes(request.getNotes())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(RecurringStatus.ACTIVE)
                .createdBy(currentUser)
                .build();

        recurring = recurringRepository.save(recurring);

        log.info("✅ Reserva fija #{} creada: {} {} a las {} por {}",
                recurring.getId(),
                getDayName(recurring.getDayOfWeek()),
                recurring.getTime(),
                recurring.getCustomerName(),
                currentUser.getEmail());

        return mapToResponse(recurring);
    }

    /**
     * Encontrar la primera ocurrencia del día de la semana desde una fecha
     */
    private LocalDate findFirstOccurrence(LocalDate startDate, DayOfWeek targetDay) {
        LocalDate date = startDate;
        while (date.getDayOfWeek() != targetDay) {
            date = date.plusDays(1);
        }
        return date;
    }

    /**
     * Cancelar un día específico (excepción esporádica)
     */
    @Transactional
    public void cancelSpecificDate(RecurringExceptionRequest request) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(request.getRecurringReservationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva fija", "id", request.getRecurringReservationId()));

        validateComplexAccess(currentUser, recurring.getCourt());

        // Verificar que la fecha corresponde al día de la semana correcto
        if (request.getExceptionDate().getDayOfWeek() != recurring.getDayOfWeek()) {
            throw new BadRequestException("La fecha no corresponde al día de la reserva fija");
        }

        // Verificar que no exista ya una excepción para esa fecha
        if (exceptionRepository.existsByRecurringReservationIdAndExceptionDate(
                recurring.getId(), request.getExceptionDate())) {
            throw new BadRequestException("Ya existe una excepción para esa fecha");
        }

        RecurringException exception = RecurringException.builder()
                .recurringReservation(recurring)
                .exceptionDate(request.getExceptionDate())
                .reason(request.getReason())
                .cancelledBy(currentUser)
                .build();

        exceptionRepository.save(exception);

        log.info("📅 Excepción creada para reserva fija #{}: fecha {} - {}",
                recurring.getId(),
                request.getExceptionDate(),
                request.getReason());
    }

    /**
     * Restaurar un día cancelado (eliminar excepción)
     */
    @Transactional
    public void restoreSpecificDate(Long recurringId, LocalDate date) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva fija", "id", recurringId));

        validateComplexAccess(currentUser, recurring.getCourt());

        exceptionRepository.findByRecurringReservationIdAndExceptionDate(recurringId, date)
                .ifPresent(exception -> {
                    exceptionRepository.delete(exception);
                    log.info("✅ Excepción eliminada para reserva fija #{}: fecha {}",
                            recurringId, date);
                });
    }

    /**
     * Cancelar permanentemente la reserva fija
     */
    @Transactional
    public void cancelPermanently(Long recurringId) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva fija", "id", recurringId));

        validateComplexAccess(currentUser, recurring.getCourt());

        recurring.setStatus(RecurringStatus.CANCELLED);
        recurringRepository.save(recurring);

        log.info("❌ Reserva fija #{} cancelada permanentemente por {}",
                recurringId, currentUser.getEmail());
    }

    /**
     * Reactivar una reserva fija cancelada
     */
    @Transactional
    public void reactivate(Long recurringId) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva fija", "id", recurringId));

        validateComplexAccess(currentUser, recurring.getCourt());

        recurring.setStatus(RecurringStatus.ACTIVE);
        recurringRepository.save(recurring);

        log.info("✅ Reserva fija #{} reactivada por {}", recurringId, currentUser.getEmail());
    }

    /**
     * Obtener todas las reservas fijas del complejo
     */
    @Transactional(readOnly = true)
    public List<RecurringReservationResponse> getComplexRecurringReservations() {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        if (currentUser.getComplex() == null) {
            throw new BadRequestException("No tienes un complejo asignado");
        }

        List<RecurringReservation> reservations = recurringRepository
                .findActiveByComplexId(currentUser.getComplex().getId());

        return reservations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Verificar si un slot está ocupado por una reserva fija
     */
    @Transactional(readOnly = true)
    public boolean isSlotOccupiedByRecurring(Long courtId, LocalDate date, LocalTime time) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // Buscar reserva fija activa
        var recurringOpt = recurringRepository.findActiveForSlot(courtId, dayOfWeek, time, date);

        if (recurringOpt.isEmpty()) {
            return false;
        }

        RecurringReservation recurring = recurringOpt.get();

        // Verificar si hay una excepción para esta fecha
        boolean hasException = exceptionRepository.existsByRecurringReservationIdAndExceptionDate(
                recurring.getId(), date);

        return !hasException;
    }

    /**
     * Obtener info de reserva fija para un slot específico
     */
    @Transactional(readOnly = true)
    public RecurringReservationResponse getRecurringForSlot(Long courtId, LocalDate date, LocalTime time) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        var recurringOpt = recurringRepository.findActiveForSlot(courtId, dayOfWeek, time, date);

        if (recurringOpt.isEmpty()) {
            return null;
        }

        RecurringReservation recurring = recurringOpt.get();

        // Verificar si hay una excepción para esta fecha
        boolean hasException = exceptionRepository.existsByRecurringReservationIdAndExceptionDate(
                recurring.getId(), date);

        if (hasException) {
            return null;
        }

        return mapToResponse(recurring);
    }

    // ========== HELPERS ==========

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private void validateAdminAccess(User user) {
        if (user.getRole() != Role.BUSINESS && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos para esta acción");
        }
    }

    private void validateComplexAccess(User user, Court court) {
        if (user.getRole() == Role.BUSINESS) {
            if (user.getComplex() == null ||
                    !user.getComplex().getId().equals(court.getComplex().getId())) {
                throw new UnauthorizedException("No administras este complejo");
            }
        }
    }

    private String getDayName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
    }


    private RecurringReservationResponse mapToResponse(RecurringReservation recurring) {
        return RecurringReservationResponse.builder()
                .id(recurring.getId())
                .courtId(recurring.getCourt().getId())
                .courtName(recurring.getCourt().getName())
                .complexName(recurring.getCourt().getComplex().getName())
                .dayOfWeek(recurring.getDayOfWeek())
                .dayOfWeekDisplay(getDayName(recurring.getDayOfWeek()))
                .time(recurring.getTime())
                .price(recurring.getPrice())
                .customerName(recurring.getCustomerName())
                .customerPhone(recurring.getCustomerPhone())
                .customerEmail(recurring.getCustomerEmail())
                .notes(recurring.getNotes())
                .startDate(recurring.getStartDate())
                .endDate(recurring.getEndDate())
                .status(recurring.getStatus())
                .createdAt(recurring.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public RecurringReservationResponse getRecurringReservationById(Long id) {
        RecurringReservation reservation = recurringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva Recurrente", "id", id));
        return mapToResponse(reservation);
    }

}