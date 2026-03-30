// src/main/java/com/reservas/entity/CashMovement.java
package com.reservas.entity;

import com.reservas.enums.MovementCategory;
import com.reservas.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegister cashRegister;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementCategory category;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Referencias opcionales (para vincular con pagos automáticos)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_payment_id")
    private PlayerPayment playerPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extra_product_id")
    private ExtraProduct extraProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}