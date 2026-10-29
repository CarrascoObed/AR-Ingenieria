package com.aring.service;

import com.aring.exception.ApiException;
import com.aring.model.ChecklistTablero;
import com.aring.model.EstadoTarea;
import com.aring.model.ImagenTablero;
import com.aring.model.InformeTablero;
import com.aring.model.MedicionTablero;
import com.aring.model.ObservacionTablero;
import com.aring.model.Tarea;
import com.aring.model.Tecnico;
import com.aring.model.TipoTablero;
import com.aring.model.TipoTarea;
import com.aring.repository.InformeTableroRepository;
import com.aring.repository.TareaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private InformeTableroRepository informeTableroRepository;

    @InjectMocks
    private DocumentoService documentoService;

    private List<Tarea> tareas;

    @BeforeEach
    void setUp() {
        Tecnico tecnico = new Tecnico();
        tecnico.setNombre("Juan");
        tecnico.setApellido("Pérez");

        Tarea tarea = new Tarea();
        tarea.setId(1L);
        tarea.setCodigo("M-0001");
        tarea.setTipo(TipoTarea.MANTENIMIENTO);
        tarea.setDireccion("Av. Siempre Viva 123");
        tarea.setNombreRecinto("Edificio Central");
        tarea.setDescripcion("Mantención general");
        tarea.setEstado(EstadoTarea.FINALIZADA);
        tarea.setTecnicoCabecera(tecnico);
        tarea.setFechaCreacion(Instant.parse("2026-08-01T12:00:00Z"));
        tarea.setFechaInicio(Instant.parse("2026-08-01T12:05:00Z"));
        tarea.setFechaFin(Instant.parse("2026-08-01T14:30:00Z"));

        tareas = new ArrayList<>();
        tareas.add(tarea);
    }

    private void configurarTableros() {
        InformeTablero informe = new InformeTablero();
        informe.setId(10L);
        informe.setTipoTablero(TipoTablero.MONOFASICO);
        informe.setNivel("3");
        informe.setUbicacion("Pasillo A");
        informe.setDescripcion("Tablero principal");

        Tecnico responsable = new Tecnico();
        responsable.setNombre("Carlos");
        responsable.setApellido("Soto");
        informe.setTecnico(responsable);

        ObservacionTablero obs1 = new ObservacionTablero();
        obs1.setOrden(0);
        obs1.setTexto("Fusible de 10A dañado.");
        ObservacionTablero obs2 = new ObservacionTablero();
        obs2.setOrden(1);
        obs2.setTexto("Se recomienda reemplazo.");
        informe.getObservaciones().add(obs1);
        informe.getObservaciones().add(obs2);

        ImagenTablero img = new ImagenTablero();
        img.setCategoria("PREVIA");
        img.setIndice(1);
        img.setBase64(fotoBase64());
        informe.getImagenes().add(img);

        MedicionTablero med = new MedicionTablero();
        med.setLabel("Tensión fase A");
        med.setUnit("V");
        med.setValor("220");
        med.setFotoBase64(fotoBase64());
        informe.getMediciones().add(med);

        ChecklistTablero check = new ChecklistTablero();
        check.setLabel("Borne principal");
        check.setStatus("OK");
        check.setFotoBase64(fotoBase64());
        informe.getChecklist().add(check);

        when(informeTableroRepository.findAllByTareaIdOrderByIdAsc(anyLong()))
                .thenReturn(List.of(informe));
    }

    @Test
    void historialExcelGeneraArchivoConDiseno() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);

        byte[] bytes = documentoService.historialExcel(null, null, null, null, null);

        assertZip(bytes);
        String sheet = contenidoDe(bytes, "xl/worksheets/sheet1.xml");
        String strings = contenidoDe(bytes, "xl/sharedStrings.xml");
        assertTrue(sheet.contains("B2:H2"));
        assertTrue(strings.contains("Historial de tareas"));
        assertTrue(strings.contains("Técnico de cabecera"));
        assertTrue(strings.contains("Fecha finalización"));
    }

    @Test
    void finalizadasExcelGeneraRendicionDeServicios() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        configurarTableros();

        byte[] bytes = documentoService.finalizadasExcel(null, null, null);

        assertZip(bytes);
        String sheet = contenidoDe(bytes, "xl/worksheets/sheet1.xml");
        String strings = contenidoDe(bytes, "xl/sharedStrings.xml");
        assertTrue(sheet.contains("B3:I3"));
        assertTrue(strings.contains("Rendicion de servicios"));
        assertTrue(strings.contains("Tipo de tablero"));
        assertTrue(strings.contains("Tamaño tablero"));
        assertTrue(strings.contains("Nivel de criticidad"));
        assertTrue(strings.contains("Valor reparación"));
        assertTrue(strings.contains("Fusible de 10A dañado."));
        assertTrue(strings.contains("Se recomienda reemplazo."));
        assertTrue(strings.contains("Monofásico"),
                "La celda de tipo de tablero debe mostrar Monofásico/Trifásico");
        assertTrue(strings.contains("No Aplica"),
                "Para tableros monofásicos la celda de tamaño de tablero debe mostrar No Aplica");
    }

    @Test
    void finalizadasWordGeneraInformePorTablero() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        configurarTableros();

        byte[] bytes = documentoService.finalizadasWord(null, null, null);

        assertZip(bytes);
        String documentXml = contenidoDe(bytes, "word/document.xml");
        assertTrue(documentXml.contains("Informe de Mantenimiento"));
        assertTrue(documentXml.contains("Responsable Mantenimiento"));
        assertTrue(documentXml.contains("Fecha Inicio"));
        assertTrue(documentXml.contains("Fecha termino"));
        assertTrue(documentXml.contains("Detalles de revisión tablero eléctrico"));
        assertTrue(documentXml.contains("Se realiza procedimiento de mantenimiento eléctrico en tablero de distribución,"));
        assertTrue(documentXml.contains("efectuando inspección visual de sus componentes, partes eléctricas, protecciones y puntos de conexión."));
        assertTrue(documentXml.contains("Se verifica el estado general de los componentes, equipos y terminales,"));
        assertTrue(documentXml.contains("realizando las pruebas correspondientes de continuidad y resistencia de aislación."));
        assertTrue(documentXml.contains("Se detectan las siguientes observaciones en los tableros:"));
        assertTrue(documentXml.contains("Respaldo tablero eléctrico"));
        assertTrue(documentXml.contains("Fusible de 10A dañado."));
        assertTrue(documentXml.contains("Se recomienda reemplazo."));
        assertTrue(documentXml.contains("Monofásico"));
        assertTrue(documentXml.contains("No Aplica"),
                "Para tableros monofásicos el tamaño debe mostrar No Aplica");
        assertTrue(documentXml.contains("Carlos Soto"),
                "El responsable de mantenimiento debe ser el técnico que realizó el tablero");
        assertFalse(documentXml.contains("Juan Pérez"),
                "El responsable no debe ser el técnico de cabecera por defecto");
        assertTrue(documentXml.contains("wp:extent"),
                "Las imágenes deben incrustarse con un tamaño real en EMU");
    }

    @Test
    void finalizadasWordTituloUnaVezPorTarea() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);

        InformeTablero primero = new InformeTablero();
        primero.setId(10L);
        primero.setTipoTablero(TipoTablero.MONOFASICO);
        primero.setNivel("3");
        primero.setUbicacion("Pasillo A");
        primero.setDescripcion("Tablero principal");

        InformeTablero segundo = new InformeTablero();
        segundo.setId(11L);
        segundo.setTipoTablero(TipoTablero.TRIFASICO);
        segundo.setNivel("2");
        segundo.setUbicacion("Pasillo B");
        segundo.setDescripcion("Tablero secundario");

        when(informeTableroRepository.findAllByTareaIdOrderByIdAsc(anyLong()))
                .thenReturn(List.of(primero, segundo));

        byte[] bytes = documentoService.finalizadasWord(null, null, null);

        assertZip(bytes);
        String documentXml = contenidoDe(bytes, "word/document.xml");
        assertEquals(1, contarOcurrencias(documentXml, "Informe de Mantenimiento"),
                "El título del informe debe aparecer una sola vez por tarea");
        assertEquals(1, contarOcurrencias(documentXml, "Tarea M-0001"),
                "El subtítulo con el código de la tarea debe aparecer una sola vez");
        assertTrue(documentXml.contains("Tablero: Tablero principal - Pasillo A - Piso 3"),
                "El título del primer tablero debe mostrar descripción, ubicación y piso");
        assertTrue(documentXml.contains("Tablero: Tablero secundario - Pasillo B - Piso 2"),
                "El título del segundo tablero debe mostrar descripción, ubicación y piso");
    }

    @Test
    void finalizadasWordTareaNoExisteLanzaError() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> documentoService.finalizadasWord("M-9999", null, null));

        assertTrue(ex.getMessage().contains("La tarea buscada no existe."));
    }

    @Test
    void finalizadasWordTareaNoFinalizadaLanzaError() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(tareas, List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> documentoService.finalizadasWord("M-0001", null, null));

        assertTrue(ex.getMessage().contains("La tarea M-0001 no existe o aún no está finalizada."));
    }

    @Test
    void finalizadasWordTareaSinTablerosLanzaError() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        when(informeTableroRepository.findAllByTareaIdOrderByIdAsc(anyLong())).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> documentoService.finalizadasWord("M-0001", null, null));

        assertTrue(ex.getMessage().contains("La tarea M-0001 no tiene tableros inspeccionados para generar el informe."));
    }

    @Test
    void finalizadasExcelTareaSinTablerosLanzaError() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        when(informeTableroRepository.findAllByTareaIdOrderByIdAsc(anyLong())).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> documentoService.finalizadasExcel("M-0001", null, null));

        assertTrue(ex.getMessage().contains("La tarea M-0001 no tiene tableros inspeccionados para generar el informe."));
    }

    @Test
    void informeTareasWordGeneraInformeConTableros() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        configurarTableros();

        byte[] bytes = documentoService.informeTareasWord(null, null, null, null, null);

        assertZip(bytes);
        String documentXml = contenidoDe(bytes, "word/document.xml");
        assertTrue(documentXml.contains("Informe de Mantenimiento"));
        assertTrue(documentXml.contains("Tarea M-0001"));
        assertTrue(documentXml.contains("Detalles de revisión tablero eléctrico"));
        assertTrue(documentXml.contains("Se detectan las siguientes observaciones en los tableros:"));
        assertTrue(documentXml.contains("Respaldo tablero eléctrico"));
        assertTrue(documentXml.contains("Fusible de 10A dañado."));
    }

    @Test
    void informeTareasWordSoloIncluyeTareasConTableros() {
        Tecnico tecnico = new Tecnico();
        tecnico.setNombre("Juan");
        tecnico.setApellido("Pérez");

        Tarea conTableros = new Tarea();
        conTableros.setId(1L);
        conTableros.setCodigo("M-0001");
        conTableros.setTipo(TipoTarea.MANTENIMIENTO);
        conTableros.setDireccion("Av. Uno");
        conTableros.setNombreRecinto("Recinto Uno");
        conTableros.setDescripcion("Desc uno");
        conTableros.setEstado(EstadoTarea.FINALIZADA);
        conTableros.setTecnicoCabecera(tecnico);
        conTableros.setFechaCreacion(Instant.parse("2026-08-01T12:00:00Z"));

        Tarea sinTableros = new Tarea();
        sinTableros.setId(2L);
        sinTableros.setCodigo("M-0002");
        sinTableros.setTipo(TipoTarea.MANTENIMIENTO);
        sinTableros.setDireccion("Av. Dos");
        sinTableros.setNombreRecinto("Recinto Dos");
        sinTableros.setDescripcion("Desc dos");
        sinTableros.setEstado(EstadoTarea.EN_PROCESO);
        sinTableros.setTecnicoCabecera(tecnico);
        sinTableros.setFechaCreacion(Instant.parse("2026-08-02T12:00:00Z"));

        when(tareaRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(conTableros, sinTableros));

        InformeTablero informe = new InformeTablero();
        informe.setId(10L);
        informe.setTipoTablero(TipoTablero.MONOFASICO);
        informe.setNivel("3");
        informe.setUbicacion("Pasillo A");
        informe.setDescripcion("Tablero principal");
        when(informeTableroRepository.findAllByTareaIdOrderByIdAsc(1L)).thenReturn(List.of(informe));
        when(informeTableroRepository.findAllByTareaIdOrderByIdAsc(2L)).thenReturn(List.of());

        byte[] bytes = documentoService.informeTareasWord(null, null, null, null, null);

        assertZip(bytes);
        String documentXml = contenidoDe(bytes, "word/document.xml");
        assertTrue(documentXml.contains("Tarea M-0001"), "Debe incluir la tarea con tableros");
        assertFalse(documentXml.contains("Tarea M-0002"),
                "No debe incluir tareas sin tableros inspeccionados");
    }

    @Test
    void informeTareasWordSinTablerosLanzaError() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        when(informeTableroRepository.findAllByTareaIdOrderByIdAsc(anyLong())).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> documentoService.informeTareasWord(null, null, null, null, null));

        assertTrue(ex.getMessage().contains("No hay tareas con tableros inspeccionados"));
    }

    private void assertZip(byte[] bytes) {
        assertTrue(bytes.length > 100, "El archivo generado está vacío");
        assertTrue(bytes[0] == 'P' && bytes[1] == 'K', "No es un archivo ZIP/OOXML");
    }

    private int contarOcurrencias(String texto, String patron) {
        int count = 0;
        int idx = 0;
        while ((idx = texto.indexOf(patron, idx)) != -1) {
            count++;
            idx += patron.length();
        }
        return count;
    }

    private String contenidoDe(byte[] bytes, String nombreEntrada) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(nombreEntrada)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    zip.transferTo(out);
                    return out.toString(StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo leer el archivo generado", e);
        }
        return "";
    }

    private String fotoBase64() {
        try {
            BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear la imagen de prueba", e);
        }
    }
}
