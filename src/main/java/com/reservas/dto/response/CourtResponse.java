// src/main/java/com/reservas/dto/response/CourtResponse.java
package com.reservas.dto.response;

import com.reservas.enums.Surface;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtResponse {
    private Long id;
    private String name;
    private Boolean covered;
    private Surface surface;
    private BigDecimal price;
    private String imageUrl;
}