package com.aring.controller;

import com.aring.dto.EstadoRequest;
import com.aring.dto.IniciarRequest;
import com.aring.dto.TareaDto;
import com.aring.dto.TareaRequest;
import com.aring.dto.TareaUpdateRequest;
import com.aring.model.Tarea;
import com.aring.service.TareaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tareas")
public class TareaController {

    private final TareaService tareaService;

    public TareaController(TareaService tareaService) {
        this.tareaService = tareaService;
    }

    @GetMapping
    public List<TareaDto> listar() {
        return tareaService.listar().stream().map(TareaDto::from).toList();
    }

    @GetMapping("/mias")
    public List<TareaDto> listarMias(@RequestParam("tecnicoId") Long tecnicoId) {
        return tareaService.listarDeTecnico(tecnicoId).stream().map(TareaDto::from).toList();
    }

    @PostMapping("/{id}/iniciar")
    public TareaDto iniciar(@PathVariable Long id, @Valid @RequestBody IniciarRequest request) {
        return TareaDto.from(tareaService.iniciar(id, request));
    }

    @PostMapping
    public TareaDto crear(@Valid @RequestBody TareaRequest request) {
        return TareaDto.from(tareaService.crear(request));
    }

    @PutMapping("/{id}")
    public TareaDto actualizarTecnico(@PathVariable Long id, @Valid @RequestBody TareaUpdateRequest request) {
        return TareaDto.from(tareaService.actualizarTecnico(id, request));
    }

    @PutMapping("/{id}/estado")
    public TareaDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return TareaDto.from(tareaService.cambiarEstado(id, request));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        tareaService.eliminar(id);
    }
}
