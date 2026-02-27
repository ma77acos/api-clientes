// src/main/java/com/reservas/dto/request/ComplexConfigurationRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ComplexConfigurationRequest {

    @NotNull(message = "La hora de inicio es requerida")
    private LocalTime slotStartTime;

    @NotNull(message = "La hora de fin es requerida")
    private LocalTime slotEndTime;

    @NotNull(message = "La duración del turno es requerida")
    @Min(value = 30, message = "Mínimo 30 minutos")
    @Max(value = 180, message = "Máximo 180 minutos")
    private Integer slotDurationMinutes;

    @Min(value = 1, message = "Mínimo 1 día")
    @Max(value = 30, message = "Máximo 30 días")
    private Integer daysAdvanceBooking;

    @Min(value = 1, message = "Mínimo 1 hora")
    @Max(value = 72, message = "Máximo 72 horas")
    private Integer cancellationHours;
}