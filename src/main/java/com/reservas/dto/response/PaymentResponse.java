// src/main/java/com/reservas/dto/response/PaymentResponse.java
package com.reservas.dto.response;

import com.reservas.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String paymentId;
    private PaymentStatus status;
    private String checkoutUrl;
    private String sandboxCheckoutUrl; // 👈 AGREGAR para testing
}