// src/main/java/com/reservas/dto/response/TableSessionPaymentResponse.java
package com.reservas.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableSessionPaymentResponse {

    private Long id;
    private BigDecimal amount;
    private String method;
    private String payerName;
    private LocalDateTime paidAt;
    private String receivedByName;
}