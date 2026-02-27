// src/main/java/com/reservas/dto/response/ComplexDetailResponse.java
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
public class ComplexDetailResponse {
    private Long id;
    private String name;
    private String city;
    private String description;
    private BigDecimal rating;
    private String address;
    private String phone;
    private String imageUrl;
}