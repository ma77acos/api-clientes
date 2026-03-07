// src/main/java/com/reservas/repository/CashRegisterRepository.java
package com.reservas.repository;

import com.reservas.entity.CashRegister;
import com.reservas.enums.CashRegisterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {

    Optional<CashRegister> findByComplexIdAndDate(Long complexId, LocalDate date);

    Optional<CashRegister> findByComplexIdAndStatus(Long complexId, CashRegisterStatus status);

    @Query("SELECT cr FROM CashRegister cr WHERE cr.complex.id = :complexId " +
            "ORDER BY cr.date DESC")
    List<CashRegister> findByComplexIdOrderByDateDesc(@Param("complexId") Long complexId);

    @Query("SELECT cr FROM CashRegister cr WHERE cr.complex.id = :complexId " +
            "AND cr.date BETWEEN :startDate AND :endDate ORDER BY cr.date DESC")
    List<CashRegister> findByComplexIdAndDateBetween(
            @Param("complexId") Long complexId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT cr FROM CashRegister cr WHERE cr.complex.id = :complexId " +
            "AND cr.date < :date AND cr.status = 'CLOSED' ORDER BY cr.date DESC LIMIT 1")
    Optional<CashRegister> findLastClosedBefore(
            @Param("complexId") Long complexId,
            @Param("date") LocalDate date);

    boolean existsByComplexIdAndDate(Long complexId, LocalDate date);
}