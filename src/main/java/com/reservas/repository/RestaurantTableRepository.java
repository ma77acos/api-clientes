// src/main/java/com/reservas/repository/RestaurantTableRepository.java
package com.reservas.repository;

import com.reservas.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    // Todas las mesas de un complejo ordenadas
    List<RestaurantTable> findByComplexIdOrderByDisplayOrderAscNameAsc(Long complexId);

    // Solo mesas activas
    List<RestaurantTable> findByComplexIdAndActiveTrueOrderByDisplayOrderAscNameAsc(Long complexId);

    // Verificar si existe por nombre en el complejo
    boolean existsByComplexIdAndNameIgnoreCase(Long complexId, String name);

    // Verificar si existe por nombre excluyendo un ID (para edición)
    @Query("SELECT COUNT(t) > 0 FROM RestaurantTable t " +
            "WHERE t.complex.id = :complexId " +
            "AND LOWER(t.name) = LOWER(:name) " +
            "AND t.id != :excludeId")
    boolean existsByComplexIdAndNameIgnoreCaseAndIdNot(
            @Param("complexId") Long complexId,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);

    // Contar mesas activas
    int countByComplexIdAndActiveTrue(Long complexId);
}