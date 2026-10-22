package com.medina.fragrances.fragrance_app.service;

import com.medina.fragrances.fragrance_app.model.Pedido;
import com.medina.fragrances.fragrance_app.repository.PedidoRepository;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class ReporteVentasService {

    private static final String[] ENCABEZADOS = {
            "ID", "Fecha", "Cliente", "Email", "Producto",
            "Cantidad", "Total", "Metodo de pago", "Metodo de entrega", "Estado"
    };

    // Ancho de cada columna en caracteres (se fija a mano para no depender de fuentes del sistema)
    private static final int[] ANCHOS = {8, 18, 26, 30, 30, 10, 14, 18, 20, 14};

    // Fila (0-based) donde empiezan los encabezados y donde empiezan los datos
    private static final int FILA_ENCABEZADO = 2;
    private static final int FILA_DATOS = 3;

    private final PedidoRepository pedidoRepository;

    public ReporteVentasService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Genera un archivo .xlsx con las ventas del mes indicado.
     * Se excluyen los pedidos con estado CANCELADO.
     */
    @Transactional(readOnly = true)
    public byte[] generarExcelVentasMes(YearMonth mes) {
        LocalDateTime inicio = mes.atDay(1).atStartOfDay();
        LocalDateTime fin = mes.plusMonths(1).atDay(1).atStartOfDay();

        List<Pedido> ventas = pedidoRepository.findByRangoFecha(inicio, fin).stream()
                .filter(pedido -> !"CANCELADO".equals(pedido.getEstado()))
                .toList();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            DataFormat formato = workbook.createDataFormat();

            // ----- Estilos -----
            Font fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            CellStyle estiloTitulo = workbook.createCellStyle();
            estiloTitulo.setFont(fuenteTitulo);

            Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            CellStyle estiloEncabezado = workbook.createCellStyle();
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);
            aplicarBordes(estiloEncabezado);

            CellStyle estiloTexto = workbook.createCellStyle();
            aplicarBordes(estiloTexto);

            CellStyle estiloNumero = workbook.createCellStyle();
            estiloNumero.setAlignment(HorizontalAlignment.CENTER);
            aplicarBordes(estiloNumero);

            CellStyle estiloFecha = workbook.createCellStyle();
            estiloFecha.setDataFormat(formato.getFormat("dd/mm/yyyy hh:mm"));
            aplicarBordes(estiloFecha);

            CellStyle estiloMoneda = workbook.createCellStyle();
            estiloMoneda.setDataFormat(formato.getFormat("\"S/.\" #,##0.00"));
            aplicarBordes(estiloMoneda);

            Font fuenteTotal = workbook.createFont();
            fuenteTotal.setBold(true);
            CellStyle estiloTotalTexto = workbook.createCellStyle();
            estiloTotalTexto.setFont(fuenteTotal);
            estiloTotalTexto.setAlignment(HorizontalAlignment.RIGHT);
            aplicarBordes(estiloTotalTexto);

            CellStyle estiloTotalNumero = workbook.createCellStyle();
            estiloTotalNumero.setFont(fuenteTotal);
            estiloTotalNumero.setAlignment(HorizontalAlignment.CENTER);
            aplicarBordes(estiloTotalNumero);

            CellStyle estiloTotalMoneda = workbook.createCellStyle();
            estiloTotalMoneda.setFont(fuenteTotal);
            estiloTotalMoneda.setDataFormat(formato.getFormat("\"S/.\" #,##0.00"));
            aplicarBordes(estiloTotalMoneda);

            // ----- Hoja -----
            Sheet hoja = workbook.createSheet("Ventas " + mes);
            for (int i = 0; i < ANCHOS.length; i++) {
                hoja.setColumnWidth(i, ANCHOS[i] * 256);
            }

            // Titulo
            String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"));
            Row filaTitulo = hoja.createRow(0);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue("Medina Fragrances - Ventas de " + nombreMes + " " + mes.getYear());
            celdaTitulo.setCellStyle(estiloTitulo);
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, ENCABEZADOS.length - 1));

            // Encabezados
            Row filaEncabezado = hoja.createRow(FILA_ENCABEZADO);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(ENCABEZADOS[i]);
                celda.setCellStyle(estiloEncabezado);
            }
            hoja.createFreezePane(0, FILA_DATOS);

            // Datos
            int indiceFila = FILA_DATOS;
            for (Pedido pedido : ventas) {
                Row fila = hoja.createRow(indiceFila++);

                crearCelda(fila, 0, pedido.getId() != null ? pedido.getId() : 0, estiloNumero);

                Cell celdaFecha = fila.createCell(1);
                if (pedido.getFecha() != null) {
                    celdaFecha.setCellValue(pedido.getFecha());
                } else {
                    celdaFecha.setCellValue("-");
                }
                celdaFecha.setCellStyle(estiloFecha);

                String cliente = pedido.getUsuario() != null ? pedido.getUsuario().getNombre() : "-";
                String email = pedido.getUsuario() != null ? pedido.getUsuario().getEmail() : "-";
                String producto = pedido.getProducto() != null ? pedido.getProducto().getNombre() : "-";

                crearCelda(fila, 2, cliente, estiloTexto);
                crearCelda(fila, 3, email, estiloTexto);
                crearCelda(fila, 4, producto, estiloTexto);
                crearCelda(fila, 5, pedido.getCantidad() != null ? pedido.getCantidad() : 0, estiloNumero);
                crearCelda(fila, 6, pedido.getTotal() != null ? pedido.getTotal() : 0.0, estiloMoneda);
                crearCelda(fila, 7, pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "Manual", estiloTexto);
                crearCelda(fila, 8, pedido.getMetodoEntrega() != null ? pedido.getMetodoEntrega() : "No indicado", estiloTexto);
                crearCelda(fila, 9, pedido.getEstado() != null ? pedido.getEstado() : "-", estiloTexto);
            }

            if (ventas.isEmpty()) {
                Row filaVacia = hoja.createRow(FILA_DATOS);
                Cell celdaVacia = filaVacia.createCell(0);
                celdaVacia.setCellValue("No hay ventas registradas en este mes.");
                hoja.addMergedRegion(new CellRangeAddress(FILA_DATOS, FILA_DATOS, 0, ENCABEZADOS.length - 1));
            } else {
                // Fila de totales con formulas de Excel
                // Las filas de Excel son 1-based: los datos van de (FILA_DATOS + 1) a (FILA_DATOS + ventas.size())
                int primeraFilaExcel = FILA_DATOS + 1;
                int ultimaFilaExcel = FILA_DATOS + ventas.size();

                Row filaTotal = hoja.createRow(indiceFila);
                Cell etiqueta = filaTotal.createCell(4);
                etiqueta.setCellValue("TOTAL");
                etiqueta.setCellStyle(estiloTotalTexto);

                Cell totalCantidad = filaTotal.createCell(5);
                totalCantidad.setCellFormula("SUM(F" + primeraFilaExcel + ":F" + ultimaFilaExcel + ")");
                totalCantidad.setCellStyle(estiloTotalNumero);

                Cell totalVentas = filaTotal.createCell(6);
                totalVentas.setCellFormula("SUM(G" + primeraFilaExcel + ":G" + ultimaFilaExcel + ")");
                totalVentas.setCellStyle(estiloTotalMoneda);

                // Calcula el resultado de las formulas para que se vea en cualquier visor
                workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            }

            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel de ventas", e);
        }
    }

    private void aplicarBordes(CellStyle estilo) {
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }

    private void crearCelda(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor != null ? valor : "-");
        celda.setCellStyle(estilo);
    }

    private void crearCelda(Row fila, int columna, double valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }
}
