// src/main/java/com/reservas/service/ReservationService.java
package com.reservas.service;

import com.reservas.dto.request.RecurringReservationRequest;
import com.reservas.dto.request.ReservationRequest;
import com.reservas.dto.response.MyReservationResponse;
import com.reservas.dto.response.ReservationResponse;
import com.reservas.entity.Court;
import com.reservas.entity.Reservation;
import com.reservas.entity.User;
import com.reservas.enums.RecurrenceType;
import com.reservas.enums.ReservationStatus;
import com.reservas.enums.Role;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.repository.CourtRepository;
import com.reservas.repository.ReservationRepository;
import com.reservas.repository.UserRepository;
import com.reservas.enums.RecurrenceType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<ReservationResponse> createRecurringReservation(RecurringReservationRequest request) {
        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", request.getCourtId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUserId()));

        List<Reservation> reservations = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        int maxOccurrences = request.getOccurrences() != null ? request.getOccurrences() : 52; // 1 año por defecto

        for (int i = 0; i < maxOccurrences; i++) {
            // Verificar disponibilidad
            if (!reservationRepository.existsByCourtIdAndDateAndTime(
                    request.getCourtId(), currentDate, request.getTime())) {

                Reservation reservation = Reservation.builder()
                        .court(court)
                        .user(user)
                        .date(currentDate)
                        .time(request.getTime())
                        .price(request.getPrice())
                        .status(ReservationStatus.CONFIRMED)
                        .build();

                reservations.add(reservation);
            }

            // Calcular siguiente fecha según tipo de recurrencia
            switch (request.getRecurrenceType()) {
                case DAILY:
                    currentDate = currentDate.plusDays(1);
                    break;
                case WEEKLY:
                    currentDate = currentDate.plusWeeks(1);
                    break;
                case MONTHLY:
                    currentDate = currentDate.plusMonths(1);
                    break;
            }
        }

        List<Reservation> savedReservations = reservationRepository.saveAll(reservations);

        return savedReservations.stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MyReservationResponse> getMyReservations() {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        List<Reservation> reservations = reservationRepository
                .findByUserIdOrderByDateDescTimeDesc(currentUser.getId());

        return reservations.stream()
                .map(this::mapToMyReservationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));
        return mapToReservationResponse(reservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));

        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        boolean canCancel = false;

        switch (currentUser.getRole()) {
            case PLAYER:
                // PLAYER solo puede cancelar sus propias reservas
                if (reservation.getUser() != null &&
                        reservation.getUser().getId().equals(currentUser.getId())) {
                    canCancel = true;
                }
                break;

            case BUSINESS:
                // BUSINESS puede cancelar cualquier reserva de su complejo
                if (currentUser.getComplex() != null) {
                    Long businessComplexId = currentUser.getComplex().getId();
                    Long reservationComplexId = reservation.getCourt().getComplex().getId();

                    if (businessComplexId.equals(reservationComplexId)) {
                        canCancel = true;
                    }
                }
                break;

            case ADMIN:
                // ADMIN puede cancelar cualquier reserva
                canCancel = true;
                break;
        }

        if (!canCancel) {
            throw new BadRequestException("No tienes permiso para cancelar esta reserva");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
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
                .customerName(reservation.getCustomerName())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    private MyReservationResponse mapToMyReservationResponse(Reservation reservation) {
        return MyReservationResponse.builder()
                .id(reservation.getId())
                .complexName(reservation.getCourt().getComplex().getName())
                .courtName(reservation.getCourt().getName())
                .date(reservation.getDate())
                .time(reservation.getTime())
                .status(reservation.getStatus())
                .build();
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        // ✅ Verificar disponibilidad solo con reservas ACTIVAS
        if (reservationRepository.existsActiveReservation(
                request.getCourtId(), request.getDate(), request.getTime())) {
            throw new BadRequestException("El horario seleccionado ya está reservado");
        }

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", request.getCourtId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUserId()));

        // ⏱️ Configurar expiración de 5 minutos
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        Reservation reservation = Reservation.builder()
                .court(court)
                .user(user)
                .date(request.getDate())
                .time(request.getTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        reservation = reservationRepository.save(reservation);

        return mapToReservationResponse(reservation);
    }
}