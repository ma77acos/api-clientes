// src/main/java/com/reservas/dto/response/QuickSaleResponse.java
package com.reservas.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickSaleResponse {

    private Long id; // ID del movimiento de caja
    private String customerName;
    private String paymentMethod;
    private BigDecimal total;
    private List<QuickSaleItemResponse> items;
    private LocalDateTime createdAt;
    private String createdByName;
}