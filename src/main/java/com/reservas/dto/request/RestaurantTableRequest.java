// src/main/java/com/reservas/dto/request/RestaurantTableRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantTableRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
    private String name;

    private Integer displayOrder = 0;

    private Boolean active = true;
}