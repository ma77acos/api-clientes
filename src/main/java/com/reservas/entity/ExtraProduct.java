// src/main/java/com/reservas/entity/ExtraProduct.java
package com.reservas.entity;

import com.reservas.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "extra_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation; // Puede ser null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_reservation_id")
    private RecurringReservation recurringReservation; // Puede ser null

    @Column(name = "product_date")
    private LocalDate productDate; // Solo se usa para recurring

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Boolean paid;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private LocalDateTime addedAt;

    private LocalDateTime paidAt;
}