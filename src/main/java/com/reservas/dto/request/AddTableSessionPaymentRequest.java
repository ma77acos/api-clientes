// src/main/java/com/reservas/dto/request/AddTableSessionPaymentRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddTableSessionPaymentRequest {

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotBlank(message = "El método de pago es requerido")
    private String method; // CASH o ELECTRONIC

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String payerName; // Opcional: "Juan", "Persona 1", etc.
}