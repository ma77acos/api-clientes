// src/main/java/com/reservas/dto/response/TableSessionResponse.java
package com.reservas.dto.response;

import com.reservas.enums.TableSessionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableSessionResponse {

    private Long id;
    private Long tableId;
    private String tableName;
    private String customerName;
    private TableSessionStatus status;

    private LocalDateTime openedAt;
    private String openedByName;
    private LocalDateTime closedAt;
    private String closedByName;

    private String notes;

    // Totales
    private BigDecimal total;
    private BigDecimal totalPaid;
    private BigDecimal totalPending;
    private boolean fullyPaid;

    // Detalle
    private List<TableSessionItemResponse> items;
    private List<TableSessionPaymentResponse> payments;
}