// src/main/java/com/reservas/entity/Complex.java
package com.reservas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complexes", indexes = {
        @Index(name = "idx_city", columnList = "city"),
        @Index(name = "idx_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 300)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @OneToMany(mappedBy = "complex", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Court> courts = new ArrayList<>();

    @OneToMany(mappedBy = "complex", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // 💳 Mercado Pago OAuth
    @Column(name = "mp_refresh_token", length = 500)
    private String mpRefreshToken;

    @Column(name = "mp_access_token", length = 500)
    private String mpAccessToken; // Token del vendedor (complejo)

    @Column(name = "mp_collector_id")
    private String mpCollectorId; // ID del vendedor en MP

    @Column(name = "mp_public_key", length = 100)
    private String mpPublicKey;

    @Column(name = "mp_scopes", length = 500)
    private String mpScopes;

    @Column(name = "mp_enabled")
    private Boolean mpEnabled; // Si el complejo tiene MP configurado

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasCoveredCourts() {
        return courts.stream().anyMatch(Court::getCovered);
    }

    public BigDecimal getMinPrice() {
        return courts.stream()
                .map(Court::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}