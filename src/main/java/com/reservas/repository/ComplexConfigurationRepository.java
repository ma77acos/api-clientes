// src/main/java/com/reservas/repository/ComplexConfigurationRepository.java
package com.reservas.repository;

import com.reservas.entity.ComplexConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplexConfigurationRepository extends JpaRepository<ComplexConfiguration, Long> {

    Optional<ComplexConfiguration> findByComplexId(Long complexId);
}