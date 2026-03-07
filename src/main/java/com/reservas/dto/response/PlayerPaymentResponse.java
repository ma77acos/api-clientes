// src/main/java/com/reservas/dto/response/PlayerPaymentResponse.java
package com.reservas.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerPaymentResponse {
    private Long id;
    private String playerName;
    private BigDecimal amount;
    private String method;
    private LocalDateTime paidAt;
}