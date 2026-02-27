// src/main/java/com/reservas/dto/response/SlotResponse.java
package com.reservas.dto.response;

import com.reservas.enums.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponse {
    private String time;
    private SlotStatus status;
}