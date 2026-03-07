// src/main/java/com/reservas/repository/TableSessionItemRepository.java
package com.reservas.repository;

import com.reservas.entity.TableSessionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TableSessionItemRepository extends JpaRepository<TableSessionItem, Long> {

    // Items de una sesión ordenados por fecha
    List<TableSessionItem> findBySessionIdOrderByAddedAtAsc(Long sessionId);

    // Total de una sesión
    @Query("SELECT COALESCE(SUM(i.totalPrice), 0) FROM TableSessionItem i " +
            "WHERE i.session.id = :sessionId")
    BigDecimal sumTotalBySessionId(@Param("sessionId") Long sessionId);

    // Contar items de una sesión
    int countBySessionId(Long sessionId);
}