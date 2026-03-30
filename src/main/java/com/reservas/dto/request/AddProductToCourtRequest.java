// src/main/java/com/reservas/dto/request/AddProductToCourtRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddProductToCourtRequest {

    @NotNull(message = "El ID del producto es requerido")
    private Long productId;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;

    // Para reserva normal
    private Long reservationId;

    // Para reserva recurrente
    private Long recurringReservationId;
    private LocalDate date; // Fecha del turno recurrente
}