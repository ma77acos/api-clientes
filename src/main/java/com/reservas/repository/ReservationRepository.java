// src/main/java/com/reservas/repository/ReservationRepository.java
package com.reservas.repository;

import com.reservas.entity.Reservation;
import com.reservas.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByUserIdOrderByDateDescTimeDesc(Long userId);

    List<Reservation> findByCourtId(Long courtId);

    List<Reservation> findByCourtIdAndDate(Long courtId, LocalDate date);

    // ✅ NUEVO: Buscar por cancha, fecha y estados específicos
    List<Reservation> findByCourtIdAndDateAndStatusIn(
            Long courtId,
            LocalDate date,
            List<ReservationStatus> statuses
    );

    Optional<Reservation> findByCourtIdAndDateAndTime(Long courtId, LocalDate date, LocalTime time);

    // ✅ ACTUALIZADO: Verificar disponibilidad solo con reservas activas
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r " +
            "WHERE r.court.id = :courtId AND r.date = :date AND r.time = :time " +
            "AND r.status IN ('CONFIRMED', 'PENDING')")
    boolean existsActiveReservation(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time
    );

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.court.complex.id = :complexId " +
            "AND r.date = :date AND r.status = :status")
    Long countByComplexIdAndDateAndStatus(@Param("complexId") Long complexId,
                                          @Param("date") LocalDate date,
                                          @Param("status") ReservationStatus status);

    // Reservas de un complejo por fecha
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.court.complex.id = :complexId AND r.date = :date " +
            "ORDER BY r.time ASC")
    List<Reservation> findByComplexIdAndDate(
            @Param("complexId") Long complexId,
            @Param("date") LocalDate date
    );

    // Reservas de un complejo en un rango de fechas
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.court.complex.id = :complexId " +
            "AND r.date BETWEEN :startDate AND :endDate " +
            "ORDER BY r.date ASC, r.time ASC")
    List<Reservation> findByComplexIdAndDateBetween(
            @Param("complexId") Long complexId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    boolean existsByCourtIdAndDateAndTime(Long courtId, LocalDate date, LocalTime time);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime expiresAt);
}