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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Service
public class DocumentoService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Santiago"));

    private static final long ANCHO_UTIL_EMU = (12240L - 2 * 1701L) * 635L;

    private final TareaRepository tareaRepository;
    private final InformeTableroRepository informeTableroRepository;

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

    public record InformeTarea(byte[] bytes, String codigo) {
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
        return new InformeTarea(generarInformeWord(List.of(tarea)), tarea.getCodigo());
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
                    document.createParagraph().createRun().addBreak(BreakType.PAGE);
                }
                primerBloque = false;
                tituloInforme(document, tarea.getCodigo());
                boolean primerTablero = true;
                for (InformeTablero informe : informes) {
                    if (!primerTablero) {
                        document.createParagraph().createRun().addBreak(BreakType.PAGE);
                    }
                    primerTablero = false;
                    agregarTableroAlWord(document, tarea, informe);
                    hayInformes = true;
                }
            }

            if (!hayInformes) {
                parrafoCuerpo(document,
                        "No se encontraron tableros inspeccionados para las tareas seleccionadas.");
            }

            configurarPagina(document);

            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException("No se pudo generar el archivo Word: " + e.getMessage(),
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void agregarTableroAlWord(XWPFDocument document, Tarea tarea, InformeTablero informe) {
        tituloTablero(document, informe);
        agregarTablaInformacion(document, tarea, informe);

        tituloSeccion(document, "Detalles de revisión tablero eléctrico");

        parrafoCuerpo(document, "Se realiza procedimiento de mantenimiento eléctrico en tablero de distribución, " +
                "efectuando inspección visual de sus componentes, partes eléctricas, protecciones y puntos de conexión.");
        parrafoCuerpo(document, "Se verifica el estado general de los componentes, equipos y terminales, " +
                "realizando las pruebas correspondientes de continuidad y resistencia de aislación.");
        parrafoCuerpo(document, "Se detectan las siguientes observaciones en los tableros:");

        List<ObservacionTablero> observaciones = new ArrayList<>(informe.getObservaciones());
        observaciones.sort(Comparator.comparingInt(ObservacionTablero::getOrden));
        if (observaciones.isEmpty()) {
            parrafoCuerpo(document, "No se registraron observaciones.");
        } else {
            for (ObservacionTablero obs : observaciones) {
                parrafoLista(document, obs.getTexto());
            }
        }

        tituloSeccion(document, "Respaldo tablero eléctrico");
        agregarFotosRespaldo(document, informe);
    }

    private void tituloTablero(XWPFDocument document, InformeTablero informe) {
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

        XWPFParagraph titulo = document.createParagraph();
        titulo.setAlignment(ParagraphAlignment.CENTER);
        titulo.setSpacingBefore(0);
        titulo.setSpacingAfter(80);
        XWPFRun run = titulo.createRun();
        run.setText(partes.isEmpty() ? "Tablero" : "Tablero: " + String.join(" - ", partes));
        run.setBold(true);
        run.setFontSize(14);
        run.setColor("1F3864");
    }

    private void tituloInforme(XWPFDocument document, String codigoTarea) {
        XWPFParagraph titulo = document.createParagraph();
        titulo.setAlignment(ParagraphAlignment.CENTER);
        titulo.setSpacingBefore(0);
        titulo.setSpacingAfter(60);
        XWPFRun runTitulo = titulo.createRun();
        runTitulo.setText("Informe de Mantenimiento");
        runTitulo.setBold(true);
        runTitulo.setFontSize(20);
        runTitulo.setColor("1F3864");

        XWPFParagraph subtitulo = document.createParagraph();
        subtitulo.setAlignment(ParagraphAlignment.CENTER);
        subtitulo.setSpacingAfter(260);
        XWPFRun runSub = subtitulo.createRun();
        runSub.setText("Tarea " + codigoTarea);
        runSub.setFontSize(12);
        runSub.setColor("5A6275");

        CTPPr pPr = subtitulo.getCTP().isSetPPr() ? subtitulo.getCTP().getPPr() : subtitulo.getCTP().addNewPPr();
        CTPBdr pBdr = pPr.addNewPBdr();
        pBdr.addNewBottom().setVal(STBorder.SINGLE);
        pBdr.addNewBottom().setSz(BigInteger.valueOf(8));
        pBdr.addNewBottom().setColor("1F3864");
    }

    private void agregarTablaInformacion(XWPFDocument document, Tarea tarea, InformeTablero informe) {
        String[][] campos = {
                {"Responsable Mantenimiento", responsableMantenimiento(informe)},
                {"Fecha Inicio", formatear(tarea.getFechaInicio())},
                {"Fecha termino", formatear(tarea.getFechaFin())},
                {"Descripcion", texto(informe.getDescripcion())},
                {"Ubicación", texto(informe.getUbicacion())},
                {"Piso", texto(informe.getNivel())},
                {"Tipo de tablero", tipoTableroRendicion(informe)},
                {"Tamaño", tamanioTableroRendicion(informe)},
        };

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

    private void escribirCeldaTexto(XWPFTableCell celda, String valor, boolean negrita) {
        XWPFParagraph parrafo = celda.getParagraphs().isEmpty()
                ? celda.addParagraph()
                : celda.getParagraphs().get(0);
        parrafo.setSpacingBefore(40);
        parrafo.setSpacingAfter(40);
        XWPFRun run = parrafo.createRun();
        run.setText(valor == null ? "" : valor);
        run.setFontSize(11);
        run.setBold(negrita);
        run.setColor(negrita ? "1F3864" : "333333");
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
        Tecnico tecnico = informe.getTecnico();
        if (tecnico == null) {
            return "No registrado";
        }
        return tecnico.getNombre() + " " + tecnico.getApellido();
    }

    private void agregarFotosRespaldo(XWPFDocument document, InformeTablero informe) {
        List<byte[]> fotos = new ArrayList<>();

        List<ImagenTablero> imagenes = new ArrayList<>(informe.getImagenes());
        imagenes.sort(Comparator.comparing(ImagenTablero::getCategoria)
                .thenComparingInt(ImagenTablero::getIndice));
        for (ImagenTablero img : imagenes) {
            byte[] bytes = decodificar(img.getBase64());
            if (bytes != null) {
                fotos.add(bytes);
            }
        }
        List<MedicionTablero> mediciones = new ArrayList<>(informe.getMediciones());
        mediciones.sort(Comparator.comparingLong(MedicionTablero::getId));
        for (MedicionTablero m : mediciones) {
            byte[] bytes = decodificar(m.getFotoBase64());
            if (bytes != null) {
                fotos.add(bytes);
            }
        }
        List<ChecklistTablero> checklist = new ArrayList<>(informe.getChecklist());
        checklist.sort(Comparator.comparingLong(ChecklistTablero::getId));
        for (ChecklistTablero c : checklist) {
            byte[] bytes = decodificar(c.getFotoBase64());
            if (bytes != null) {
                fotos.add(bytes);
            }
        }

        if (fotos.isEmpty()) {
            parrafoCuerpo(document, "No se registraron fotografías de respaldo.");
            return;
        }

        int columnas = columnasCollage(fotos.size());
        int totalFilas = (fotos.size() + columnas - 1) / columnas;

        XWPFTable tabla = document.createTable(totalFilas, columnas);
        tabla.setTableAlignment(TableRowAlign.CENTER);
        tabla.setWidth("100%");

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
        tblPr.getTblCellSpacing().setW(BigInteger.valueOf(120));

        long margenEMU = Units.toEMU(2);
        long anchoCeldaEMU = ANCHO_UTIL_EMU / columnas;
        long altoMaxEMU = altoMaximoFoto(columnas);
        int twipsCelda = (int) (anchoCeldaEMU / 635);

        int indiceFoto = 0;
        for (int fila = 0; fila < totalFilas; fila++) {
            XWPFTableRow filaTabla = tabla.getRow(fila);
            filaTabla.setHeightRule(TableRowHeightRule.AUTO);
            filaTabla.getCtRow().addNewTrPr().addNewCantSplit();
            for (int columna = 0; columna < columnas; columna++) {
                XWPFTableCell celda = filaTabla.getCell(columna);
                celda.setWidth(twipsCelda + "");
                celda.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                if (indiceFoto >= fotos.size()) {
                    continue;
                }
                XWPFParagraph parrafo = celda.getParagraphs().isEmpty()
                        ? celda.addParagraph()
                        : celda.getParagraphs().get(0);
                parrafo.setAlignment(ParagraphAlignment.CENTER);
                parrafo.setSpacingBefore(0);
                parrafo.setSpacingAfter(0);
                try {
                    byte[] foto = fotos.get(indiceFoto);
                    int tipo = tipoImagen(foto);
                    int[] dimensiones = dimensionesFoto(foto, anchoCeldaEMU - 2 * margenEMU, altoMaxEMU);
                    XWPFRun run = parrafo.createRun();
                    run.addPicture(new ByteArrayInputStream(foto), tipo,
                            "foto_" + indiceFoto + (tipo == Document.PICTURE_TYPE_PNG ? ".png" : ".jpg"),
                            dimensiones[0], dimensiones[1]);
                } catch (Exception ignorada) {
                    // si una foto no se puede incrustar, el informe se genera igual
                }
                indiceFoto++;
            }
        }
    }

    // ------------------------------------------------------------------
    // Word: utilidades
    // ------------------------------------------------------------------

    private void tituloSeccion(XWPFDocument document, String texto) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setSpacingBefore(240);
        parrafo.setSpacingAfter(140);
        XWPFRun run = parrafo.createRun();
        run.setText(texto);
        run.setBold(true);
        run.setFontSize(13);
        run.setColor("1F3864");
    }

    private void parrafoCuerpo(XWPFDocument document, String texto) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setSpacingAfter(60);
        XWPFRun run = parrafo.createRun();
        run.setText(texto);
        run.setFontSize(11);
    }

    private void parrafoLista(XWPFDocument document, String texto) {
        XWPFParagraph parrafo = document.createParagraph();
        parrafo.setSpacingAfter(40);
        parrafo.setIndentationLeft(360);
        XWPFRun run = parrafo.createRun();
        run.setText("• " + texto(texto));
        run.setFontSize(11);
    }

    private void configurarPagina(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = sectPr.addNewPgSz();
        pageSize.setW(12240);
        pageSize.setH(15840);
        CTPageMar margenes = sectPr.addNewPgMar();
        margenes.setTop(1417);
        margenes.setRight(1701);
        margenes.setBottom(1417);
        margenes.setLeft(1701);
        margenes.setHeader(709);
        margenes.setFooter(709);

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

    private int columnasCollage(int cantidad) {
        if (cantidad == 1) {
            return 1;
        }
        if (cantidad == 2 || cantidad == 4) {
            return 2;
        }
        if (cantidad == 3) {
            return 3;
        }
        return Math.min(4, (int) Math.ceil(Math.sqrt(cantidad)));
    }

    private long altoMaximoFoto(int columnas) {
        if (columnas == 1) {
            return Units.toEMU(300);
        }
        if (columnas == 2) {
            return Units.toEMU(235);
        }
        if (columnas == 3) {
            return Units.toEMU(205);
        }
        return Units.toEMU(185);
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
