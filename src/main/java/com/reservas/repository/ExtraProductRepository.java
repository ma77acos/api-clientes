package com.reservas.repository;

import com.reservas.entity.ExtraProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExtraProductRepository extends JpaRepository<ExtraProduct, Long> {

    // Para reservas normales
    List<ExtraProduct> findByReservationId(Long reservationId);

    // Para reservas recurrentes (por fecha específica)
    List<ExtraProduct> findByRecurringReservationIdAndProductDate(Long recurringId, LocalDate productDate);
}