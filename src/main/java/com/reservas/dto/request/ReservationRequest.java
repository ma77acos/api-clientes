// src/main/java/com/reservas/dto/request/ReservationRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    @NotNull(message = "El courtId es requerido")
    private Long courtId;

    @NotNull(message = "La fecha es requerida")
    private LocalDate date;

    @NotNull(message = "La hora es requerida")
    private LocalTime time;

    @NotNull(message = "El userId es requerido")
    private Long userId;

    @NotNull(message = "El precio es requerido")
    @Positive(message = "El precio debe ser positivo")
    private BigDecimal price;
}