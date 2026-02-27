// src/main/java/com/reservas/repository/CourtRepository.java
package com.reservas.repository;

import com.reservas.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByComplexId(Long complexId);
}