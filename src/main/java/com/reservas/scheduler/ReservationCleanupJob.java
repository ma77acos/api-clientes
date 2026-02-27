// src/main/java/com/reservas/scheduler/ReservationCleanupJob.java
package com.reservas.scheduler;

import com.reservas.entity.Reservation;
import com.reservas.enums.ReservationStatus;
import com.reservas.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupJob {

    private final ReservationRepository reservationRepository;

    // Ejecutar cada minuto
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now);

        if (!expiredReservations.isEmpty()) {
            log.info("Cancelando {} reservas expiradas", expiredReservations.size());

            expiredReservations.forEach(reservation -> {
                reservation.setStatus(ReservationStatus.CANCELLED);
                log.info("Reserva {} cancelada por expiración", reservation.getId());
            });

            reservationRepository.saveAll(expiredReservations);
        }
    }
}