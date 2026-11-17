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
        String rut = normalizarRut(request.getRut());
        if (tecnicoRepository.existsByRut(rut)) {
            throw new ApiException("Ya existe un técnico con el RUT ingresado.", HttpStatus.CONFLICT);
        }
        Tecnico tecnico = new Tecnico();
        tecnico.setNombre(request.getNombre().trim());
        tecnico.setApellido(request.getApellido().trim());
        tecnico.setRut(rut);
        tecnico.setPassword(passwordEncoder.encode(request.getPassword()));
        tecnico.setContactoNombre(request.getContactoNombre().trim());
        tecnico.setContactoApellido(request.getContactoApellido().trim());
        tecnico.setContactoTelefono(normalizarTelefono(request.getContactoTelefono()));
        tecnico.setFechaCreacion(Instant.now());
        return tecnicoRepository.save(tecnico);
    }

    @Transactional
    public Tecnico actualizar(Long id, TecnicoUpdateRequest request) {
        Tecnico tecnico = buscarOError(id);
        String rut = normalizarRut(request.getRut());
        if (tecnicoRepository.existsByRutAndIdNot(rut, id)) {
            throw new ApiException("Ya existe otro técnico con el RUT ingresado.", HttpStatus.CONFLICT);
        }
        tecnico.setRut(rut);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            tecnico.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        tecnico.setContactoNombre(request.getContactoNombre().trim());
        tecnico.setContactoApellido(request.getContactoApellido().trim());
        tecnico.setContactoTelefono(normalizarTelefono(request.getContactoTelefono()));
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

    private String normalizarRut(String rut) {
        String valor = rut == null ? "" : rut.trim();
        if (!valor.matches("\\d{1,8}")) {
            throw new ApiException("El RUT debe contener entre 1 y 8 dígitos, sin guion ni dígito verificador.",
                    HttpStatus.BAD_REQUEST);
        }
        return valor;
    }

    private String normalizarTelefono(String telefono) {
        String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
        String local;
        if (digitos.startsWith("569")) {
            local = digitos.substring(3);
        } else if (digitos.startsWith("56")) {
            local = digitos.substring(2);
            if (local.startsWith("9")) local = local.substring(1);
        } else if (digitos.startsWith("9") && digitos.length() == 9) {
            local = digitos.substring(1);
        } else {
            local = digitos;
        }
        if (!local.matches("\\d{8}")) {
            throw new ApiException("El teléfono debe incluir +569 y 8 dígitos.", HttpStatus.BAD_REQUEST);
        }
        return "+569" + local;
    }
}
