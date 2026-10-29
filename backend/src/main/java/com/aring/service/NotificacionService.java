package com.aring.service;

import com.aring.model.Notificacion;
import com.aring.model.TipoNotificacion;
import com.aring.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional
    public Notificacion crear(String mensaje, TipoNotificacion tipo) {
        return crear(mensaje, tipo, null, null);
    }

    @Transactional
    public Notificacion crear(String mensaje, TipoNotificacion tipo, Long tareaId, Long tecnicoDestinoId) {
        Notificacion notificacion = new Notificacion();
        notificacion.setTipo(tipo);
        notificacion.setMensaje(mensaje);
        notificacion.setFecha(Instant.now());
        notificacion.setLeida(false);
        notificacion.setTareaId(tareaId);
        notificacion.setTecnicoDestinoId(tecnicoDestinoId);
        return notificacionRepository.save(notificacion);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listar() {
        return notificacionRepository.findAllByTipoOrderByFechaDesc(TipoNotificacion.TAREA_FINALIZADA);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listarDeTecnico(Long tecnicoId) {
        return notificacionRepository.findAllByTecnicoDestinoIdOrderByFechaDesc(tecnicoId);
    }

    @Transactional
    public Notificacion marcarLeida(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new com.aring.exception.ApiException("La notificación no existe.",
                        org.springframework.http.HttpStatus.NOT_FOUND));
        notificacion.setLeida(true);
        return notificacionRepository.save(notificacion);
    }

    @Transactional
    public void marcarTodasLeidas() {
        notificacionRepository.marcarTodasLeidas();
    }

    @Transactional
    public void marcarTodasLeidasTecnico(Long tecnicoId) {
        notificacionRepository.marcarTodasLeidasTecnico(tecnicoId);
    }

    @Transactional
    public void eliminarTodas() {
        notificacionRepository.borrarFinalizadas();
    }

    @Transactional
    public void eliminarTodasTecnico(Long tecnicoId) {
        notificacionRepository.borrarDeTecnico(tecnicoId);
    }
}
