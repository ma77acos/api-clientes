// src/main/java/com/reservas/entity/RecurringException.java
package com.reservas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_exceptions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recurring_date",
                        columnNames = {"recurring_reservation_id", "exception_date"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_reservation_id", nullable = false)
    private RecurringReservation recurringReservation;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private User cancelledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}