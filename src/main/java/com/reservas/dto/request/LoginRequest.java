// src/main/java/com/reservas/dto/request/LoginRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "El username es requerido")
    @Email(message = "El username debe ser un email válido")
    private String username;

    @NotBlank(message = "La contraseña es requerida")
    private String password;
}