// src/main/java/com/reservas/dto/request/PaymentRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "El reservationId es requerido")
    private Long reservationId;

    @NotNull(message = "El amount es requerido")
    @Positive(message = "El amount debe ser positivo")
    private BigDecimal amount;

    @NotBlank(message = "El método de pago es requerido")
    private String method;
}