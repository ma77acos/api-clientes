// src/main/java/com/reservas/dto/request/OpenTableSessionRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenTableSessionRequest {

    @NotNull(message = "El ID de la mesa es requerido")
    private Long tableId;

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String customerName; // Opcional

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    private String notes; // Opcional
}