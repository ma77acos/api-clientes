// src/main/java/com/reservas/repository/TableSessionRepository.java
package com.reservas.repository;

import com.reservas.entity.TableSession;
import com.reservas.enums.TableSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableSessionRepository extends JpaRepository<TableSession, Long> {

    // Sesión abierta de una mesa específica
    Optional<TableSession> findByTableIdAndStatus(Long tableId, TableSessionStatus status);

    // Verificar si una mesa tiene sesión abierta
    boolean existsByTableIdAndStatus(Long tableId, TableSessionStatus status);

    // Todas las sesiones abiertas del complejo
    List<TableSession> findByComplexIdAndStatusOrderByOpenedAtAsc(
            Long complexId, TableSessionStatus status);

    // Historial de sesiones cerradas del complejo (por rango de fechas)
    @Query("SELECT ts FROM TableSession ts " +
            "WHERE ts.complex.id = :complexId " +
            "AND ts.status = :status " +
            "AND ts.closedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY ts.closedAt DESC")
    List<TableSession> findByComplexIdAndStatusAndClosedAtBetween(
            @Param("complexId") Long complexId,
            @Param("status") TableSessionStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Sesiones cerradas de hoy (para reportes)
    @Query("SELECT ts FROM TableSession ts " +
            "WHERE ts.complex.id = :complexId " +
            "AND ts.status = 'CLOSED' " +
            "AND ts.closedAt >= :startOfDay " +
            "ORDER BY ts.closedAt DESC")
    List<TableSession> findTodayClosedSessions(
            @Param("complexId") Long complexId,
            @Param("startOfDay") LocalDateTime startOfDay);
}