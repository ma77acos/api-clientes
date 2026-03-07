// src/main/java/com/reservas/dto/request/OpenCashRegisterRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenCashRegisterRequest {

    @NotNull(message = "El monto inicial es requerido")
    @PositiveOrZero(message = "El monto debe ser positivo o cero")
    private BigDecimal openingAmount;
}