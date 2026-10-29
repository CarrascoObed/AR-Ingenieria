package com.aring.service;

import com.aring.exception.ApiException;
import com.aring.model.EstadoTarea;
import com.aring.model.Tecnico;
import com.aring.repository.InformeTableroRepository;
import com.aring.repository.NotificacionRepository;
import com.aring.repository.TareaRepository;
import com.aring.repository.TecnicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TecnicoServiceTest {

    @Mock
    private TecnicoRepository tecnicoRepository;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private InformeTableroRepository informeTableroRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TecnicoService tecnicoService;

    private Tecnico tecnico;

    @BeforeEach
    void setUp() {
        tecnico = new Tecnico();
        tecnico.setId(1L);
        tecnico.setNombre("Juan");
        tecnico.setApellido("Pérez");
    }

    @Test
    void eliminarInvitadoSoloAFinalizadasOk() {
        when(tecnicoRepository.findById(1L)).thenReturn(Optional.of(tecnico));
        when(tareaRepository.existsByTecnicoCabeceraIdAndEstadoNot(1L, EstadoTarea.FINALIZADA))
                .thenReturn(false);
        when(tareaRepository.existsByTecnicosInvitadosIdAndEstadoNot(1L, EstadoTarea.FINALIZADA))
                .thenReturn(false);

        tecnicoService.eliminar(1L);

        verify(tareaRepository).liberarTareasDeTecnico(1L);
        verify(tareaRepository).eliminarInvitacionesDeTecnico(1L);
        verify(informeTableroRepository).deleteByTecnicoId(1L);
        verify(tecnicoRepository).delete(tecnico);
    }

    @Test
    void eliminarInvitadoATareaActivaLanzaError() {
        when(tecnicoRepository.findById(1L)).thenReturn(Optional.of(tecnico));
        when(tareaRepository.existsByTecnicoCabeceraIdAndEstadoNot(1L, EstadoTarea.FINALIZADA))
                .thenReturn(false);
        when(tareaRepository.existsByTecnicosInvitadosIdAndEstadoNot(1L, EstadoTarea.FINALIZADA))
                .thenReturn(true);

        assertThrows(ApiException.class, () -> tecnicoService.eliminar(1L));
    }

    @Test
    void eliminarCabeceraDeTareaEnCursoLanzaError() {
        when(tecnicoRepository.findById(1L)).thenReturn(Optional.of(tecnico));
        when(tareaRepository.existsByTecnicoCabeceraIdAndEstadoNot(1L, EstadoTarea.FINALIZADA))
                .thenReturn(true);

        assertThrows(ApiException.class, () -> tecnicoService.eliminar(1L));
    }

    @Test
    void eliminarCabeceraSoloDeFinalizadasOk() {
        when(tecnicoRepository.findById(1L)).thenReturn(Optional.of(tecnico));
        when(tareaRepository.existsByTecnicoCabeceraIdAndEstadoNot(1L, EstadoTarea.FINALIZADA))
                .thenReturn(false);
        when(tareaRepository.existsByTecnicosInvitadosIdAndEstadoNot(1L, EstadoTarea.FINALIZADA))
                .thenReturn(false);

        tecnicoService.eliminar(1L);

        verify(tareaRepository).liberarTareasDeTecnico(1L);
        verify(tecnicoRepository).delete(tecnico);
    }

    @Test
    void eliminarSinRestriccionesDejaTareasYLimpiaRelaciones() {
        when(tecnicoRepository.findById(1L)).thenReturn(Optional.of(tecnico));

        tecnicoService.eliminarSinRestricciones(1L);

        verify(tareaRepository).liberarTareasDeTecnico(1L);
        verify(tareaRepository).eliminarInvitacionesDeTecnico(1L);
        verify(informeTableroRepository).deleteByTecnicoId(1L);
        verify(notificacionRepository).borrarDeTecnico(1L);
        verify(tecnicoRepository).delete(tecnico);
    }

    @Test
    void eliminarSinRestriccionesTecnicoInexistenteLanzaError() {
        when(tecnicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> tecnicoService.eliminarSinRestricciones(99L));
    }
}
