// src/main/java/com/reservas/dto/request/RefreshTokenRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {

    @NotBlank(message = "El refresh token es requerido")
    private String refreshToken;
}