// src/main/java/com/reservas/dto/response/ExtraProductResponse.java
package com.reservas.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraProductResponse {
    private Long id;
    private String name;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
    private Boolean paid;
    private String paymentMethod;
    private LocalDateTime addedAt;
    private LocalDateTime paidAt;
}