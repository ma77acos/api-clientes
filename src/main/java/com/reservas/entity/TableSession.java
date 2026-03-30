// src/main/java/com/reservas/entity/TableSession.java
package com.reservas.entity;

import com.reservas.enums.TableSessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "table_sessions", indexes = {
        @Index(name = "idx_session_table", columnList = "table_id"),
        @Index(name = "idx_session_complex", columnList = "complex_id"),
        @Index(name = "idx_session_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private Complex complex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    @Column(name = "customer_name", length = 100)
    private String customerName; // Nombre opcional para identificar

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TableSessionStatus status = TableSessionStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_user_id")
    private User openedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id")
    private User closedBy;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(length = 500)
    private String notes;

    // ========== RELACIONES ==========

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TableSessionItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TableSessionPayment> payments = new ArrayList<>();

    // ========== MÉTODOS CALCULADOS ==========

    public BigDecimal getTotal() {
        return items.stream()
                .map(TableSessionItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPaid() {
        return payments.stream()
                .map(TableSessionPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPending() {
        return getTotal().subtract(getTotalPaid()).max(BigDecimal.ZERO);
    }

    public boolean isFullyPaid() {
        return getTotalPending().compareTo(BigDecimal.ZERO) <= 0;
    }
}