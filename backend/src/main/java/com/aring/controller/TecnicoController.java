package com.aring.controller;

import com.aring.dto.TecnicoDto;
import com.aring.dto.TecnicoRequest;
import com.aring.dto.TecnicoUpdateRequest;
import com.aring.model.Tecnico;
import com.aring.service.TecnicoService;
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
@RequestMapping("/api/tecnicos")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    @GetMapping
    public List<TecnicoDto> listar() {
        return tecnicoService.listar().stream().map(TecnicoDto::from).toList();
    }

    @PostMapping
    public TecnicoDto crear(@Valid @RequestBody TecnicoRequest request) {
        return TecnicoDto.from(tecnicoService.crear(request));
    }

    @PutMapping("/{id}")
    public TecnicoDto actualizar(@PathVariable Long id, @Valid @RequestBody TecnicoUpdateRequest request) {
        return TecnicoDto.from(tecnicoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        tecnicoService.eliminar(id);
    }
}
