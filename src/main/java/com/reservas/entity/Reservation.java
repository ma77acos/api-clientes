// src/main/java/com/reservas/entity/Reservation.java
package com.reservas.entity;

import com.reservas.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// src/main/java/com/reservas/entity/Reservation.java
@Entity
@Table(name = "reservations",
        //uniqueConstraints = {
        //        @UniqueConstraint(name = "uk_court_date_time", columnNames = {"court_id", "date", "time"})
        //},
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_court_id", columnList = "court_id"),
                @Index(name = "idx_date", columnList = "date"),
                @Index(name = "idx_status", columnList = "status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    // ✅ Usuario que hizo la reserva (NULLABLE para reservas de admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true) // ✅ CAMBIADO A TRUE
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    // ✅ Datos del cliente (para reservas hechas por admin)
    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ✅ Indica si fue creada por admin
    @Column(name = "created_by_admin")
    private Boolean createdByAdmin;

    // ✅ Admin que creó la reserva
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    @OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY)
    private AvailabilitySlot availabilitySlot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 🆕 Relaciones para pagos y productos
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlayerPayment> playerPayments = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExtraProduct> extraProducts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ReservationStatus.PENDING;
        }
        if (createdByAdmin == null) {
            createdByAdmin = false;
        }
    }

    // 🆕 Métodos de utilidad
    public BigDecimal getProductsTotal() {
        return extraProducts.stream()
                .map(ExtraProduct::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getGrandTotal() {
        return price.add(getProductsTotal());
    }

    public BigDecimal getTotalPaid() {
        BigDecimal playerPaymentsTotal = playerPayments.stream()
                .map(PlayerPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidProductsTotal = extraProducts.stream()
                .filter(ExtraProduct::getPaid)
                .map(ExtraProduct::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return playerPaymentsTotal.add(paidProductsTotal);
    }

    public BigDecimal getTotalPending() {
        return getGrandTotal().subtract(getTotalPaid()).max(BigDecimal.ZERO);
    }

    public boolean isFullyPaid() {
        return getTotalPending().compareTo(BigDecimal.ZERO) <= 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}