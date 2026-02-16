package com.main.service;

import com.main.dto.ClientRequest;
import com.main.dto.ClienteResponse;
import com.main.entity.Cliente;
import com.main.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public List<Cliente> getAll() {
        return clienteRepository.findAll();
    }

    public Cliente getById(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
    }

    public List<Cliente> search(String nombre) {
        return clienteRepository.searchClientes(nombre);
    }

    public Cliente create(ClientRequest  request) {

        if (clienteRepository.existsByCuit(request.getCuit()))
            throw new IllegalArgumentException("CUIT ya existe");

        if (clienteRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email ya existe");

        Cliente cliente = mapToEntity(request);

        Cliente saved = clienteRepository.save(cliente);
        log.info("Cliente creado id={}", saved.getId());

        return saved;
    }

    public Cliente update(Long id, ClientRequest request) {

        Cliente cliente = getById(id);

        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setRazonSocial(request.getRazonSocial());
        cliente.setTelefonoCelular(request.getTelefonoCelular());
        cliente.setEmail(request.getEmail());
        cliente.setFechaNacimiento(request.getFechaNacimiento());

        Cliente updated = clienteRepository.save(cliente);
        log.info("Cliente actualizado id={}", updated.getId());

        return updated;
    }

    public void delete(Long id) {
        Cliente cliente = getById(id);
        clienteRepository.delete(cliente);
        log.info("Cliente eliminado id={}", id);
    }

    private Cliente mapToEntity(ClientRequest request) {
        return Cliente.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .razonSocial(request.getRazonSocial())
                .cuit(request.getCuit())
                .fechaNacimiento(request.getFechaNacimiento())
                .telefonoCelular(request.getTelefonoCelular())
                .email(request.getEmail())
                .build();
    }

    private ClienteResponse toResponse(Cliente c) {
        return ClienteResponse.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .apellido(c.getApellido())
                .razonSocial(c.getRazonSocial())
                .cuit(c.getCuit())
                .fechaNacimiento(c.getFechaNacimiento())
                .telefonoCelular(c.getTelefonoCelular())
                .email(c.getEmail())
                .build();
    }

}
