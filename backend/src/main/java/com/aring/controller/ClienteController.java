package com.aring.controller;

import com.aring.dto.ClienteDto;
import com.aring.dto.ClienteRequest;
import com.aring.dto.ClienteUpdateRequest;
import com.aring.model.Cliente;
import com.aring.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteDto> listar() {
        return clienteService.listar().stream().map(ClienteDto::from).toList();
    }

    @PostMapping
    public ClienteDto crear(@Valid @RequestBody ClienteRequest request) {
        return ClienteDto.from(clienteService.crear(request));
    }

    @PutMapping("/{id}")
    public ClienteDto actualizar(@PathVariable Long id, @Valid @RequestBody ClienteUpdateRequest request) {
        return ClienteDto.from(clienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
    }
}
