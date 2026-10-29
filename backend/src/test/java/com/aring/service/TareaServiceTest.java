package com.aring.service;

import com.aring.dto.TareaRequest;
import com.aring.exception.ApiException;
import com.aring.model.EstadoTarea;
import com.aring.model.SecuenciaTarea;
import com.aring.model.Tarea;
import com.aring.model.Tecnico;
import com.aring.model.TipoTarea;
import com.aring.repository.InformeTableroRepository;
import com.aring.repository.NotificacionRepository;
import com.aring.repository.SecuenciaTareaRepository;
import com.aring.repository.TareaRepository;
import com.aring.repository.TecnicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TareaServiceTest {

    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private TecnicoRepository tecnicoRepository;
    @Mock
    private SecuenciaTareaRepository secuenciaTareaRepository;
    @Mock
    private InformeTableroRepository informeTableroRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private TareaService tareaService;

    private SecuenciaTarea secuencia;

    @BeforeEach
    void setUp() {
        secuencia = new SecuenciaTarea();
        secuencia.setId(1L);
        secuencia.setTipo(TipoTarea.MANTENIMIENTO);
        secuencia.setUltimoNumero(0L);
    }

    @Test
    void generarCodigoGeneraIdConPrefijoM() {
        when(secuenciaTareaRepository.findByTipoParaActualizar(TipoTarea.MANTENIMIENTO))
                .thenReturn(Optional.of(secuencia));

        String codigo = tareaService.generarCodigo(TipoTarea.MANTENIMIENTO);

        assertEquals("M-0001", codigo);
        assertEquals(1L, secuencia.getUltimoNumero());
    }

    @Test
    void generarCodigoSecuencial() {
        when(secuenciaTareaRepository.findByTipoParaActualizar(TipoTarea.MANTENIMIENTO))
                .thenReturn(Optional.of(secuencia));

        assertEquals("M-0001", tareaService.generarCodigo(TipoTarea.MANTENIMIENTO));
        assertEquals("M-0002", tareaService.generarCodigo(TipoTarea.MANTENIMIENTO));
    }

    @Test
    void soloSePermiteCrearMantenimiento() {
        TareaRequest request = new TareaRequest();
        request.setTipo(TipoTarea.REPARACION);
        request.setDireccion("Calle 1");
        request.setNombreRecinto("Recinto");
        request.setDescripcion("Desc");
        request.setTecnicoCabeceraId(1L);

        assertThrows(ApiException.class, () -> tareaService.crear(request));
    }

    @Test
    void eliminarTareaIniciadaLanzaError() {
        Tarea tarea = new Tarea();
        tarea.setId(1L);
        tarea.setEstado(EstadoTarea.EN_PROCESO);
        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));

        assertThrows(ApiException.class, () -> tareaService.eliminar(1L));
    }

    @Test
    void eliminarTareaNoIniciadaOk() {
        Tarea tarea = new Tarea();
        tarea.setId(1L);
        tarea.setEstado(EstadoTarea.NO_INICIADA);
        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));

        tareaService.eliminar(1L);

        verify(tareaRepository).delete(tarea);
    }

    @Test
    void eliminarCualquieraLimpiaRelacionesYBorraEnCualquierEstado() {
        Tarea tarea = new Tarea();
        tarea.setId(1L);
        tarea.setEstado(EstadoTarea.FINALIZADA);
        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));

        tareaService.eliminarCualquiera(1L);

        verify(informeTableroRepository).deleteByTareaId(1L);
        verify(notificacionRepository).borrarDeTarea(1L);
        verify(tareaRepository).eliminarInvitacionesDeTarea(1L);
        verify(tareaRepository).delete(tarea);
    }

    @Test
    void eliminarCualquieraTareaInexistenteLanzaError() {
        when(tareaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> tareaService.eliminarCualquiera(99L));
    }

    @Test
    void eliminarTodasBorraTodoYReiniciaSecuencia() {
        secuencia.setUltimoNumero(10L);
        when(secuenciaTareaRepository.findAll()).thenReturn(List.of(secuencia));

        tareaService.eliminarTodas();

        verify(informeTableroRepository).deleteAll();
        verify(notificacionRepository).deleteAll();
        verify(tareaRepository).eliminarTodasLasInvitaciones();
        verify(tareaRepository).deleteAll();
        assertEquals(0L, secuencia.getUltimoNumero(), "La secuencia debe reiniciarse a 0");
    }

    @Test
    void cambiarEstadoAFinalizadaCreaNotificacion() {
        Tecnico tecnico = new Tecnico();
        tecnico.setNombre("Juan");
        tecnico.setApellido("Pérez");

        Tarea tarea = new Tarea();
        tarea.setId(1L);
        tarea.setCodigo("M-0001");
        tarea.setEstado(EstadoTarea.EN_PROCESO);
        tarea.setTecnicoCabecera(tecnico);
        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
        when(tareaRepository.save(any(Tarea.class))).thenAnswer(inv -> inv.getArgument(0));

        com.aring.dto.EstadoRequest request = new com.aring.dto.EstadoRequest();
        request.setEstado(EstadoTarea.FINALIZADA);

        Tarea resultado = tareaService.cambiarEstado(1L, request);

        assertEquals(EstadoTarea.FINALIZADA, resultado.getEstado());
        verify(notificacionService).crear(
                "Tarea M-0001 finalizada por Juan Pérez.",
                com.aring.model.TipoNotificacion.TAREA_FINALIZADA,
                1L,
                null);
    }

    @Test
    void actualizarTecnicoSoloSiNoIniciada() {
        Tarea tarea = new Tarea();
        tarea.setId(1L);
        tarea.setEstado(EstadoTarea.NO_INICIADA);
        Tecnico tecnico = new Tecnico();
        tecnico.setId(2L);
        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
        when(tecnicoRepository.findById(2L)).thenReturn(Optional.of(tecnico));
        when(tareaRepository.save(any(Tarea.class))).thenAnswer(inv -> inv.getArgument(0));

        com.aring.dto.TareaUpdateRequest request = new com.aring.dto.TareaUpdateRequest();
        request.setTecnicoCabeceraId(2L);

        Tarea resultado = tareaService.actualizarTecnico(1L, request);

        assertEquals(tecnico, resultado.getTecnicoCabecera());
    }
}
