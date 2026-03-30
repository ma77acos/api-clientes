// src/main/java/com/reservas/dto/request/AddPlayerPaymentRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddPlayerPaymentRequest {

    @NotBlank(message = "El nombre del jugador es requerido")
    private String playerName;

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotBlank(message = "El método de pago es requerido")
    @Pattern(regexp = "CASH|ELECTRONIC", message = "Método de pago inválido")
    private String method;
}