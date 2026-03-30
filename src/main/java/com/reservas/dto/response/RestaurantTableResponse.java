// src/main/java/com/reservas/dto/response/RestaurantTableResponse.java
package com.reservas.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantTableResponse {

    private Long id;
    private String name;
    private Integer displayOrder;
    private Boolean active;
    private LocalDateTime createdAt;

    // Info de sesión activa (si existe)
    private boolean hasOpenSession;
    private Long openSessionId;
    private String openSessionCustomerName;
    private LocalDateTime openSessionStartedAt;
}