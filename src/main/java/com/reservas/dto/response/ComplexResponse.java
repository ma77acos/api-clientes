// src/main/java/com/reservas/dto/response/ComplexResponse.java
package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplexResponse {
    private Long id;
    private String name;
    private String city;
    private BigDecimal rating;
    private BigDecimal priceFrom;
    private Boolean coveredCourts;
    private Integer occupationToday;
    private String imageUrl;
}