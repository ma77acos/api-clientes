// src/main/java/com/reservas/repository/ProductRepository.java
package com.reservas.repository;

import com.reservas.entity.Product;
import com.reservas.enums.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Todos los productos de un complejo
    List<Product> findByComplexIdOrderByNameAsc(Long complexId);

    // Solo productos disponibles
    List<Product> findByComplexIdAndAvailableTrueOrderByNameAsc(Long complexId);

    // Por categoría
    List<Product> findByComplexIdAndCategoryAndAvailableTrueOrderByNameAsc(
            Long complexId, ProductCategory category);

    // Verificar si existe por nombre en el complejo
    boolean existsByComplexIdAndNameIgnoreCase(Long complexId, String name);

    // Verificar si existe por nombre excluyendo un ID (para edición)
    @Query("SELECT COUNT(p) > 0 FROM Product p " +
            "WHERE p.complex.id = :complexId " +
            "AND LOWER(p.name) = LOWER(:name) " +
            "AND p.id != :excludeId")
    boolean existsByComplexIdAndNameIgnoreCaseAndIdNot(
            @Param("complexId") Long complexId,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);

    List<Product> findByComplexIdAndAvailableTrueAndQuickProductTrueOrderByNameAsc(Long complexId);
}