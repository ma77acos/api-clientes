// src/main/java/com/reservas/entity/ComplexConfiguration.java
package com.reservas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "complex_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplexConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", unique = true, nullable = false)
    private Complex complex;

    @Column(name = "slot_start_time", nullable = false)
    private LocalTime slotStartTime;  // Ej: 08:00

    @Column(name = "slot_end_time", nullable = false)
    private LocalTime slotEndTime;    // Ej: 23:00

    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes;  // Ej: 60, 90, 120

    @Column(name = "days_advance_booking")
    @Builder.Default
    private Integer daysAdvanceBooking = 7;  // Días de anticipación para reservar

    @Column(name = "cancellation_hours")
    @Builder.Default
    private Integer cancellationHours = 24;  // Horas antes para cancelar
}