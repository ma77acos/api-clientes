// src/main/java/com/reservas/entity/CashRegister.java
package com.reservas.entity;

import com.reservas.enums.CashRegisterStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cash_registers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"complex_id", "date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private Complex complex;

    @Column(nullable = false)
    private LocalDate date;

    // ========== APERTURA ==========

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal openingAmount;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_user_id")
    private User openedBy;

    // ========== CIERRE ==========

    @Column(precision = 10, scale = 2)
    private BigDecimal expectedAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal difference;

    @Column(precision = 10, scale = 2)
    private BigDecimal carryOverAmount;

    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id")
    private User closedBy;

    @Column(length = 500)
    private String closingNotes;

    // ========== ESTADO ==========

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CashRegisterStatus status = CashRegisterStatus.OPEN;

    // ========== RELACIONES ==========

    @OneToMany(mappedBy = "cashRegister", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CashMovement> movements = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    // ========== MÉTODOS CALCULADOS ==========

    public BigDecimal calculateTotalIncome() {
        return movements.stream()
                .filter(m -> m.getType() == com.reservas.enums.MovementType.INCOME ||
                        m.getType() == com.reservas.enums.MovementType.DEPOSIT)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalExpense() {
        return movements.stream()
                .filter(m -> m.getType() == com.reservas.enums.MovementType.EXPENSE ||
                        m.getType() == com.reservas.enums.MovementType.WITHDRAWAL)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateExpectedAmount() {
        return openingAmount
                .add(calculateTotalIncome())
                .subtract(calculateTotalExpense());
    }
}