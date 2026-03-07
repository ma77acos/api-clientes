// src/main/java/com/reservas/repository/CashMovementRepository.java
package com.reservas.repository;

import com.reservas.entity.CashMovement;
import com.reservas.enums.MovementCategory;
import com.reservas.enums.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    List<CashMovement> findByCashRegisterIdOrderByCreatedAtDesc(Long cashRegisterId);

    List<CashMovement> findByCashRegisterIdAndType(Long cashRegisterId, MovementType type);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM CashMovement m " +
            "WHERE m.cashRegister.id = :cashRegisterId AND m.type IN ('INCOME', 'DEPOSIT')")
    BigDecimal sumIncomesByCashRegisterId(@Param("cashRegisterId") Long cashRegisterId);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM CashMovement m " +
            "WHERE m.cashRegister.id = :cashRegisterId AND m.type IN ('EXPENSE', 'WITHDRAWAL')")
    BigDecimal sumExpensesByCashRegisterId(@Param("cashRegisterId") Long cashRegisterId);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM CashMovement m " +
            "WHERE m.cashRegister.id = :cashRegisterId AND m.category = :category")
    BigDecimal sumByCategory(
            @Param("cashRegisterId") Long cashRegisterId,
            @Param("category") MovementCategory category);

    // 🔧 Buscar por PlayerPayment
    Optional<CashMovement> findByPlayerPaymentId(Long playerPaymentId);

    // 🔧 Buscar por ExtraProduct
    Optional<CashMovement> findByExtraProductId(Long extraProductId);

    // Verificar existencia
    boolean existsByPlayerPaymentId(Long playerPaymentId);

    boolean existsByExtraProductId(Long extraProductId);
}