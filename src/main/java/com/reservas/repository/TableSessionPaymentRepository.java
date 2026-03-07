// src/main/java/com/reservas/repository/TableSessionPaymentRepository.java
package com.reservas.repository;

import com.reservas.entity.TableSessionPayment;
import com.reservas.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TableSessionPaymentRepository extends JpaRepository<TableSessionPayment, Long> {

    // Pagos de una sesión
    List<TableSessionPayment> findBySessionIdOrderByPaidAtAsc(Long sessionId);

    // Total pagado en una sesión
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM TableSessionPayment p " +
            "WHERE p.session.id = :sessionId")
    BigDecimal sumAmountBySessionId(@Param("sessionId") Long sessionId);

    // Pagos en efectivo del complejo en un rango (para caja)
    @Query("SELECT p FROM TableSessionPayment p " +
            "WHERE p.session.complex.id = :complexId " +
            "AND p.method = :method " +
            "AND p.paidAt BETWEEN :startDate AND :endDate")
    List<TableSessionPayment> findByComplexIdAndMethodAndPaidAtBetween(
            @Param("complexId") Long complexId,
            @Param("method") PaymentMethod method,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}