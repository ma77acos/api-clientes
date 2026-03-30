// src/main/java/com/reservas/dto/request/CashMovementRequest.java
package com.reservas.dto.request;

import com.reservas.enums.MovementCategory;
import com.reservas.enums.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashMovementRequest {

    @NotNull(message = "El tipo es requerido")
    private MovementType type;

    @NotNull(message = "La categoría es requerida")
    private MovementCategory category;

    private String description;

    @NotNull(message = "El monto es requerido")
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal amount;
}
