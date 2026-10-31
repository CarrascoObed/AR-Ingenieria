package com.aring.service;

import com.aring.exception.ApiException;
import com.aring.model.ChecklistTablero;
import com.aring.model.Cliente;
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
        med.setVariableId("mono_tension");
        med.setLabel("Tensión fase A");
        med.setUnit("V");
        med.setValor("220");
        med.setFotoBase64(fotoBase64());
        informe.getMediciones().add(med);

        MedicionTablero termo = new MedicionTablero();
        termo.setVariableId("mono_termografia");
        termo.setLabel("Termografía");
        termo.setUnit("°C");
        termo.setValor("35");
        termo.setFotoBase64(fotoBase64());
        informe.getMediciones().add(termo);

        ChecklistTablero check = new ChecklistTablero();
        check.setItemId("mono_c2");
        check.setLabel("Borne principal");
        check.setStatus("OK");
        check.setFotoBase64(fotoBase64());
        informe.getChecklist().add(check);

        ChecklistTablero checkTermo = new ChecklistTablero();
        checkTermo.setItemId("mono_c9");
        checkTermo.setLabel("Termografía");
        checkTermo.setStatus("C");
        checkTermo.setFotoBase64(fotoBase64());
        informe.getChecklist().add(checkTermo);

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
    void finalizadasWordGeneraInformeConEstructuraPlantilla() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        configurarTableros();

        byte[] bytes = documentoService.finalizadasWord(null, null, null);

        assertZip(bytes);
        String documentXml = contenidoDe(bytes, "word/document.xml");
        assertTrue(documentXml.contains("Informe de mantenimiento preventivo eléctrico"),
                "La portada debe tener el título de la plantilla");
        assertTrue(documentXml.contains("Declaración de Confidencialidad"));
        assertTrue(documentXml.contains("1. Introducción."));
        assertTrue(documentXml.contains("2. Objetivos del Mantenimiento."));
        assertTrue(documentXml.contains("3.1 Instrumentación para el servicio"));
        assertTrue(documentXml.contains("3.2 Procedimiento de mantenimiento."));
        assertTrue(documentXml.contains("3.3 Pautas de revisión para tableros (captura de datos tipo Check list)"));
        assertTrue(documentXml.contains("4. Levantamiento, valorización y condiciones de pago."));
        assertTrue(documentXml.contains("5. Respaldo fotográfico de servicio realizado."));
        assertTrue(documentXml.contains("6. Respaldo fotográfico termografía"));
        assertTrue(documentXml.contains("7. Resumen de mantenimiento y observaciones."));
        assertTrue(documentXml.contains("8. Agradecimientos."));

        // Sección 3.3 agrupada por tablero con su información real
        assertTrue(documentXml.contains("Tablero: Tablero principal - Pasillo A - Piso 3"));
        assertTrue(documentXml.contains("Carlos Soto"),
                "El responsable de mantenimiento debe ser el técnico que realizó el tablero");
        assertTrue(documentXml.contains("Fusible de 10A dañado."));
        assertTrue(documentXml.contains("Se recomienda reemplazo."));
        assertTrue(documentXml.contains("Monofásico"));
        assertTrue(documentXml.contains("No Aplica"),
                "Para tableros monofásicos el tamaño debe mostrar No Aplica");

        // Pies de contexto para las fotos
        assertTrue(documentXml.contains("Tablero principal - Pasillo A - Piso 3 - Evidencia previa"),
                "Las fotos previas/posteriores deben presentarse en 3.3 con pie");
        assertTrue(documentXml.contains("Medición Tensión fase A"),
                "Las fotos de mediciones deben presentarse en 5 con pie");
        assertTrue(documentXml.contains("Evidencia checklist: Borne principal"),
                "Las evidencias de checklist deben presentarse en 5 con pie");
        assertTrue(documentXml.contains("wp:extent"),
                "Las imágenes deben incrustarse con un tamaño real en EMU");
    }

    @Test
    void finalizadasWordGeneraTermografiaEnSeccionSeis() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        configurarTableros();

        byte[] bytes = documentoService.finalizadasWord(null, null, null);

        String documentXml = contenidoDe(bytes, "word/document.xml");
        // Las fotos termográficas (medición mono_termografia y checklist mono_c9 "Termografía")
        // se presentan en la sección 6, identificadas con tablero y contexto.
        assertTrue(documentXml.contains("Piso 3 - Termografía (35 °C)"),
                "La foto de la medición de termografía debe ir en la sección 6 con su contexto");
        assertTrue(documentXml.contains("Evidencia checklist: Termografía"),
                "La evidencia del checklist de termografía debe ir en la sección 6");
        // En la sección 5 no deben aparecer como "Medición Termografía" (la clasificación es por metadatos).
        assertFalse(documentXml.contains("Medición Termografía"));
    }

    @Test
    void finalizadasWordNoIncluyeCobrosNiEspacioReservado() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        configurarTableros();

        byte[] bytes = documentoService.finalizadasWord(null, null, null);

        String documentXml = contenidoDe(bytes, "word/document.xml");
        int inicioSeccion4 = documentXml.indexOf("4. Levantamiento, valorización y condiciones de pago.");
        int finSeccion4 = documentXml.indexOf("5. Respaldo fotográfico de servicio realizado.");
        assertTrue(inicioSeccion4 >= 0 && finSeccion4 > inicioSeccion4,
                "La sección 4 debe existir y la 5 debe ir después");
        String contenidoSeccion4 = documentXml.substring(inicioSeccion4, finSeccion4);
        assertFalse(contenidoSeccion4.contains("wp:extent"),
                "La sección 4 no debe insertar ninguna imagen de cobros/valorización");
        assertFalse(contenidoSeccion4.contains("w:tbl>"),
                "La sección 4 no debe dejar espacios de tabla vacíos");
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
        assertEquals(1, contarOcurrencias(documentXml, "Informe de mantenimiento preventivo eléctrico"),
                "La portada debe aparecer una sola vez por tarea");
        assertEquals(1, contarOcurrencias(documentXml, "Tarea M-0001"),
                "La tarea debe identificarse una sola vez");
        assertTrue(documentXml.contains("Tablero: Tablero principal - Pasillo A - Piso 3"),
                "El primer tablero debe mostrar descripción, ubicación y piso");
        assertTrue(documentXml.contains("Tablero: Tablero secundario - Pasillo B - Piso 2"),
                "El segundo tablero debe mostrar descripción, ubicación y piso");
    }

    @Test
    void finalizadasWordPortadaUsaDatosDelCliente() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        configurarTableros();

        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cliente.setNombreRecinto("Edificio Costanera");
        cliente.setDireccion("Av. Costanera 480, Santiago");
        cliente.setImagenBase64(fotoBase64());
        tareas.get(0).setCliente(cliente);

        byte[] bytes = documentoService.finalizadasWord(null, null, null);

        String documentXml = contenidoDe(bytes, "word/document.xml");
        assertTrue(documentXml.contains("Edificio Costanera"),
                "La portada debe usar el nombre del recinto del Cliente asociado");
        assertTrue(documentXml.contains("Av. Costanera 480, Santiago"),
                "La portada debe usar la dirección del Cliente asociado");
        assertTrue(documentXml.contains("Elaborado por Juan Pérez"),
                "La portada debe indicar al técnico de cabecera");
        assertTrue(documentXml.contains("Agosto del 2026"),
                "La portada debe mostrar el mes y año reales del mantenimiento");
        assertTrue(documentXml.contains("Santiago, agosto 2026"),
                "La portada debe cerrar con ciudad y mes del servicio");
    }

    @Test
    void finalizadasWordTareaSinClienteUsaDatosDeTarea() {
        when(tareaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tareas);
        configurarTableros();

        byte[] bytes = documentoService.finalizadasWord(null, null, null);

        String documentXml = contenidoDe(bytes, "word/document.xml");
        assertTrue(documentXml.contains("Edificio Central"),
                "Sin Cliente, la portada debe usar los datos de la tarea");
        assertTrue(documentXml.contains("Av. Siempre Viva 123"));
        assertFalse(documentXml.contains("null"),
                "El documento no debe contener valores nulos");
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
        assertTrue(documentXml.contains("Informe de mantenimiento preventivo eléctrico"));
        assertTrue(documentXml.contains("Tarea M-0001"));
        assertTrue(documentXml.contains("3.3 Pautas de revisión para tableros (captura de datos tipo Check list)"));
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
