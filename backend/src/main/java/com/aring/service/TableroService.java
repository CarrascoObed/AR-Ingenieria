package com.aring.service;

import com.aring.dto.InformeTableroDto;
import com.aring.dto.TableroRequest;
import com.aring.exception.ApiException;
import com.aring.model.ChecklistTablero;
import com.aring.model.ImagenTablero;
import com.aring.model.InformeTablero;
import com.aring.model.MedicionTablero;
import com.aring.model.ObservacionTablero;
import com.aring.model.Tarea;
import com.aring.model.Tecnico;
import com.aring.model.TipoTablero;
import com.aring.repository.InformeTableroRepository;
import com.aring.repository.TareaRepository;
import com.aring.repository.TecnicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TableroService {

    private final InformeTableroRepository informeTableroRepository;
    private final TareaRepository tareaRepository;
    private final TecnicoRepository tecnicoRepository;

    public TableroService(InformeTableroRepository informeTableroRepository,
                          TareaRepository tareaRepository,
                          TecnicoRepository tecnicoRepository) {
        this.informeTableroRepository = informeTableroRepository;
        this.tareaRepository = tareaRepository;
        this.tecnicoRepository = tecnicoRepository;
    }

    @Transactional
    public InformeTableroDto crear(Long tareaId, TableroRequest request, Long tecnicoIdToken) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new ApiException("La tarea no existe.", HttpStatus.NOT_FOUND));

        InformeTablero informe = new InformeTablero();
        informe.setTarea(tarea);
        try {
            informe.setTipoTablero(TipoTablero.valueOf(request.getTipoTablero()));
        } catch (IllegalArgumentException e) {
            throw new ApiException("Tipo de tablero inválido: " + request.getTipoTablero(), HttpStatus.BAD_REQUEST);
        }
        informe.setDescripcion(request.getDescripcion().trim());
        informe.setUbicacion(request.getUbicacion().trim());
        informe.setNivel(request.getNivel().trim());
        informe.setReapriete(request.getReapriete());
        informe.setAseo(request.getAseo());
        informe.setTamanio(request.getTamanioTablero());
        informe.setFechaCreacion(Instant.now());

        Long tecnicoId = tecnicoIdToken != null ? tecnicoIdToken : request.getTecnicoId();
        if (tecnicoId != null) {
            tecnicoRepository.findById(tecnicoId).ifPresent(informe::setTecnico);
        }

        request.getMediciones().forEach(m -> {
            MedicionTablero med = new MedicionTablero();
            med.setVariableId(m.getVariableId());
            med.setLabel(m.getLabel());
            med.setUnit(m.getUnit());
            med.setValor(m.getValor());
            med.setFotoBase64(m.getFotoBase64());
            informe.getMediciones().add(med);
        });

        request.getChecklist().forEach(c -> {
            ChecklistTablero item = new ChecklistTablero();
            item.setItemId(c.getItemId());
            item.setLabel(c.getLabel());
            item.setStatus(c.getStatus());
            item.setFotoBase64(c.getFotoBase64());
            informe.getChecklist().add(item);
        });

        request.getImagenes().forEach(i -> {
            ImagenTablero img = new ImagenTablero();
            img.setCategoria(i.getCategoria());
            img.setIndice(i.getIndice());
            img.setBase64(i.getBase64());
            informe.getImagenes().add(img);
        });

        int orden = 0;
        for (String obs : request.getObservaciones()) {
            ObservacionTablero observacion = new ObservacionTablero();
            observacion.setTexto(obs);
            observacion.setOrden(orden++);
            informe.getObservaciones().add(observacion);
        }

        informeTableroRepository.save(informe);
        tarea.setTablerosRealizados(tarea.getTablerosRealizados() + 1);
        tareaRepository.save(tarea);
        return InformeTableroDto.from(informe);
    }

    @Transactional(readOnly = true)
    public List<InformeTableroDto> listarPorTarea(Long tareaId) {
        return informeTableroRepository.findAllByTareaIdOrderByIdAsc(tareaId).stream()
                .map(InformeTableroDto::from).toList();
    }
}
