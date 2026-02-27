package com.reservas.dto.request;

import com.reservas.enums.Surface;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourtRequest {
    @NotBlank(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "Debe indicar si es techada")
    private Boolean covered;

    @NotNull(message = "La superficie es requerida")
    private Surface surface;

    @NotNull(message = "El precio es requerido")
    @Positive(message = "El precio debe ser positivo")
    private BigDecimal price;
}