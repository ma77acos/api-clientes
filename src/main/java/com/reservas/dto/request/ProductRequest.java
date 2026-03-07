// src/main/java/com/reservas/dto/request/ProductRequest.java
package com.reservas.dto.request;

import com.reservas.enums.ProductCategory;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String name;

    @NotNull(message = "La categoría es requerida")
    private ProductCategory category;

    @NotNull(message = "El precio es requerido")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal price;

    private String imageUrl;

    private Boolean available = true;

    private Boolean quickProduct = false;
}