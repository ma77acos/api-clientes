package com.reservas.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ClienteResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String razonSocial;
    private String cuit;
    private LocalDate fechaNacimiento;
    private String telefonoCelular;
    private String email;
}