// src/main/java/com/reservas/repository/RecurringExceptionRepository.java
package com.reservas.repository;

import com.reservas.entity.RecurringException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringExceptionRepository extends JpaRepository<RecurringException, Long> {

    // Verificar si existe una excepción para una fecha
    boolean existsByRecurringReservationIdAndExceptionDate(
            Long recurringReservationId,
            LocalDate exceptionDate
    );

    // Obtener excepción específica
    Optional<RecurringException> findByRecurringReservationIdAndExceptionDate(
            Long recurringReservationId,
            LocalDate exceptionDate
    );

    // Obtener todas las excepciones de una reserva recurrente
    List<RecurringException> findByRecurringReservationIdOrderByExceptionDateDesc(
            Long recurringReservationId
    );

    // Obtener excepciones en un rango de fechas
    @Query("SELECT e FROM RecurringException e " +
            "WHERE e.recurringReservation.id = :recurringId " +
            "AND e.exceptionDate BETWEEN :startDate AND :endDate")
    List<RecurringException> findByRecurringIdAndDateRange(
            @Param("recurringId") Long recurringId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}