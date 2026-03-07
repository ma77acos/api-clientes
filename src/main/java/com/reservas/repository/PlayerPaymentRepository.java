package com.reservas.repository;

import com.reservas.entity.PlayerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlayerPaymentRepository extends JpaRepository<PlayerPayment, Long> {

    // Para reservas normales
    List<PlayerPayment> findByReservationId(Long reservationId);
    int countByReservationId(Long reservationId);

    // Para reservas recurrentes (por fecha específica)
    List<PlayerPayment> findByRecurringReservationIdAndPaymentDate(Long recurringId, LocalDate paymentDate);
    int countByRecurringReservationIdAndPaymentDate(Long recurringId, LocalDate paymentDate);
}