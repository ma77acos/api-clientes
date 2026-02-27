// src/main/java/com/reservas/repository/RecurringReservationRepository.java
package com.reservas.repository;

import com.reservas.entity.RecurringReservation;
import com.reservas.enums.RecurringStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringReservationRepository extends JpaRepository<RecurringReservation, Long> {

    // Buscar reservas fijas activas para una cancha, día y hora
    @Query("SELECT r FROM RecurringReservation r " +
            "WHERE r.court.id = :courtId " +
            "AND r.dayOfWeek = :dayOfWeek " +
            "AND r.time = :time " +
            "AND r.status = 'ACTIVE' " +
            "AND r.startDate <= :date " +
            "AND (r.endDate IS NULL OR r.endDate >= :date)")
    Optional<RecurringReservation> findActiveForSlot(
            @Param("courtId") Long courtId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("time") LocalTime time,
            @Param("date") LocalDate date
    );

    // Buscar todas las reservas fijas activas de un complejo
    @Query("SELECT r FROM RecurringReservation r " +
            "WHERE r.court.complex.id = :complexId " +
            "AND r.status = 'ACTIVE' " +
            "ORDER BY r.dayOfWeek, r.time")
    List<RecurringReservation> findActiveByComplexId(@Param("complexId") Long complexId);

    // Buscar reservas fijas de una cancha
    List<RecurringReservation> findByCourtIdAndStatus(Long courtId, RecurringStatus status);

    // Verificar si existe reserva fija activa
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM RecurringReservation r " +
            "WHERE r.court.id = :courtId " +
            "AND r.dayOfWeek = :dayOfWeek " +
            "AND r.time = :time " +
            "AND r.status = 'ACTIVE' " +
            "AND r.startDate <= :date " +
            "AND (r.endDate IS NULL OR r.endDate >= :date)")
    boolean existsActiveForSlot(
            @Param("courtId") Long courtId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("time") LocalTime time,
            @Param("date") LocalDate date
    );
}