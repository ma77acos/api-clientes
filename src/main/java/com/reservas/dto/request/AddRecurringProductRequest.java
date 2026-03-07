// src/main/java/com/reservas/dto/request/AddRecurringProductRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddRecurringProductRequest {

    @NotNull(message = "La fecha es requerida")
    private LocalDate date;

    @NotBlank(message = "El nombre del producto es requerido")
    private String name;

    @NotNull(message = "El precio es requerido")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal unitPrice;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;
}
