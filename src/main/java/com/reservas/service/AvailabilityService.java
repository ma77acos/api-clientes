// src/main/java/com/reservas/service/AvailabilityService.java
package com.reservas.service;

import com.reservas.dto.response.AvailabilityResponse;
import com.reservas.dto.response.SlotResponse;
import com.reservas.entity.AvailabilitySlot;
import com.reservas.entity.Court;
import com.reservas.entity.Reservation;
import com.reservas.enums.ReservationStatus;
import com.reservas.enums.SlotStatus;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.repository.AvailabilitySlotRepository;
import com.reservas.repository.CourtRepository;
import com.reservas.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final RecurringReservationService recurringService;
    private final ComplexConfigurationService configService;

    // 🕐 Horarios desde las 14:00 hasta las 00:00 (cada 1.5 horas)
    private static final List<LocalTime> DEFAULT_SLOTS = List.of(
            LocalTime.of(14, 0),  // 14:00
            LocalTime.of(15, 30), // 15:30
            LocalTime.of(17, 0),  // 17:00
            LocalTime.of(18, 30), // 18:30
            LocalTime.of(20, 0),  // 20:00
            LocalTime.of(21, 30), // 21:30
            LocalTime.of(23, 0)   // 23:00
    );

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(Long courtId, LocalDate date) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", courtId));

        // ✅ Obtener slots según configuración del complejo
        List<LocalTime> slots = configService.getAvailableSlotTimes(court.getComplex().getId());

        // Slots bloqueados manualmente
        List<AvailabilitySlot> blockedSlots = availabilitySlotRepository
                .findByCourtIdAndDateAndStatus(courtId, date, SlotStatus.BLOCKED);
        Set<LocalTime> blockedTimes = blockedSlots.stream()
                .map(AvailabilitySlot::getTime)
                .collect(Collectors.toSet());

        // ✅ Reservas activas (incluyendo PAYED)
        List<Reservation> activeReservations = reservationRepository
                .findByCourtIdAndDateAndStatusIn(
                        courtId, date,
                        List.of(
                                ReservationStatus.CONFIRMED,
                                ReservationStatus.PENDING,
                                ReservationStatus.PAYED  // ← Agregar esto
                        )
                );
        Set<LocalTime> bookedTimes = activeReservations.stream()
                .map(Reservation::getTime)
                .collect(Collectors.toSet());

        // Construir slots
        List<SlotResponse> slotResponses = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (LocalTime time : slots) {
            SlotStatus status;

            if (blockedTimes.contains(time)) {
                status = SlotStatus.BLOCKED;
            } else if (bookedTimes.contains(time)) {
                status = SlotStatus.BOOKED;
            } else if (recurringService.isSlotOccupiedByRecurring(courtId, date, time)) {
                status = SlotStatus.BOOKED;
            } else {
                status = SlotStatus.FREE;
            }

            slotResponses.add(SlotResponse.builder()
                    .time(time.format(timeFormatter))
                    .status(status)
                    .build());
        }

        return AvailabilityResponse.builder()
                .courtId(courtId)
                .date(date)
                .slots(slotResponses)
                .build();
    }

    @Transactional
    public void blockSlot(Long courtId, LocalDate date, LocalTime time) {
        AvailabilitySlot slot = availabilitySlotRepository
                .findByCourtIdAndDateAndTime(courtId, date, time)
                .orElse(AvailabilitySlot.builder()
                        .court(courtRepository.findById(courtId)
                                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", courtId)))
                        .date(date)
                        .time(time)
                        .build());

        slot.setStatus(SlotStatus.BLOCKED);
        availabilitySlotRepository.save(slot);
    }

    @Transactional
    public void unblockSlot(Long courtId, LocalDate date, LocalTime time) {
        availabilitySlotRepository.findByCourtIdAndDateAndTime(courtId, date, time)
                .ifPresent(slot -> {
                    slot.setStatus(SlotStatus.FREE);
                    availabilitySlotRepository.save(slot);
                });
    }
}