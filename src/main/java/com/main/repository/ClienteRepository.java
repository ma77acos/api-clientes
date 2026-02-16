package com.main.repository;

import com.main.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCuit(String cuit);

    boolean existsByEmail(String email);

    @Query(value = "SELECT * FROM sp_search_clientes(:nombre)", nativeQuery = true)
    List<Cliente> searchClientes(@Param("nombre") String nombre);

    @Query(value = "SELECT * FROM buscar_clientes(:nombre)", nativeQuery = true)
    List<Cliente> buscarPorNombre(@Param("nombre") String nombre);

}
