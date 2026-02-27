// src/main/java/com/reservas/repository/PaymentRepository.java
package com.reservas.repository;

import com.reservas.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReservationId(Long reservationId);

    Optional<Payment> findByExternalPaymentId(String externalPaymentId);
}