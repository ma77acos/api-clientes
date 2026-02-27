// src/main/java/com/reservas/dto/response/AuthResponse.java
package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private UserResponse user;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}