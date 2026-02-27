// src/main/java/com/reservas/dto/request/AdminReservationRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class AdminReservationRequest {

    @NotNull(message = "El courtId es requerido")
    private Long courtId;

    @NotNull(message = "La fecha es requerida")
    private LocalDate date;

    @NotNull(message = "La hora es requerida")
    private LocalTime time;

    @NotBlank(message = "El nombre del cliente es requerido")
    private String customerName;

    private String customerPhone;

    private String customerEmail;

    @NotNull(message = "El precio es requerido")
    @Positive(message = "El precio debe ser positivo")
    private BigDecimal price;

    private String notes; // Notas adicionales
}