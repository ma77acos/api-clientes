// src/main/java/com/reservas/dto/request/RecurringExceptionRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringExceptionRequest {

    @NotNull(message = "El ID de la reserva recurrente es requerido")
    private Long recurringReservationId;

    @NotNull(message = "La fecha de excepción es requerida")
    private LocalDate exceptionDate;

    private String reason;  // Opcional
}