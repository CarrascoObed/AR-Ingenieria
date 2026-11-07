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
import com.aring.repository.InformeTableroRepository;
import com.aring.repository.TareaRepository;
import com.aring.spec.TareaSpecifications;
import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowHeightRule;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblCellMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DocumentoService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Santiago"));

    private static final DateTimeFormatter FORMATO_MES_ANO = DateTimeFormatter
            .ofPattern("MMMM 'del' yyyy", new Locale("es", "CL"))
            .withZone(ZoneId.of("America/Santiago"));

    private static final DateTimeFormatter FORMATO_SANTIAGO_MES_ANO = DateTimeFormatter
            .ofPattern("'Santiago, 'MMMM yyyy", new Locale("es", "CL"))
            .withZone(ZoneId.of("America/Santiago"));

    private static final DateTimeFormatter FORMATO_ARCHIVO_MES_ANO = DateTimeFormatter
            .ofPattern("MMMM-yyyy", new Locale("es", "CL"))
            .withZone(ZoneId.of("America/Santiago"));

    // IDs usados por las versiones de Android: el tablero trifásico llama
    // "tri_temp" a la medición cuya evidencia es la fotografía termográfica.
    private static final Set<String> VARIABLES_TERMOGRAFIA = Set.of(
            "mono_termografia", "tri_termografia", "tri_temp");
    private static final Set<String> ITEMS_TERMOGRAFIA = Set.of("mono_c9", "tri_c21");

    private static final long ANCHO_PORTADA_EMU = cmToEmu(15.59);
    private static final long ALTO_PORTADA_EMU = cmToEmu(12.44);

    private final TareaRepository tareaRepository;
    private final InformeTableroRepository informeTableroRepository;
    private final AtomicInteger contadorFotosWord = new AtomicInteger();

    public DocumentoService(TareaRepository tareaRepository, InformeTableroRepository informeTableroRepository) {
        this.tareaRepository = tareaRepository;
        this.informeTableroRepository = informeTableroRepository;
    }

    // ------------------------------------------------------------------
    // Excel: Historial de tareas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] historialExcel(String codigo, String nombreTecnico, String nombreRecinto,
                                 Instant fechaDesde, Instant fechaHasta) {
        List<Tarea> tareas = buscarTareas(codigo, nombreTecnico, nombreRecinto, null, fechaDesde, fechaHasta);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Historial");
            sheet.setDisplayGridlines(false);

            String[] cabeceras = {"ID Tarea", "Técnico de cabecera", "Dirección", "Nombre recinto",
                    "Fecha inicio", "Fecha finalización", "Estado"};
            int[] anchos = {12, 24, 36, 28, 19, 19, 16};
            for (int i = 0; i < cabeceras.length; i++) {
                sheet.setColumnWidth(i + 1, anchos[i] * 256);
            }

            // Título B2:H2
            Row filaTitulo = sheet.createRow(1);
            filaTitulo.setHeightInPoints(28);
            CellStyle estiloTitulo = estiloTituloHistorial(workbook);
            for (int c = 1; c <= 7; c++) {
                filaTitulo.createCell(c).setCellStyle(estiloTitulo);
            }
            filaTitulo.getCell(1).setCellValue("Historial de tareas");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 7));

            // Cabecera B3:H3
            Row filaCabecera = sheet.createRow(2);
            filaCabecera.setHeightInPoints(22);
            CellStyle estiloCabecera = estiloCabeceraHistorial(workbook);
            for (int i = 0; i < cabeceras.length; i++) {
                Cell cell = filaCabecera.createCell(i + 1);
                cell.setCellValue(cabeceras[i]);
                cell.setCellStyle(estiloCabecera);
            }

            // Datos desde la fila 4
            if (tareas.isEmpty()) {
                Row row = sheet.createRow(3);
                escribirCelda(row, 1,
                        "No hay tareas registradas con los filtros seleccionados.",
                        estiloDatoHistorial(workbook, false, false));
                sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 7));
            } else {
                CellStyle datoTexto = estiloDatoHistorial(workbook, false, false);
                CellStyle datoTextoAlt = estiloDatoHistorial(workbook, false, true);
                CellStyle datoCentrado = estiloDatoHistorial(workbook, true, false);
                CellStyle datoCentradoAlt = estiloDatoHistorial(workbook, true, true);

                int fila = 3;
                boolean alternar = false;
                for (Tarea t : tareas) {
                    Row row = sheet.createRow(fila++);
                    escribirCelda(row, 1, t.getCodigo(), alternar ? datoCentradoAlt : datoCentrado);
                    escribirCelda(row, 2, nombreCompletoTecnico(t), alternar ? datoTextoAlt : datoTexto);
                    escribirCelda(row, 3, t.getDireccion(), alternar ? datoTextoAlt : datoTexto);
                    escribirCelda(row, 4, t.getNombreRecinto(), alternar ? datoTextoAlt : datoTexto);
                    escribirCelda(row, 5, formatear(t.getFechaInicio()), alternar ? datoCentradoAlt : datoCentrado);
                    escribirCelda(row, 6, formatear(t.getFechaFin()), alternar ? datoCentradoAlt : datoCentrado);
                    escribirCelda(row, 7, estadoTarea(t), alternar ? datoCentradoAlt : datoCentrado);
                    alternar = !alternar;
                }
                sheet.createFreezePane(0, 3);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException("No se pudo generar el archivo Excel: " + e.getMessage(),
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------------------------------------------------------
    // Excel: Rendición de servicios
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] finalizadasExcel(String codigo, String nombreTecnico, String nombreRecinto) {
        List<Tarea> tareas = tareasFinalizadasConDatos(codigo, nombreTecnico, nombreRecinto);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Rendición de servicios");
            sheet.setDisplayGridlines(false);

            int[] anchos = {7, 20, 33, 18, 16, 50, 22, 22};
            for (int i = 0; i < anchos.length; i++) {
                sheet.setColumnWidth(i + 1, anchos[i] * 256);
            }

            CellStyle estiloTitulo = estiloTituloHistorial(workbook);
            CellStyle estiloCabecera = estiloCabeceraHistorial(workbook);
            CellStyle datoTexto = estiloDatoHistorial(workbook, false, false);
            CellStyle datoTextoAlt = estiloDatoHistorial(workbook, false, true);
            CellStyle datoCentrado = estiloDatoHistorial(workbook, true, false);
            CellStyle datoCentradoAlt = estiloDatoHistorial(workbook, true, true);
            CellStyle datoMoneda = estiloDatoMonedaHistorial(workbook, false);
            CellStyle datoMonedaAlt = estiloDatoMonedaHistorial(workbook, true);

            int fila = 2;
            boolean primerBloque = true;
            for (Tarea tarea : tareas) {
                List<InformeTablero> informes = informeTableroRepository.findAllByTareaIdOrderByIdAsc(tarea.getId());
                if (informes.isEmpty()) {
                    continue;
                }
                if (!primerBloque) {
                    fila += 2;
                }
                primerBloque = false;

                // Título B3:I3
                Row filaTitulo = sheet.createRow(fila++);
                filaTitulo.setHeightInPoints(28);
                for (int c = 1; c <= 8; c++) {
                    filaTitulo.createCell(c).setCellStyle(estiloTitulo);
                }
                filaTitulo.getCell(1).setCellValue("Rendicion de servicios");
                sheet.addMergedRegion(new CellRangeAddress(fila - 1, fila - 1, 1, 8));

                // Cabecera B4:I4
                String[] cabeceras = {"Piso", "Ubicación", "Descripcion", "Tipo de tablero", "Tamaño tablero", "Observaciones",
                        "Nivel de criticidad", "Valor reparación"};
                Row filaCabecera = sheet.createRow(fila++);
                filaCabecera.setHeightInPoints(22);
                for (int i = 0; i < cabeceras.length; i++) {
                    Cell cell = filaCabecera.createCell(i + 1);
                    cell.setCellValue(cabeceras[i]);
                    cell.setCellStyle(estiloCabecera);
                }

                // Datos: una fila por observación, repitiendo piso/ubicación/descripción/tipo y tamaño de tablero
                boolean alternar = false;
                for (InformeTablero informe : informes) {
                    String tipoTablero = tipoTableroRendicion(informe);
                    String tamanioTablero = tamanioTableroRendicion(informe);
                    List<ObservacionTablero> observaciones = new ArrayList<>(informe.getObservaciones());
                    observaciones.sort(Comparator.comparingInt(ObservacionTablero::getOrden));
                    if (observaciones.isEmpty()) {
                        Row row = sheet.createRow(fila++);
                        escribirCelda(row, 1, informe.getNivel(), alternar ? datoCentradoAlt : datoCentrado);
                        escribirCelda(row, 2, informe.getUbicacion(), alternar ? datoTextoAlt : datoTexto);
                        escribirCelda(row, 3, informe.getDescripcion(), alternar ? datoTextoAlt : datoTexto);
                        escribirCelda(row, 4, tipoTablero, alternar ? datoTextoAlt : datoTexto);
                        escribirCelda(row, 5, tamanioTablero, alternar ? datoTextoAlt : datoTexto);
                        escribirCelda(row, 6, "", alternar ? datoTextoAlt : datoTexto);
                        escribirCelda(row, 7, "", alternar ? datoCentradoAlt : datoCentrado);
                        escribirCelda(row, 8, "", alternar ? datoMonedaAlt : datoMoneda);
                        alternar = !alternar;
                    } else {
                        for (ObservacionTablero obs : observaciones) {
                            Row row = sheet.createRow(fila++);
                            escribirCelda(row, 1, informe.getNivel(), alternar ? datoCentradoAlt : datoCentrado);
                            escribirCelda(row, 2, informe.getUbicacion(), alternar ? datoTextoAlt : datoTexto);
                            escribirCelda(row, 3, informe.getDescripcion(), alternar ? datoTextoAlt : datoTexto);
                            escribirCelda(row, 4, tipoTablero, alternar ? datoTextoAlt : datoTexto);
                            escribirCelda(row, 5, tamanioTablero, alternar ? datoTextoAlt : datoTexto);
                            escribirCelda(row, 6, obs.getTexto(), alternar ? datoTextoAlt : datoTexto);
                            escribirCelda(row, 7, "", alternar ? datoCentradoAlt : datoCentrado);
                            escribirCelda(row, 8, "", alternar ? datoMonedaAlt : datoMoneda);
                            alternar = !alternar;
                        }
                    }
                }
            }

            if (primerBloque) {
                Row row = sheet.createRow(fila);
                escribirCelda(row, 1,
                        "No hay tareas finalizadas con tableros registrados para los filtros seleccionados.",
                        datoTexto);
                sheet.addMergedRegion(new CellRangeAddress(fila, fila, 1, 8));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException("No se pudo generar el archivo Excel: " + e.getMessage(),
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------------------------------------------------------
    // Word: Informe de tarea finalizada
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] finalizadasWord(String codigo, String nombreTecnico, String nombreRecinto) {
        List<Tarea> tareas = tareasFinalizadasConDatos(codigo, nombreTecnico, nombreRecinto);
        return generarInformeWord(tareas);
    }

    @Transactional(readOnly = true)
    public byte[] documentosFinalizadasZip(int anio, Integer mes) {
        if (mes != null && (mes < 1 || mes > 12)) {
            throw new ApiException("El mes debe estar entre 1 y 12.",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        List<Tarea> tareas = buscarTareas(null, null, null, EstadoTarea.FINALIZADA, null, null).stream()
                .filter(tarea -> fechaDentroDelPeriodo(tarea, anio, mes))
                .filter(tarea -> !informeTableroRepository.findAllByTareaIdOrderByIdAsc(tarea.getId()).isEmpty())
                .sorted(Comparator.comparing(this::fechaParaDocumento,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (tareas.isEmpty()) {
            throw new ApiException("No hay documentos de tareas finalizadas para el período seleccionado.",
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Tarea tarea : tareas) {
                String nombreBase = nombreBaseArchivoZip(tarea);
                agregarEntradaZip(zip, nombreBase + ".xlsx", finalizadasExcel(tarea.getCodigo(), null, null));
                agregarEntradaZip(zip, nombreBase + ".docx", informeTareaWord(tarea.getId()).bytes());
            }
            zip.finish();
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new ApiException("No se pudieron generar los documentos del período: " + e.getMessage(),
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean fechaDentroDelPeriodo(Tarea tarea, int anio, Integer mes) {
        Instant fecha = fechaParaDocumento(tarea);
        if (fecha == null) {
            return false;
        }
        var fechaLocal = fecha.atZone(ZoneId.of("America/Santiago"));
        return fechaLocal.getYear() == anio && (mes == null || fechaLocal.getMonthValue() == mes);
    }

    private Instant fechaParaDocumento(Tarea tarea) {
        return tarea.getFechaFin() != null ? tarea.getFechaFin()
                : tarea.getFechaInicio() != null ? tarea.getFechaInicio() : tarea.getFechaCreacion();
    }

    private String nombreBaseArchivoZip(Tarea tarea) {
        String recinto = texto(tarea.getNombreRecinto()).trim()
                .replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "-")
                .replaceAll("[. ]+$", "");
        if (recinto.isBlank()) {
            recinto = "Tarea";
        }
        Instant fecha = fechaParaDocumento(tarea);
        String mesAno = fecha == null ? "" : capitalizar(FORMATO_ARCHIVO_MES_ANO.format(fecha));
        return recinto + (mesAno.isBlank() ? "" : "-" + mesAno) + "-" + tarea.getCodigo();
    }

    private void agregarEntradaZip(ZipOutputStream zip, String nombre, byte[] contenido) throws java.io.IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido);
        zip.closeEntry();
    }

    public record DatosArchivoTarea(
            String codigo,
            String nombreRecinto,
            Instant fechaCreacion,
            Instant fechaInicio,
            Instant fechaFin) {
    }

    public DatosArchivoTarea datosArchivoTarea(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return tareaRepository.findByCodigoIgnoreCase(codigo.trim())
                .map(tarea -> new DatosArchivoTarea(
                        tarea.getCodigo(),
                        tarea.getNombreRecinto(),
                        tarea.getFechaCreacion(),
                        tarea.getFechaInicio(),
                        tarea.getFechaFin()))
                .orElse(null);
    }

    public record TareaInforme(Long id, String codigo, String nombreRecinto, String direccion,
                               String nombreTecnico, Instant fecha) {
    }

    @Transactional(readOnly = true)
    public List<String> sugerirEdificios(String consulta) {
        if (consulta == null || consulta.trim().length() < 2) {
            return List.of();
        }
        return tareaRepository.sugerirEdificiosFinalizados(consulta.trim(), PageRequest.of(0, 10));
    }

    @Transactional(readOnly = true)
    public List<TareaInforme> buscarTareasFinalizadasParaInforme(String nombreRecinto, String direccion,
                                                                  String nombreTecnico, Instant fechaDesde,
                                                                  Instant fechaHasta) {
        if ((nombreRecinto == null || nombreRecinto.isBlank())
                && (direccion == null || direccion.isBlank())
                && (nombreTecnico == null || nombreTecnico.isBlank())
                && fechaDesde == null && fechaHasta == null) {
            return List.of();
        }
        Specification<Tarea> spec = TareaSpecifications.documentosFinalizados(
                nombreRecinto, direccion, nombreTecnico, fechaDesde, fechaHasta);
        return tareaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaFin")).stream()
                .map(tarea -> new TareaInforme(
                        tarea.getId(),
                        tarea.getCodigo(),
                        tarea.getNombreRecinto(),
                        tarea.getDireccion(),
                        tarea.getTecnicoCabecera() == null ? "Sin técnico asignado"
                                : tarea.getTecnicoCabecera().getNombre() + " "
                                        + tarea.getTecnicoCabecera().getApellido(),
                        tarea.getFechaFin() != null ? tarea.getFechaFin()
                                : tarea.getFechaInicio() != null ? tarea.getFechaInicio() : tarea.getFechaCreacion()))
                .toList();
    }

    public record InformeTarea(byte[] bytes, DatosArchivoTarea tarea) {
    }

    @Transactional(readOnly = true)
    public InformeTarea informeTareaWord(Long tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new ApiException("La tarea no existe.",
                        org.springframework.http.HttpStatus.NOT_FOUND));
        if (informeTableroRepository.findAllByTareaIdOrderByIdAsc(tarea.getId()).isEmpty()) {
            throw new ApiException("La tarea " + tarea.getCodigo()
                            + " no tiene tableros inspeccionados para generar el informe.",
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return new InformeTarea(
                generarInformeWord(List.of(tarea)),
                new DatosArchivoTarea(
                        tarea.getCodigo(),
                        tarea.getNombreRecinto(),
                        tarea.getFechaCreacion(),
                        tarea.getFechaInicio(),
                        tarea.getFechaFin()));
    }

    @Transactional(readOnly = true)
    public byte[] informeTareasWord(String codigo, String nombreTecnico, String nombreRecinto,
                                    Instant fechaDesde, Instant fechaHasta) {
        List<Tarea> tareas = buscarTareas(codigo, nombreTecnico, nombreRecinto, null, fechaDesde, fechaHasta);
        List<Tarea> conDatos = tareas.stream()
                .filter(t -> !informeTableroRepository.findAllByTareaIdOrderByIdAsc(t.getId()).isEmpty())
                .toList();
        if (conDatos.isEmpty()) {
            String mensaje = codigo != null && !codigo.isBlank()
                    ? "La tarea " + codigo + " no tiene tableros inspeccionados para generar el informe."
                    : "No hay tareas con tableros inspeccionados para los filtros seleccionados.";
            throw new ApiException(mensaje, org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return generarInformeWord(conDatos);
    }

    private byte[] generarInformeWord(List<Tarea> tareas) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            boolean primerBloque = true;
            boolean hayInformes = false;
            for (Tarea tarea : tareas) {
                List<InformeTablero> informes = informeTableroRepository.findAllByTareaIdOrderByIdAsc(tarea.getId());
                if (informes.isEmpty()) {
                    continue;
                }
                if (!primerBloque) {
                    saltoPagina(document);
                }
                primerBloque = false;
                agregarInformeTarea(document, tarea, informes);
                hayInformes = true;
            }

            if (!hayInformes) {
                parrafoCuerpo(document,
                        "No se encontraron tableros inspeccionados para las tareas seleccionadas.");
            }

            configurarPagina(document);
            if (hayInformes) {
                agregarEncabezadoLogo(document);
            }

            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException("No se pudo generar el archivo Word: " + e,
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void agregarInformeTarea(XWPFDocument document, Tarea tarea, List<InformeTablero> informes) {
        agregarPortada(document, tarea);
        saltoPagina(document);
        agregarIndice(document);
        saltoPagina(document);
        agregarContenidoGeneral(document, tarea);

        saltoPagina(document);
        tituloSeccion(document, "3.3 Pautas de revisión para tableros");
        for (InformeTablero informe : informes) {
            agregarSeccionTablero(document, tarea, informe);
        }

        tituloSeccion(document, "4. Levantamiento, valorización y condiciones de pago.");
        parrafoCuerpo(document,
                "Del mantenimiento realizado se desprende el listado de los activos intervenidos; el detalle "
                        + "valorizado del servicio y sus condiciones de pago se acompañan como anexo de este informe.");

        tituloSeccion(document, "6. Respaldo fotográfico termografía");
        agregarRespaldoTermografia(document, informes);

        tituloSeccion(document, "7. Resumen de mantenimiento y observaciones.");
        agregarResumenMantenimiento(document, informes);

        tituloSeccion(document, "8. Agradecimientos.");
        parrafoCuerpo(document,
                "Agradeciendo su interés por nuestros productos y/o servicios, quedamos a vuestra disposición, "
                        + "en caso de requerir mayor información, no dude en contactar a nuestra empresa al correo: "
                        + "montajesindustriales.a@gmail.com, o directamente a nuestro jefe de operación.");
        agregarLineasEnBlanco(document, 5);
        parrafoCuerpo(document, "Saludos cordiales");
        parrafoCuerpo(document, "Luis Rojas Liberona");
        parrafoCuerpo(document, "Gerente general");
        parrafoCuerpo(document, "AR Ingeniería Eléctrica SpA");
        parrafoCuerpo(document, "Móvil +569 64200260");
    }

    private record FotoInforme(byte[] bytes, String pie) {
    }

    private void agregarPortada(XWPFDocument document, Tarea tarea) {
        Cliente cliente = tarea.getCliente();
        String recinto = cliente != null && !texto(cliente.getNombreRecinto()).isBlank()
                ? texto(cliente.getNombreRecinto())
                : texto(tarea.getNombreRecinto());
        String direccion = cliente != null && !texto(cliente.getDireccion()).isBlank()
                ? texto(cliente.getDireccion())
                : texto(tarea.getDireccion());
        Instant fechaCierre = tarea.getFechaFin() != null ? tarea.getFechaFin()
                : tarea.getFechaInicio() != null ? tarea.getFechaInicio() : tarea.getFechaCreacion();

        tituloPortada(document, "Informe de mantenimiento preventivo eléctrico", 16);
        if (!recinto.isBlank()) {
            subtituloPortada(document, recinto, true);
        }
        if (!direccion.isBlank()) {
            subtituloPortada(document, direccion, false);
        }
        subtituloPortada(document, "Mantenimiento eléctrico", false);
        if (fechaCierre != null) {
            subtituloPortada(document, capitalizar(FORMATO_MES_ANO.format(fechaCierre)), false);
        }

        byte[] edificio = cliente != null ? decodificar(cliente.getImagenBase64()) : null;
        if (edificio != null) {
            agregarImagenPortada(document, edificio);
        }

        Tecnico cabecera = tarea.getTecnicoCabecera();
        if (cabecera != null) {
            subtituloPortada(document, "Elaborado por " + cabecera.getNombre() + " " + cabecera.getApellido(), false);
        }
        if (fechaCierre != null) {
            subtituloPortada(document, FORMATO_SANTIAGO_MES_ANO.format(fechaCierre), false);
        }
    }

    private void tituloPortada(XWPFDocument document, String valor, int tamano) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setAlignment(ParagraphAlignment.CENTER);
        parrafo.setSpacingBefore(1200);
        parrafo.setSpacingAfter(0);
        XWPFRun run = parrafo.createRun();
        run.setText(valor);
        run.setBold(false);
        run.setFontSize(tamano);
        run.setFontFamily("Arial");
        run.setColor("2E74B5");
    }

    private void subtituloPortada(XWPFDocument document, String valor, boolean negrita) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setAlignment(ParagraphAlignment.CENTER);
        parrafo.setSpacingAfter(60);
        XWPFRun run = parrafo.createRun();
        run.setText(valor);
        run.setBold(negrita);
        run.setFontFamily("Arial");
        run.setFontSize(negrita ? 16 : 14);
        run.setColor("1B6DC1");
    }

    private void agregarIndice(XWPFDocument document) {
        XWPFParagraph titulo = document.createParagraph();
        titulo.setSpacingBefore(120);
        titulo.setSpacingAfter(200);
        XWPFRun run = titulo.createRun();
        run.setText("Índice");
        run.setBold(true);
        run.setFontFamily("Arial");
        run.setFontSize(14);
        run.setColor("1B6DC1");

        String[] entradas = {
                "Declaración de Confidencialidad",
                "1. Introducción.",
                "2. Objetivos del Mantenimiento.",
                "3. Procedimiento general del mantenimiento.",
                "3.1 Instrumentación para el servicio",
                "3.2 Procedimiento de mantenimiento.",
                "3.3 Pautas de revisión para tableros",
                "4. Levantamiento, valorización y condiciones de pago.",
                "6. Respaldo fotográfico termografía",
                "7. Resumen de mantenimiento y observaciones.",
                "8. Agradecimientos."
        };
        for (String entrada : entradas) {
            XWPFParagraph parrafo = document.createParagraph();
            parrafo.setSpacingAfter(60);
            XWPFRun runEntrada = parrafo.createRun();
            runEntrada.setText(entrada);
            runEntrada.setFontFamily("Arial");
            runEntrada.setFontSize(10);
        }
    }

    private void agregarContenidoGeneral(XWPFDocument document, Tarea tarea) {
        Cliente cliente = tarea.getCliente();
        String recinto = cliente != null && !texto(cliente.getNombreRecinto()).isBlank()
                ? texto(cliente.getNombreRecinto())
                : texto(tarea.getNombreRecinto());

        tituloSeccion(document, "Declaración de Confidencialidad");
        parrafoCuerpo(document,
                "La presente documentación y material de soporte contiene información confidencial. "
                        + "Este material puede ser impreso o fotocopiado para uso en la evaluación del servicio realizado por "
                        + "AR ingeniería eléctrica spa, pero no compartido con terceros.");

        tituloSeccion(document, "1. Introducción.");
        parrafoCuerpo(document,
                "El presente informe tiene el objetivo de concentrar los resultados obtenidos del trabajo de mantenimiento "
                        + "preventivo, realizado por la empresa AR Ingeniería SpA a los tableros de espacio común de "
                        + (recinto.isBlank() ? "la propiedad" : recinto)
                        + "; individualizados en el punto 3.3 de este informe.");

        tituloSeccion(document, "2. Objetivos del Mantenimiento.");
        parrafoCuerpo(document,
                "El mantenimiento tiene por propósito extender la vida útil de los activos eléctricos del edificio. "
                        + "También debe ser capaz de generar ahorros a largo plazo, principalmente derivados de la alta "
                        + "continuidad operacional de los recintos, control de fallas y bajo costo de reparaciones "
                        + "(gestionados a través de los mantenimientos preventivos).");

        tituloSeccion(document, "3. Procedimiento general del mantenimiento.");
        tituloSubseccion(document, "3.1 Instrumentación para el servicio");
        parrafoCuerpo(document,
                "Para el desarrollo de las actividades de mantenimiento, es necesario la aplicación de instrumentos "
                        + "especializados tales como cámara termográfica, analizador de redes, probador de aislación e "
                        + "instrumentación de puesta en servicio de nuevas instalaciones; elementos que AR Ingeniería SpA. "
                        + "ha adquirido de las empresas que cuentan con mayor renombre en el rubro (Uni-t, Fluke y Hioki).");
        agregarInstrumentos(document);
        tituloSubseccion(document, "3.2 Procedimiento de mantenimiento.");
        parrafoCuerpo(document,
                "El procedimiento de mantenimiento preventivo, aplicado a los tableros eléctricos, se subdivide en tres "
                        + "áreas de ejecución. Cada una de las áreas, se centra en pautas de chequeo que aseguran la revisión "
                        + "de manera confiable y sistemática.");
        parrafoCuerpo(document,
                "Inspección visual: es donde se realizan revisión de índole físicas, fallas que pueden ser detectadas de "
                        + "fácil manera por los ojos de los técnicos especializados e inconsistencias que pudieran existir.");
        parrafoCuerpo(document,
                "Limpieza y mediciones: Se refiere al trabajo directo de la toma de parámetros eléctricos, voltajes, "
                        + "corrientes, temperaturas, factor de potencia. Además de la captura de las mediciones, se realiza "
                        + "el trabajo de limpieza general y re apriete de terminales y fijaciones; de esta manera, se "
                        + "minimiza el daño que el polvo puede causar y se chequean desviaciones en las mediciones, que "
                        + "pudiesen evidenciar un problema en los tableros.");
        parrafoCuerpo(document,
                "Termografía: Se refiere a la captura de referencias infrarrojas emitidas por los componentes de los "
                        + "tableros, mediante instrumento especializado como lo es la cámara termográfica. Los diferenciales "
                        + "de temperatura observados en el instrumento, permiten la detección indirecta de fallas que de otro "
                        + "modo no pudiesen ser detectadas (Puntos de discontinuidad térmica, mal dimensionamiento de "
                        + "conductores o fuerte presencia de armónicos en la red).");
    }

    private void agregarInstrumentos(XWPFDocument document) {
        List<FotoInforme> instrumentos = new ArrayList<>();
        for (String nombre : List.of("instrumento1.jpg", "instrumento2.jpg", "instrumento3.jpg", "instrumento4.png")) {
            byte[] bytes = recursoInforme(nombre);
            if (bytes != null) {
                instrumentos.add(new FotoInforme(bytes, null));
            }
        }
        if (!instrumentos.isEmpty()) {
            agregarGaleria(document, instrumentos, 4, 4.33, 4.33);
        }
    }

    private void agregarSeccionTablero(XWPFDocument document, Tarea tarea, InformeTablero informe) {
        String nombreTablero = nombreTablero(informe);
        tituloSubseccion(document, "Tablero: " + nombreTablero);

        agregarTablaDatos(document, new String[][] {
                {"Técnico", responsableMantenimiento(informe, tarea)},
                {"Ayudante(s)", ayudantesTarea(tarea)},
        });

        tituloSubseccion(document, "Detalles");
        List<ObservacionTablero> observaciones = new ArrayList<>(informe.getObservaciones());
        observaciones.sort(Comparator.comparingInt(ObservacionTablero::getOrden));
        if (observaciones.isEmpty()) {
            parrafoCuerpo(document, "Sin observaciones registradas.");
        } else {
            for (ObservacionTablero observacion : observaciones) {
                parrafoListaConGuion(document, observacion.getTexto());
            }
        }

        List<FotoInforme> fotosTablero = fotosNoTermograficas(informe);
        if (!fotosTablero.isEmpty()) {
            agregarLineasEnBlanco(document, 3);
            agregarGaleria(document, fotosTablero, 6, 2.8, 4.98);
            agregarLineasEnBlanco(document, 3);
        }
    }

    private void agregarLineasEnBlanco(XWPFDocument document, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            XWPFParagraph parrafo = document.createParagraph();
            parrafo.setSpacingBefore(0);
            parrafo.setSpacingAfter(0);
            parrafo.createRun().setFontSize(10);
        }
    }

    private void parrafoListaConGuion(XWPFDocument document, String texto) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setSpacingAfter(40);
        parrafo.setIndentationLeft(360);
        XWPFRun run = parrafo.createRun();
        run.setText("- " + texto(texto));
        run.setFontFamily("Arial");
        run.setFontSize(10);
    }

    private void agregarTablaDatos(XWPFDocument document, String[][] campos) {
        XWPFTable tabla = document.createTable(campos.length, 2);
        tabla.setWidth("100%");
        tabla.setTableAlignment(TableRowAlign.LEFT);

        CTTblPr tblPr = tabla.getCTTbl().getTblPr();
        tblPr.addNewTblLayout().setType(STTblLayoutType.FIXED);

        CTTblCellMar cellMar = tblPr.addNewTblCellMar();
        cellMar.addNewTop().setW(BigInteger.valueOf(80));
        cellMar.addNewBottom().setW(BigInteger.valueOf(80));
        cellMar.addNewLeft().setW(BigInteger.valueOf(130));
        cellMar.addNewRight().setW(BigInteger.valueOf(130));

        CTTblBorders bordes = tblPr.addNewTblBorders();
        for (CTBorder borde : List.of(
                bordes.addNewTop(), bordes.addNewLeft(), bordes.addNewBottom(), bordes.addNewRight(),
                bordes.addNewInsideH(), bordes.addNewInsideV())) {
            borde.setVal(STBorder.SINGLE);
            borde.setSz(BigInteger.valueOf(4));
            borde.setColor("C9CFE0");
        }

        for (int i = 0; i < campos.length; i++) {
            XWPFTableRow fila = tabla.getRow(i);
            fila.getCtRow().addNewTrPr().addNewCantSplit();

            XWPFTableCell celdaLabel = fila.getCell(0);
            celdaLabel.setWidth("2800");
            celdaLabel.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            sombrearCelda(celdaLabel, "EDF1F9");
            escribirCeldaTexto(celdaLabel, campos[i][0], true);

            XWPFTableCell celdaValor = fila.getCell(1);
            celdaValor.setWidth("6030");
            celdaValor.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            escribirCeldaTexto(celdaValor, campos[i][1], false);
        }
    }

    private void agregarTablaDosColumnas(XWPFDocument document, String cabecera1, String cabecera2,
                                         List<String[]> filas) {
        if (filas.isEmpty()) {
            return;
        }
        XWPFTable tabla = document.createTable(filas.size() + 1, 2);
        tabla.setWidth("100%");
        tabla.setTableAlignment(TableRowAlign.LEFT);

        CTTblPr tblPr = tabla.getCTTbl().getTblPr();
        tblPr.addNewTblLayout().setType(STTblLayoutType.FIXED);

        CTTblCellMar cellMar = tblPr.addNewTblCellMar();
        cellMar.addNewTop().setW(BigInteger.valueOf(80));
        cellMar.addNewBottom().setW(BigInteger.valueOf(80));
        cellMar.addNewLeft().setW(BigInteger.valueOf(130));
        cellMar.addNewRight().setW(BigInteger.valueOf(130));

        CTTblBorders bordes = tblPr.addNewTblBorders();
        for (CTBorder borde : List.of(
                bordes.addNewTop(), bordes.addNewLeft(), bordes.addNewBottom(), bordes.addNewRight(),
                bordes.addNewInsideH(), bordes.addNewInsideV())) {
            borde.setVal(STBorder.SINGLE);
            borde.setSz(BigInteger.valueOf(4));
            borde.setColor("C9CFE0");
        }

        XWPFTableRow filaCabecera = tabla.getRow(0);
        filaCabecera.getCtRow().addNewTrPr().addNewCantSplit();
        for (int c = 0; c < 2; c++) {
            XWPFTableCell celda = filaCabecera.getCell(c);
            celda.setWidth("4415");
            celda.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            sombrearCelda(celda, "EDF1F9");
            escribirCeldaTexto(celda, c == 0 ? cabecera1 : cabecera2, true);
        }

        for (int i = 0; i < filas.size(); i++) {
            XWPFTableRow fila = tabla.getRow(i + 1);
            fila.getCtRow().addNewTrPr().addNewCantSplit();
            for (int c = 0; c < 2; c++) {
                XWPFTableCell celda = fila.getCell(c);
                celda.setWidth("4415");
                celda.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                escribirCeldaTexto(celda, filas.get(i)[c], false);
            }
        }
    }

    private String nombreTablero(InformeTablero informe) {
        List<String> partes = new ArrayList<>();
        if (!texto(informe.getDescripcion()).isBlank()) {
            partes.add(texto(informe.getDescripcion()));
        }
        if (!texto(informe.getUbicacion()).isBlank()) {
            partes.add(texto(informe.getUbicacion()));
        }
        if (!texto(informe.getNivel()).isBlank()) {
            partes.add("Piso " + texto(informe.getNivel()));
        }
        return partes.isEmpty() ? "Tablero" : String.join(" - ", partes);
    }

    private String capitalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }

    private byte[] recursoInforme(String nombre) {
        try (InputStream in = DocumentoService.class.getResourceAsStream("/informe/" + nombre)) {
            return in == null ? null : in.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    private void escribirCeldaTexto(XWPFTableCell celda, String valor, boolean negrita) {
        XWPFParagraph parrafo = celda.getParagraphs().isEmpty()
                ? celda.addParagraph()
                : celda.getParagraphs().get(0);
        parrafo.setSpacingBefore(40);
        parrafo.setSpacingAfter(40);
        XWPFRun run = parrafo.createRun();
        run.setText(valor == null ? "" : valor);
        run.setFontFamily("Arial");
        run.setFontSize(11);
        run.setBold(negrita);
        run.setColor(negrita ? "1B6DC1" : "333333");
    }

    private void sombrearCelda(XWPFTableCell celda, String color) {
        CTTcPr tcPr = celda.getCTTc().getTcPr();
        if (tcPr == null) {
            tcPr = celda.getCTTc().addNewTcPr();
        }
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(color);
    }

    private String responsableMantenimiento(InformeTablero informe) {
        return responsableMantenimiento(informe, informe.getTarea());
    }

    private String responsableMantenimiento(InformeTablero informe, Tarea tarea) {
        Tecnico tecnico = informe.getTecnico() != null ? informe.getTecnico() : tarea.getTecnicoCabecera();
        if (tecnico == null) {
            return "No registrado";
        }
        return tecnico.getNombre() + " " + tecnico.getApellido();
    }

    private String ayudantesTarea(Tarea tarea) {
        List<String> ayudantes = tarea.getTecnicosInvitados().stream()
                .map(tecnico -> tecnico.getNombre() + " " + tecnico.getApellido())
                .toList();
        return ayudantes.isEmpty() ? "Sin ayudantes registrados" : String.join(", ", ayudantes);
    }

    private List<FotoInforme> fotosNoTermograficas(InformeTablero informe) {
        List<FotoInforme> fotos = new ArrayList<>();
        List<ImagenTablero> imagenes = new ArrayList<>(informe.getImagenes());
        imagenes.sort(Comparator
                .comparingInt((ImagenTablero imagen) -> ordenCategoriaImagen(imagen.getCategoria()))
                .thenComparingInt(ImagenTablero::getIndice));
        agregarImagenesCategoria(fotos, imagenes, "PREVIA");

        List<MedicionTablero> mediciones = new ArrayList<>(informe.getMediciones());
        mediciones.sort(Comparator.comparingLong(m -> m.getId() == null ? 0L : m.getId()));
        for (MedicionTablero medicion : mediciones) {
            if (!esMedicionTermografica(medicion)) {
                byte[] bytes = decodificar(medicion.getFotoBase64());
                if (bytes != null) {
                    fotos.add(new FotoInforme(bytes, null));
                }
            }
        }

        agregarImagenesCategoria(fotos, imagenes, "REAPRIETE");
        agregarImagenesCategoria(fotos, imagenes, "ASEO");

        List<ChecklistTablero> checklist = new ArrayList<>(informe.getChecklist());
        checklist.sort(Comparator.comparingLong(c -> c.getId() == null ? 0L : c.getId()));
        for (ChecklistTablero item : checklist) {
            if (!esChecklistTermografico(item)) {
                byte[] bytes = decodificar(item.getFotoBase64());
                if (bytes != null) {
                    fotos.add(new FotoInforme(bytes, null));
                }
            }
        }
        agregarImagenesCategoria(fotos, imagenes, "POSTERIOR");
        for (ImagenTablero imagen : imagenes) {
            String categoria = texto(imagen.getCategoria()).toUpperCase(Locale.ROOT);
            if (!List.of("PREVIA", "REAPRIETE", "ASEO", "POSTERIOR").contains(categoria)) {
                agregarImagen(fotos, imagen);
            }
        }
        return fotos;
    }

    private void agregarImagenesCategoria(List<FotoInforme> fotos, List<ImagenTablero> imagenes, String categoria) {
        for (ImagenTablero imagen : imagenes) {
            if (categoria.equalsIgnoreCase(texto(imagen.getCategoria()))) {
                agregarImagen(fotos, imagen);
            }
        }
    }

    private void agregarImagen(List<FotoInforme> fotos, ImagenTablero imagen) {
        byte[] bytes = decodificar(imagen.getBase64());
        if (bytes != null) {
            fotos.add(new FotoInforme(bytes, null));
        }
    }

    private int ordenCategoriaImagen(String categoria) {
        return switch (texto(categoria).toUpperCase(Locale.ROOT)) {
            case "PREVIA" -> 0;
            case "REAPRIETE" -> 1;
            case "ASEO" -> 2;
            case "POSTERIOR" -> 3;
            default -> 4;
        };
    }

    private void agregarRespaldoTermografia(XWPFDocument document, List<InformeTablero> informes) {
        List<FotoInforme> fotosTermografia = new ArrayList<>();
        for (InformeTablero informe : informes) {
            List<FotoInforme> fotos = new ArrayList<>();
            List<MedicionTablero> mediciones = new ArrayList<>(informe.getMediciones());
            mediciones.sort(Comparator.comparingLong(m -> m.getId() == null ? 0L : m.getId()));
            for (MedicionTablero medicion : mediciones) {
                if (!esMedicionTermografica(medicion)) {
                    continue;
                }
                byte[] bytes = decodificar(medicion.getFotoBase64());
                if (bytes == null) {
                    continue;
                }
                fotos.add(new FotoInforme(bytes, null));
            }
            List<ChecklistTablero> checklist = new ArrayList<>(informe.getChecklist());
            checklist.sort(Comparator.comparingLong(c -> c.getId() == null ? 0L : c.getId()));
            for (ChecklistTablero item : checklist) {
                if (!esChecklistTermografico(item)) {
                    continue;
                }
                byte[] bytes = decodificar(item.getFotoBase64());
                if (bytes == null) {
                    continue;
                }
                fotos.add(new FotoInforme(bytes, null));
            }
            if (!fotos.isEmpty()) {
                fotosTermografia.addAll(fotos);
            }
        }
        if (fotosTermografia.isEmpty()) {
            parrafoCuerpo(document,
                    "No se registraron fotografías termográficas para los tableros de este mantenimiento.");
        } else {
            tituloSubseccion(document, "Registros termográficos del mantenimiento");
            agregarGaleria(document, fotosTermografia, 6, 2.8, 4.98);
        }
    }

    private boolean esMedicionTermografica(MedicionTablero medicion) {
        return VARIABLES_TERMOGRAFIA.contains(texto(medicion.getVariableId()))
                || "Termografía".equalsIgnoreCase(texto(medicion.getLabel()))
                || "Temperatura".equalsIgnoreCase(texto(medicion.getLabel()));
    }

    private boolean esChecklistTermografico(ChecklistTablero item) {
        return ITEMS_TERMOGRAFIA.contains(texto(item.getItemId()))
                || "Termografía".equalsIgnoreCase(texto(item.getLabel()));
    }

    private void agregarResumenMantenimiento(XWPFDocument document, List<InformeTablero> informes) {
        parrafoCuerpo(document,
                "Se realiza el procedimiento estándar para mantenimiento preventivo de tableros: limpieza, "
                        + "inspección visual, medición de parámetros eléctricos y termografía.");

        List<String> observaciones = new ArrayList<>();
        for (InformeTablero informe : informes) {
            String nombreTablero = nombreTablero(informe);
            List<ObservacionTablero> delTablero = new ArrayList<>(informe.getObservaciones());
            delTablero.sort(Comparator.comparingInt(ObservacionTablero::getOrden));
            for (ObservacionTablero observacion : delTablero) {
                observaciones.add(nombreTablero + " - " + texto(observacion.getTexto()));
            }
        }
        if (observaciones.isEmpty()) {
            parrafoCuerpo(document,
                    "No se detectan observaciones de operatividad a la fecha en los tableros revisados.");
        } else {
            parrafoCuerpo(document, "Se detectan las siguientes observaciones durante el mantenimiento:");
            for (String observacion : observaciones) {
                parrafoLista(document, observacion);
            }
        }

        parrafoCuerpo(document,
                "Se sugiere cambio de componentes una vez se detecte falla, para evitar problemas acumulados en el tiempo.");
    }

    private void agregarEncabezadoLogo(XWPFDocument document) {
        byte[] logo = recursoInforme("logo-aring.png");
        if (logo == null) {
            return;
        }
        try {
            XWPFHeader cabecera = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFParagraph parrafo = cabecera.createParagraph();
            parrafo.setAlignment(ParagraphAlignment.RIGHT);
            parrafo.setSpacingAfter(0);
            XWPFRun run = parrafo.createRun();
            int[] dimensiones = dimensionesFoto(logo, Units.toEMU(110), Units.toEMU(28));
            run.addPicture(new ByteArrayInputStream(logo), Document.PICTURE_TYPE_PNG,
                    "logo_aring.png", dimensiones[0], dimensiones[1]);
        } catch (Exception ignorada) {
            // el informe se genera igual si el logo no está disponible
        }
    }

    private void agregarImagenPortada(XWPFDocument document, byte[] bytes) {
        long ancho = ANCHO_PORTADA_EMU;
        long alto = ALTO_PORTADA_EMU;
        byte[] recortada = recortarImagen(bytes, (double) ancho / alto);
        if (recortada == null) {
            agregarImagenCentrada(document, bytes, ancho, alto);
            return;
        }
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setAlignment(ParagraphAlignment.CENTER);
        parrafo.setSpacingBefore(120);
        parrafo.setSpacingAfter(120);
        try {
            XWPFRun run = parrafo.createRun();
            int numero = contadorFotosWord.incrementAndGet();
            run.addPicture(new ByteArrayInputStream(recortada), Document.PICTURE_TYPE_JPEG,
                    "edificio_portada_" + numero + ".jpg", (int) ancho, (int) alto);
        } catch (Exception ignorada) {
            // Si la imagen no se puede insertar, la portada se genera sin ella.
        }
    }

    private byte[] recortarImagen(byte[] bytes, double proporcionObjetivo) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
            if (original == null || original.getWidth() <= 0 || original.getHeight() <= 0) {
                return null;
            }
            int x = 0;
            int y = 0;
            int ancho = original.getWidth();
            int alto = original.getHeight();
            double proporcion = (double) ancho / alto;
            if (proporcion > proporcionObjetivo) {
                ancho = Math.max(1, (int) Math.round(alto * proporcionObjetivo));
                x = (original.getWidth() - ancho) / 2;
            } else if (proporcion < proporcionObjetivo) {
                alto = Math.max(1, (int) Math.round(ancho / proporcionObjetivo));
                y = (original.getHeight() - alto) / 2;
            }
            BufferedImage recorte = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = recorte.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(original, 0, 0, ancho, alto, x, y, x + ancho, y + alto, null);
            graphics.dispose();
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(recorte, "jpg", out);
                return out.toByteArray();
            }
        } catch (Exception ignorada) {
            return null;
        }
    }

    private void agregarImagenCentrada(XWPFDocument document, byte[] bytes, long anchoMaxEmu, long altoMaxEmu) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setAlignment(ParagraphAlignment.CENTER);
        parrafo.setSpacingBefore(120);
        parrafo.setSpacingAfter(120);
        try {
            int tipo = tipoImagen(bytes);
            int[] dimensiones = dimensionesFoto(bytes, anchoMaxEmu, altoMaxEmu);
            XWPFRun run = parrafo.createRun();
            int numero = contadorFotosWord.incrementAndGet();
            run.addPicture(new ByteArrayInputStream(bytes), tipo,
                    "foto_" + numero + (tipo == Document.PICTURE_TYPE_PNG ? ".png" : ".jpg"),
                    dimensiones[0], dimensiones[1]);
        } catch (Exception ignorada) {
            // si una foto no se puede incrustar, el informe se genera igual
        }
    }

    private void agregarGaleria(XWPFDocument document, List<FotoInforme> fotos, int columnas,
                                double anchoFotoCm, double altoFotoCm) {
        if (fotos.isEmpty()) {
            return;
        }
        int columnasEfectivas = Math.max(1, Math.min(columnas, fotos.size()));
        int totalFilas = (fotos.size() + columnasEfectivas - 1) / columnasEfectivas;

        XWPFTable tabla = document.createTable(totalFilas, columnasEfectivas);
        tabla.setTableAlignment(TableRowAlign.CENTER);
        long anchoCeldaEMU = cmToEmu(anchoFotoCm);
        long altoFotoEMU = cmToEmu(altoFotoCm);
        int twipsCelda = (int) Math.round(anchoFotoCm / 2.54 * 1440);
        tabla.setWidth((twipsCelda * columnasEfectivas) + "");

        CTTblPr tblPr = tabla.getCTTbl().getTblPr();
        tblPr.addNewTblLayout().setType(STTblLayoutType.FIXED);
        CTTblBorders bordes = tblPr.addNewTblBorders();
        for (CTBorder borde : List.of(
                bordes.addNewTop(), bordes.addNewLeft(), bordes.addNewBottom(), bordes.addNewRight(),
                bordes.addNewInsideH(), bordes.addNewInsideV())) {
            borde.setVal(STBorder.NONE);
            borde.setSz(BigInteger.ZERO);
            borde.setColor("000000");
        }
        tblPr.addNewTblCellSpacing().setType(STTblWidth.DXA);
        tblPr.getTblCellSpacing().setW(BigInteger.ZERO);
        CTTblCellMar cellMargins = tblPr.addNewTblCellMar();
        cellMargins.addNewTop().setW(BigInteger.ZERO);
        cellMargins.addNewBottom().setW(BigInteger.ZERO);
        cellMargins.addNewLeft().setW(BigInteger.ZERO);
        cellMargins.addNewRight().setW(BigInteger.ZERO);

        int indiceFoto = 0;
        for (int fila = 0; fila < totalFilas; fila++) {
            XWPFTableRow filaTabla = tabla.getRow(fila);
            filaTabla.setHeightRule(TableRowHeightRule.AUTO);
            filaTabla.getCtRow().addNewTrPr().addNewCantSplit();
            for (int columna = 0; columna < columnasEfectivas; columna++) {
                XWPFTableCell celda = filaTabla.getCell(columna);
                celda.setWidth(twipsCelda + "");
                celda.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                if (indiceFoto >= fotos.size()) {
                    continue;
                }
                FotoInforme foto = fotos.get(indiceFoto++);
                XWPFParagraph parrafo = celda.getParagraphs().isEmpty()
                        ? celda.addParagraph()
                        : celda.getParagraphs().get(0);
                parrafo.setAlignment(ParagraphAlignment.CENTER);
                parrafo.setSpacingBefore(0);
                parrafo.setSpacingAfter(0);
                try {
                    byte[] bytes = foto.bytes();
                    int tipo = tipoImagen(bytes);
                    int[] dimensiones = dimensionesFoto(bytes, anchoCeldaEMU, altoFotoEMU);
                    XWPFRun run = parrafo.createRun();
                    int numero = contadorFotosWord.incrementAndGet();
                    run.addPicture(new ByteArrayInputStream(bytes), tipo,
                            "foto_" + numero + (tipo == Document.PICTURE_TYPE_PNG ? ".png" : ".jpg"),
                            dimensiones[0], dimensiones[1]);
                } catch (Exception ignorada) {
                    // si una foto no se puede incrustar, el informe se genera igual
                }
                if (foto.pie() != null && !foto.pie().isBlank()) {
                    XWPFParagraph parrafoPie = celda.addParagraph();
                    parrafoPie.setAlignment(ParagraphAlignment.CENTER);
                    parrafoPie.setSpacingBefore(20);
                    parrafoPie.setSpacingAfter(60);
                    XWPFRun runPie = parrafoPie.createRun();
                    runPie.setText(foto.pie());
                    runPie.setFontSize(9);
                    runPie.setItalic(true);
                    runPie.setColor("5A6275");
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Word: utilidades
    // ------------------------------------------------------------------

    private void tituloSeccion(XWPFDocument document, String texto) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setSpacingBefore(240);
        parrafo.setSpacingAfter(80);
        XWPFRun run = parrafo.createRun();
        run.setText(texto);
        run.setBold(false);
        run.setFontFamily("Arial");
        run.setFontSize(14);
        run.setColor("1B6DC1");
    }

    private void tituloSubseccion(XWPFDocument document, String texto) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setSpacingBefore(160);
        parrafo.setSpacingAfter(60);
        XWPFRun run = parrafo.createRun();
        run.setText(texto);
        run.setBold(false);
        run.setFontFamily("Arial");
        run.setFontSize(13);
        run.setColor("1B6DC1");
    }

    private void saltoPagina(XWPFDocument document) {
        document.createParagraph().createRun().addBreak(BreakType.PAGE);
    }

    private void parrafoCuerpo(XWPFDocument document, String texto) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setAlignment(ParagraphAlignment.BOTH);
        parrafo.setSpacingAfter(0);
        XWPFRun run = parrafo.createRun();
        run.setText(texto);
        run.setFontFamily("Arial");
        run.setFontSize(10);
    }

    private void parrafoLista(XWPFDocument document, String texto) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setSpacingAfter(40);
        parrafo.setIndentationLeft(360);
        XWPFRun run = parrafo.createRun();
        run.setText("• " + texto(texto));
        run.setFontFamily("Arial");
        run.setFontSize(10);
    }

    private void configurarPagina(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = sectPr.addNewPgSz();
        pageSize.setW(12240);
        pageSize.setH(15840);
        CTPageMar margenes = sectPr.addNewPgMar();
        margenes.setTop(720);
        margenes.setRight(720);
        margenes.setBottom(720);
        margenes.setLeft(720);
        margenes.setHeader(432);
        margenes.setFooter(432);

        XWPFFooter pie = document.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph parrafo = pie.createParagraph();
        parrafo.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = parrafo.createRun();
        run.setFontSize(9);
        run.setColor("8A8A8A");
        run.setText("Página ");
        XWPFRun campo = parrafo.createRun();
        campo.setFontSize(9);
        campo.setColor("8A8A8A");
        CTR ctr = campo.getCTR();
        ctr.addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        ctr.addNewInstrText().setStringValue(" PAGE ");
        ctr.addNewFldChar().setFldCharType(STFldCharType.END);
    }

    private int tipoImagen(byte[] bytes) {
        if (bytes.length > 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return Document.PICTURE_TYPE_PNG;
        }
        return Document.PICTURE_TYPE_JPEG;
    }

    private static long cmToEmu(double centimetros) {
        return Math.round(centimetros / 2.54 * 72 * 12700);
    }

    private int[] dimensionesFoto(byte[] bytes, long anchoMax, long altoMax) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
                double proporcion = (double) img.getHeight() / img.getWidth();
                long ancho = anchoMax;
                long alto = Math.round(anchoMax * proporcion);
                if (alto > altoMax) {
                    alto = altoMax;
                    ancho = Math.round(altoMax / proporcion);
                }
                return new int[]{(int) ancho, (int) alto};
            }
        } catch (Exception ignorada) {
            // se usa el tamaño por defecto si no se puede leer la imagen
        }
        return new int[]{(int) anchoMax, (int) altoMax};
    }

    private byte[] decodificar(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Excel: estilos
    // ------------------------------------------------------------------

    private CellStyle estiloTituloHistorial(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        aplicarBordes(style);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle estiloCabeceraHistorial(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new Color(217, 226, 243), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        aplicarBordes(style);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle estiloDatoMonedaHistorial(Workbook workbook, boolean alternar) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        if (alternar) {
            style.setFillForegroundColor(new XSSFColor(new Color(242, 247, 251), null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        aplicarBordes(style);
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("$#,##0.00"));
        return style;
    }

    private CellStyle estiloDatoHistorial(Workbook workbook, boolean centrado, boolean alternar) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(centrado ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        if (alternar) {
            style.setFillForegroundColor(new XSSFColor(new Color(242, 247, 251), null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        aplicarBordes(style);
        return style;
    }

    private void aplicarBordes(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    // ------------------------------------------------------------------
    // Utilidades generales
    // ------------------------------------------------------------------

    private void verificarTareaExiste(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return;
        }
        boolean existe = !buscarTareas(codigo, null, null, null, null, null).isEmpty();
        if (!existe) {
            throw new ApiException("La tarea buscada no existe.",
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
    }

    private List<Tarea> buscarTareas(String codigo, String nombreTecnico, String nombreRecinto, EstadoTarea estado,
                                     Instant fechaDesde, Instant fechaHasta) {
        Specification<Tarea> spec = TareaSpecifications.conFiltros(
                codigo, nombreTecnico, nombreRecinto, estado, fechaDesde, fechaHasta);
        return tareaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
    }

    private List<Tarea> tareasFinalizadasConDatos(String codigo, String nombreTecnico, String nombreRecinto) {
        verificarTareaExiste(codigo);
        List<Tarea> tareas = buscarTareas(codigo, nombreTecnico, nombreRecinto, EstadoTarea.FINALIZADA, null, null);
        if (codigo != null && !codigo.isBlank() && tareas.isEmpty()) {
            throw new ApiException("La tarea " + codigo + " no existe o aún no está finalizada.",
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        List<Tarea> conDatos = tareas.stream()
                .filter(t -> !informeTableroRepository.findAllByTareaIdOrderByIdAsc(t.getId()).isEmpty())
                .toList();
        if (conDatos.isEmpty()) {
            String mensaje = codigo != null && !codigo.isBlank()
                    ? "La tarea " + codigo + " no tiene tableros inspeccionados para generar el informe."
                    : "No hay tareas finalizadas con tableros registrados para los filtros seleccionados.";
            throw new ApiException(mensaje, org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return conDatos;
    }

    private void escribirCelda(Row row, int index, String valor, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(valor == null ? "" : valor);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private String nombreCompletoTecnico(Tarea tarea) {
        if (tarea.getTecnicoCabecera() == null) {
            return "Sin técnico de cabecera";
        }
        return tarea.getTecnicoCabecera().getNombre() + " " + tarea.getTecnicoCabecera().getApellido();
    }

    private String tipoTableroRendicion(InformeTablero informe) {
        if (informe.getTipoTablero() == null || informe.getTipoTablero() == TipoTablero.MONOFASICO) {
            return "Monofásico";
        }
        return informe.getTipoTablero().getEtiqueta();
    }

    private String tamanioTableroRendicion(InformeTablero informe) {
        if (informe.getTipoTablero() == null || informe.getTipoTablero() == TipoTablero.MONOFASICO
                || informe.getTamanio() == null || informe.getTamanio().isBlank()) {
            return "No Aplica";
        }
        return informe.getTamanio();
    }

    private String estadoTarea(Tarea tarea) {
        return tarea.getEstado() != null ? tarea.getEstado().getEtiqueta() : "";
    }

    private String formatear(Instant instante) {
        return instante == null ? "" : FORMATO_FECHA.format(instante);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
