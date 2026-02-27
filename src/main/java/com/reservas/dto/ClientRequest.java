package com.reservas.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClientRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    private String razonSocial;

    @NotBlank
    @Pattern(regexp = "^\\d{2}-\\d{8}-\\d{1}$")
    private String cuit;

    @NotNull
    @Past
    private LocalDate fechaNacimiento;

    @NotBlank
    private String telefonoCelular;

    @NotBlank
    @Email
    private String email;
}
