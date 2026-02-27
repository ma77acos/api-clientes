// src/main/java/com/reservas/dto/request/RecurringReservationRequest.java
package com.reservas.dto.request;

import com.reservas.enums.RecurrenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringReservationRequest {

    @NotNull(message = "El courtId es requerido")
    private Long courtId;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate startDate;

    @NotNull(message = "La hora es requerida")
    private LocalTime time;

    @NotNull(message = "El precio es requerido")
    @Positive(message = "El precio debe ser positivo")
    private BigDecimal price;

    @NotNull(message = "El tipo de recurrencia es requerido")
    @Builder.Default
    private RecurrenceType recurrenceType = RecurrenceType.WEEKLY;  // Sin @NotNull

    @NotNull(message = "El día de la semana es requerido")
    private DayOfWeek dayOfWeek;

    @NotBlank(message = "El nombre del cliente es requerido")
    private String customerName;

    private Long userId;              // Opcional: null si lo crea BUSINESS
    private Integer occurrences;      // Opcional
    private LocalDate endDate;        // Opcional
    private String customerPhone;     // Opcional
    private String customerEmail;     // Opcional
    private String notes;             // Opcional
}