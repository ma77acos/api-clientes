// src/main/java/com/reservas/dto/response/CashMovementResponse.java
package com.reservas.dto.response;

import com.reservas.enums.MovementCategory;
import com.reservas.enums.MovementType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashMovementResponse {

    private Long id;
    private MovementType type;
    private MovementCategory category;
    private String categoryDisplayName;
    private String description;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private String createdByName;

    // Info adicional si viene de pago automático
    private boolean isAutomatic;
    private String reservationInfo; // "Cancha 1 - 18:00 - Juan"
}
