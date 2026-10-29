package com.aring.controller;

import com.aring.model.Notificacion;
import com.aring.service.NotificacionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public List<Notificacion> listar(@RequestParam(value = "tecnicoId", required = false) Long tecnicoId) {
        if (tecnicoId != null) {
            return notificacionService.listarDeTecnico(tecnicoId);
        }
        return notificacionService.listar();
    }

    @PutMapping("/{id}/leida")
    public Notificacion marcarLeida(@PathVariable Long id) {
        return notificacionService.marcarLeida(id);
    }

    @PutMapping("/leer-todas")
    public void marcarTodasLeidas(@RequestParam(value = "tecnicoId", required = false) Long tecnicoId) {
        if (tecnicoId != null) {
            notificacionService.marcarTodasLeidasTecnico(tecnicoId);
        } else {
            notificacionService.marcarTodasLeidas();
        }
    }

    @DeleteMapping
    public void eliminarTodas(@RequestParam(value = "tecnicoId", required = false) Long tecnicoId) {
        if (tecnicoId != null) {
            notificacionService.eliminarTodasTecnico(tecnicoId);
        } else {
            notificacionService.eliminarTodas();
        }
    }
}
