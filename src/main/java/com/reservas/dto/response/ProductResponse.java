// src/main/java/com/reservas/dto/response/ProductResponse.java
package com.reservas.dto.response;

import com.reservas.enums.ProductCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private ProductCategory category;
    private String categoryDisplayName;
    private BigDecimal price;
    private String imageUrl;
    private Boolean available;
    private Boolean quickProduct;
    private LocalDateTime createdAt;
}