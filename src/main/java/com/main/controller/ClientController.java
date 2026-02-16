package com.main.controller;

import com.main.dto.ClientRequest;
import com.main.entity.Cliente;
import com.main.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClientController {

    private final ClienteService clienteService;

    @GetMapping
    public List<Cliente> getAll() {
        return clienteService.getAll();
    }

    @GetMapping("/{id}")
    public Cliente getById(@PathVariable Long id) {
        return clienteService.getById(id);
    }

    @GetMapping("/search")
    public List<Cliente> search(@RequestParam String nombre) {
        return clienteService.search(nombre);
    }

    @PostMapping
    public ResponseEntity<Cliente> create(@Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.create(request));
    }

    @PutMapping("/{id}")
    public Cliente update(@PathVariable Long id,
                          @Valid @RequestBody ClientRequest request) {
        return clienteService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clienteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
