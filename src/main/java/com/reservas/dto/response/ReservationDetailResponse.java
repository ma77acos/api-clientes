// src/main/java/com/reservas/dto/response/ReservationDetailResponse.java
package com.reservas.dto.response;

import com.reservas.enums.ReservationStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDetailResponse {
    private Long id;
    private Long courtId;
    private String courtName;
    private Long complexId;
    private String complexName;
    private String complexAddress;
    private LocalDate date;
    private LocalTime time;
    private BigDecimal price;
    private ReservationStatus status;
    private String customerName;
    private LocalDateTime createdAt;

    // Campos de pago
    private Boolean isFullyPaid;
    private BigDecimal totalPaid;
    private BigDecimal totalPending;
    private BigDecimal productsTotal;
    private BigDecimal grandTotal;

    // Listas de pagos y productos
    private List<PlayerPaymentResponse> playerPayments;
    private List<ExtraProductResponse> extraProducts;
}