// src/main/java/com/reservas/dto/response/UserResponse.java
package com.reservas.dto.response;

import com.reservas.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String displayName;
    private Role role;
    private String photoUrl;
    private Long complexId;
    private String complexName; // ✅ NUEVO
}