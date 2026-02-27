// src/main/java/com/reservas/repository/ComplexRepository.java
package com.reservas.repository;

import com.reservas.entity.Complex;
import com.reservas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplexRepository extends JpaRepository<Complex, Long> {

    List<Complex> findByCity(String city);

    @Query("SELECT c FROM Complex c WHERE " +
            "(:city IS NULL OR LOWER(c.city) = LOWER(:city)) AND " +
            "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Complex> findByFilters(@Param("city") String city, @Param("search") String search);

    @Query("SELECT c FROM Complex c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Complex> searchByName(@Param("search") String search);

    List<Complex> findTop6ByActiveOrderByRatingDesc(Boolean active);

    // O si no tenés campo "active":
    @Query("SELECT c FROM Complex c ORDER BY c.rating DESC LIMIT 6")
    List<Complex> findTop6OrderByRatingDesc();

}