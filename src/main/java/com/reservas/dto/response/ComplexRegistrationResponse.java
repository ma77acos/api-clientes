// src/main/java/com/reservas/dto/response/ComplexRegistrationResponse.java
package com.reservas.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComplexRegistrationResponse {
    private Long complexId;
    private Long userId;
    private String message;
}