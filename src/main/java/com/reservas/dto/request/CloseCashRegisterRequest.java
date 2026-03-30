// src/main/java/com/reservas/dto/request/CloseCashRegisterRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloseCashRegisterRequest {

    @NotNull(message = "El monto real es requerido")
    @PositiveOrZero(message = "El monto debe ser positivo o cero")
    private BigDecimal actualAmount;

    @NotNull(message = "El monto para mañana es requerido")
    @PositiveOrZero(message = "El monto debe ser positivo o cero")
    private BigDecimal carryOverAmount;

    private String closingNotes;
}
