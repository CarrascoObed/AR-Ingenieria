package com.aring.service;

import com.aring.dto.TecnicoRequest;
import com.aring.dto.TecnicoUpdateRequest;
import com.aring.exception.ApiException;
import com.aring.model.EstadoTarea;
import com.aring.model.Tecnico;
import com.aring.repository.InformeTableroRepository;
import com.aring.repository.NotificacionRepository;
import com.aring.repository.TareaRepository;
import com.aring.repository.TecnicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TecnicoService {

    private final TecnicoRepository tecnicoRepository;
    private final TareaRepository tareaRepository;
    private final InformeTableroRepository informeTableroRepository;
    private final NotificacionRepository notificacionRepository;
    private final PasswordEncoder passwordEncoder;

    public TecnicoService(TecnicoRepository tecnicoRepository,
                          TareaRepository tareaRepository,
                          InformeTableroRepository informeTableroRepository,
                          NotificacionRepository notificacionRepository,
                          PasswordEncoder passwordEncoder) {
        this.tecnicoRepository = tecnicoRepository;
        this.tareaRepository = tareaRepository;
        this.informeTableroRepository = informeTableroRepository;
        this.notificacionRepository = notificacionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Tecnico> listar() {
        return tecnicoRepository.findAllByOrderByNombreAscApellidoAsc();
    }

    @Transactional
    public Tecnico crear(TecnicoRequest request) {
        if (tecnicoRepository.existsByRut(request.getRut().trim())) {
            throw new ApiException("Ya existe un técnico con el RUT ingresado.", HttpStatus.CONFLICT);
        }
        Tecnico tecnico = new Tecnico();
        tecnico.setNombre(request.getNombre().trim());
        tecnico.setApellido(request.getApellido().trim());
        tecnico.setRut(request.getRut().trim());
        tecnico.setPassword(passwordEncoder.encode(request.getPassword()));
        tecnico.setContactoNombre(request.getContactoNombre().trim());
        tecnico.setContactoApellido(request.getContactoApellido().trim());
        tecnico.setContactoTelefono(request.getContactoTelefono().trim());
        tecnico.setFechaCreacion(Instant.now());
        return tecnicoRepository.save(tecnico);
    }

    @Transactional
    public Tecnico actualizar(Long id, TecnicoUpdateRequest request) {
        Tecnico tecnico = buscarOError(id);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            tecnico.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        tecnico.setContactoNombre(request.getContactoNombre().trim());
        tecnico.setContactoApellido(request.getContactoApellido().trim());
        tecnico.setContactoTelefono(request.getContactoTelefono().trim());
        return tecnicoRepository.save(tecnico);
    }

    @Transactional
    public void eliminar(Long id) {
        Tecnico tecnico = buscarOError(id);
        if (tareaRepository.existsByTecnicoCabeceraIdAndEstadoNot(id, EstadoTarea.FINALIZADA)) {
            throw new ApiException("No se puede eliminar: el técnico tiene tareas en curso asignadas.", HttpStatus.CONFLICT);
        }
        if (tareaRepository.existsByTecnicosInvitadosIdAndEstadoNot(id, EstadoTarea.FINALIZADA)) {
            throw new ApiException("No se puede eliminar: el técnico está invitado a tareas en curso.", HttpStatus.CONFLICT);
        }
        tareaRepository.liberarTareasDeTecnico(id);
        tareaRepository.eliminarInvitacionesDeTecnico(id);
        informeTableroRepository.deleteByTecnicoId(id);
        tecnicoRepository.delete(tecnico);
    }

    @Transactional
    public void eliminarSinRestricciones(Long id) {
        Tecnico tecnico = buscarOError(id);
        tareaRepository.liberarTareasDeTecnico(id);
        tareaRepository.eliminarInvitacionesDeTecnico(id);
        informeTableroRepository.deleteByTecnicoId(id);
        notificacionRepository.borrarDeTecnico(id);
        tecnicoRepository.delete(tecnico);
    }

    private Tecnico buscarOError(Long id) {
        return tecnicoRepository.findById(id)
                .orElseThrow(() -> new ApiException("El técnico no existe.", HttpStatus.NOT_FOUND));
    }
}
