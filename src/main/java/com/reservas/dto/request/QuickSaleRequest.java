// src/main/java/com/reservas/dto/request/QuickSaleRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickSaleRequest {

    @NotEmpty(message = "Debe incluir al menos un producto")
    private List<QuickSaleItemRequest> items;

    @NotBlank(message = "El método de pago es requerido")
    private String paymentMethod; // CASH o ELECTRONIC

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String customerName; // Opcional
}