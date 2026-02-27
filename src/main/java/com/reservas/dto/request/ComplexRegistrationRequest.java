// src/main/java/com/reservas/dto/request/ComplexRegistrationRequest.java
package com.reservas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ComplexRegistrationRequest {

    @NotBlank(message = "El nombre del complejo es requerido")
    private String complexName;

    @NotBlank(message = "La ciudad es requerida")
    private String city;

    @NotBlank(message = "La dirección es requerida")
    private String address;

    @NotBlank(message = "El teléfono es requerido")
    private String phone;

    @NotBlank(message = "El nombre del responsable es requerido")
    private String ownerName;

    @NotBlank(message = "El email es requerido")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
}