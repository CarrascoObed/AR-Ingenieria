package com.aring.service;

import com.aring.dto.EstadoRequest;
import com.aring.dto.IniciarRequest;
import com.aring.dto.TareaRequest;
import com.aring.dto.TareaUpdateRequest;
import com.aring.exception.ApiException;
import com.aring.model.EstadoTarea;
import com.aring.model.Cliente;
import com.aring.model.SecuenciaTarea;
import com.aring.model.Tarea;
import com.aring.model.Tecnico;
import com.aring.model.TipoNotificacion;
import com.aring.model.TipoTarea;
import com.aring.repository.InformeTableroRepository;
import com.aring.repository.ClienteRepository;
import com.aring.repository.NotificacionRepository;
import com.aring.repository.SecuenciaTareaRepository;
import com.aring.repository.TareaRepository;
import com.aring.repository.TecnicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TareaService {

    private final TareaRepository tareaRepository;
    private final TecnicoRepository tecnicoRepository;
    private final ClienteRepository clienteRepository;
    private final SecuenciaTareaRepository secuenciaTareaRepository;
    private final InformeTableroRepository informeTableroRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;

    public TareaService(TareaRepository tareaRepository,
                        TecnicoRepository tecnicoRepository,
                        ClienteRepository clienteRepository,
                        SecuenciaTareaRepository secuenciaTareaRepository,
                        InformeTableroRepository informeTableroRepository,
                        NotificacionRepository notificacionRepository,
                        NotificacionService notificacionService) {
        this.tareaRepository = tareaRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.clienteRepository = clienteRepository;
        this.secuenciaTareaRepository = secuenciaTareaRepository;
        this.informeTableroRepository = informeTableroRepository;
        this.notificacionRepository = notificacionRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public List<Tarea> listar() {
        return tareaRepository.findAll();
    }

    @Transactional
    public Tarea crear(TareaRequest request) {
        if (request.getTipo() != TipoTarea.MANTENIMIENTO) {
            throw new ApiException("Por ahora solo se pueden crear tareas de tipo Mantenimiento.", HttpStatus.BAD_REQUEST);
        }
        Tecnico tecnico = tecnicoRepository.findById(request.getTecnicoCabeceraId())
                .orElseThrow(() -> new ApiException("El técnico asignado no existe.", HttpStatus.BAD_REQUEST));

        Tarea tarea = new Tarea();
        tarea.setCodigo(generarCodigo(TipoTarea.MANTENIMIENTO));
        tarea.setTipo(TipoTarea.MANTENIMIENTO);
        tarea.setDireccion(request.getDireccion().trim());
        tarea.setNombreRecinto(request.getNombreRecinto().trim());
        tarea.setDescripcion(request.getDescripcion().trim());
        tarea.setTecnicoCabecera(tecnico);
        if (request.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ApiException("El cliente seleccionado no existe.", HttpStatus.BAD_REQUEST));
            tarea.setCliente(cliente);
        }
        tarea.setEstado(EstadoTarea.NO_INICIADA);
        tarea.setFechaCreacion(Instant.now());
        return tareaRepository.save(tarea);
    }

    @Transactional
    public Tarea actualizarTecnico(Long id, TareaUpdateRequest request) {
        Tarea tarea = buscarOError(id);
        if (tarea.getEstado() != EstadoTarea.NO_INICIADA) {
            throw new ApiException("Solo se puede editar una tarea que aún no ha sido iniciada.", HttpStatus.BAD_REQUEST);
        }
        Tecnico tecnico = tecnicoRepository.findById(request.getTecnicoCabeceraId())
                .orElseThrow(() -> new ApiException("El técnico asignado no existe.", HttpStatus.BAD_REQUEST));
        tarea.setTecnicoCabecera(tecnico);
        return tareaRepository.save(tarea);
    }

    @Transactional
    public void eliminar(Long id) {
        Tarea tarea = buscarOError(id);
        if (tarea.getEstado() != EstadoTarea.NO_INICIADA) {
            throw new ApiException("No se puede eliminar una tarea que ya ha sido iniciada.", HttpStatus.BAD_REQUEST);
        }
        tareaRepository.delete(tarea);
    }

    @Transactional
    public void eliminarCualquiera(Long id) {
        Tarea tarea = buscarOError(id);
        informeTableroRepository.deleteByTareaId(id);
        notificacionRepository.borrarDeTarea(id);
        tareaRepository.eliminarInvitacionesDeTarea(id);
        tareaRepository.delete(tarea);
    }

    @Transactional
    public void eliminarTodas() {
        informeTableroRepository.deleteAll();
        notificacionRepository.deleteAll();
        tareaRepository.eliminarTodasLasInvitaciones();
        tareaRepository.deleteAll();
        secuenciaTareaRepository.findAll()
                .forEach(secuencia -> {
                    secuencia.setUltimoNumero(0L);
                    secuenciaTareaRepository.save(secuencia);
                });
    }

    @Transactional
    public Tarea cambiarEstado(Long id, EstadoRequest request) {
        Tarea tarea = buscarOError(id);
        EstadoTarea nuevo = request.getEstado();

        if (nuevo == EstadoTarea.FINALIZADA && tarea.getEstado() == EstadoTarea.FINALIZADA) {
            return tarea;
        }

        if (nuevo == EstadoTarea.EN_PROCESO && tarea.getEstado() == EstadoTarea.NO_INICIADA) {
            tarea.setEstado(EstadoTarea.EN_PROCESO);
            tarea.setFechaInicio(Instant.now());
        } else if (nuevo == EstadoTarea.FINALIZADA && tarea.getEstado() == EstadoTarea.EN_PROCESO) {
            tarea.setEstado(EstadoTarea.FINALIZADA);
            tarea.setFechaFin(Instant.now());
            Tecnico tecnico = tarea.getTecnicoCabecera();
            notificacionService.crear("Tarea " + tarea.getCodigo() + " finalizada por "
                    + tecnico.getNombre() + " " + tecnico.getApellido() + ".",
                    TipoNotificacion.TAREA_FINALIZADA, tarea.getId(), null);
        } else {
            throw new ApiException("Transición de estado no permitida.", HttpStatus.BAD_REQUEST);
        }
        return tareaRepository.save(tarea);
    }

    @Transactional
    public Tarea iniciar(Long id, IniciarRequest request) {
        Tarea tarea = buscarOError(id);
        if (tarea.getEstado() == EstadoTarea.EN_PROCESO) {
            // Idempotencia para reintentos de la sincronización móvil: si la tarea
            // ya está en proceso se devuelve tal cual, sin duplicar notificaciones.
            return tarea;
        }
        if (tarea.getEstado() != EstadoTarea.NO_INICIADA) {
            throw new ApiException("Solo se puede iniciar una tarea que está 'No iniciada'.", HttpStatus.BAD_REQUEST);
        }
        tarea.setEstado(EstadoTarea.EN_PROCESO);
        tarea.setFechaInicio(Instant.now());

        List<Tecnico> invitados = new ArrayList<>();
        for (Long tecnicoId : request.getAyudantesIds()) {
            invitados.add(tecnicoRepository.findById(tecnicoId)
                    .orElseThrow(() -> new ApiException("El técnico invitado no existe.", HttpStatus.BAD_REQUEST)));
        }
        tarea.setTecnicosInvitados(invitados);
        tareaRepository.save(tarea);

        notificacionService.crear("La tarea " + tarea.getCodigo() + " fue iniciada.",
                TipoNotificacion.TAREA_INICIADA, tarea.getId(), tarea.getTecnicoCabecera().getId());
        Tecnico cabecera = tarea.getTecnicoCabecera();
        String nombreCabecera = cabecera.getNombre() + " " + cabecera.getApellido();
        for (Tecnico invitado : invitados) {
            notificacionService.crear(
                    "Técnico: " + nombreCabecera + " te ha invitado a realizar la tarea " + tarea.getCodigo() + ".",
                    TipoNotificacion.INVITACION,
                    tarea.getId(),
                    invitado.getId());
        }
        return tarea;
    }

    @Transactional(readOnly = true)
    public List<Tarea> listarDeTecnico(Long tecnicoId) {
        return tareaRepository.findAllParaTecnico(tecnicoId);
    }

    @Transactional
    public String generarCodigo(TipoTarea tipo) {
        SecuenciaTarea secuencia = secuenciaTareaRepository.findByTipoParaActualizar(tipo)
                .orElseThrow(() -> new ApiException("Secuencia de tareas no configurada.", HttpStatus.INTERNAL_SERVER_ERROR));
        long siguiente = secuencia.getUltimoNumero() + 1;
        secuencia.setUltimoNumero(siguiente);
        secuenciaTareaRepository.save(secuencia);
        return tipo.getPrefijo() + "-" + String.format("%04d", siguiente);
    }

    private Tarea buscarOError(Long id) {
        return tareaRepository.findById(id)
                .orElseThrow(() -> new ApiException("La tarea no existe.", HttpStatus.NOT_FOUND));
    }
}
