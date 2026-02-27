// src/main/java/com/reservas/entity/AvailabilitySlot.java
package com.reservas.entity;

import com.reservas.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "availability_slots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_court_date_time", columnNames = {"court_id", "date", "time"})
        },
        indexes = {
                @Index(name = "idx_court_date", columnList = "court_id, date"),
                @Index(name = "idx_court_date_status", columnList = "court_id, date, status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;
}