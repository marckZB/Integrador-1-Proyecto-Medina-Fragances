package com.medina.fragrances.fragrance_app.service;

import com.medina.fragrances.fragrance_app.model.Pedido;
import com.medina.fragrances.fragrance_app.model.Producto;
import com.medina.fragrances.fragrance_app.model.Usuario;
import com.medina.fragrances.fragrance_app.repository.PedidoRepository;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reportes en Excel (Apache POI) para el panel de administrador:
 * inventario, pedidos pendientes, envios y despachos, y clientes registrados.
 * El reporte de ventas del mes esta en ReporteVentasService.
 */
@Service
public class ReportesAdminService {

    // Mismo criterio que usa el panel para "stock bajo"
    private static final int STOCK_BAJO = 5;

    // Cantidad de productos que muestra el reporte de mas solicitados
    private static final int TOP_PRODUCTOS = 10;

    // Estados que entran en el reporte de envios y despachos, en este orden
    private static final List<String> ESTADOS_ENVIO = List.of("PAGADO", "ENVIADO", "ENTREGADO");

    // Fila (0-based) del encabezado y donde empiezan los datos
    private static final int FILA_ENCABEZADO = 3;
    private static final int FILA_DATOS = 4;

    private static final String FORMATO_MONEDA = "\"S/.\" #,##0.00";
    private static final String FORMATO_FECHA_EXCEL = "dd/mm/yyyy hh:mm";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Comparator<LocalDateTime> POR_FECHA = Comparator.nullsLast(Comparator.naturalOrder());

    private final PedidoRepository pedidoRepository;
    private final ProductoService productoService;
    private final UsuarioService usuarioService;

    public ReportesAdminService(PedidoRepository pedidoRepository,
                                ProductoService productoService,
                                UsuarioService usuarioService) {
        this.pedidoRepository = pedidoRepository;
        this.productoService = productoService;
        this.usuarioService = usuarioService;
    }

    // =====================================================================
    // 1. Reporte de inventario
    // =====================================================================
    @Transactional(readOnly = true)
    public byte[] generarReporteInventario() {
        List<Producto> productos = productoService.listarTodos().stream()
                .sorted(Comparator.comparing((Producto p) -> valor(p.getNombre()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        try (Workbook wb = new XSSFWorkbook()) {
            Estilos est = crearEstilos(wb);
            String[] encabezados = {"ID", "Producto", "Marca", "Categoría", "Precio", "Stock",
                    "Valor en inventario", "Estado de stock"};
            int[] anchos = {8, 40, 20, 18, 14, 10, 22, 18};
            Sheet hoja = crearHoja(wb, est, "Inventario", "Medina Fragrances - Reporte de inventario",
                    encabezados, anchos);

            if (productos.isEmpty()) {
                filaVacia(hoja, encabezados.length, "No hay productos registrados.");
                return exportar(wb);
            }

            int indice = FILA_DATOS;
            for (Producto p : productos) {
                Row fila = hoja.createRow(indice++);
                int stock = p.getStock() != null ? p.getStock() : 0;
                double precio = p.getPrecio() != null ? p.getPrecio() : 0.0;
                boolean bajo = stock <= STOCK_BAJO;
                String estadoStock = stock == 0 ? "Sin stock" : (bajo ? "Stock bajo" : "Disponible");

                CellStyle sTexto = bajo ? est.alertaTexto : est.texto;
                CellStyle sCentrado = bajo ? est.alertaCentrado : est.centrado;
                CellStyle sMoneda = bajo ? est.alertaMoneda : est.moneda;

                escribirNumero(fila, 0, p.getId() != null ? p.getId() : 0, sCentrado);
                escribirTexto(fila, 1, valor(p.getNombre()), sTexto);
                escribirTexto(fila, 2, valor(p.getMarca()), sTexto);
                escribirTexto(fila, 3, valor(p.getCategoria()), sTexto);
                escribirNumero(fila, 4, precio, sMoneda);
                escribirNumero(fila, 5, stock, sCentrado);
                escribirNumero(fila, 6, precio * stock, sMoneda);
                escribirTexto(fila, 7, estadoStock, sTexto);
            }

            Row total = hoja.createRow(indice);
            escribirTexto(total, 4, "TOTAL", est.totalEtiqueta);
            totalSuma(total, 5, productos.size(), est.totalCentrado);
            totalSuma(total, 6, productos.size(), est.totalMoneda);

            return exportar(wb);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte de inventario", e);
        }
    }

    // =====================================================================
    // 2. Reporte de pedidos pendientes (todos, sin filtro de mes)
    // =====================================================================
    @Transactional(readOnly = true)
    public byte[] generarReportePedidosPendientes() {
        List<Pedido> pendientes = pedidoRepository.findByEstado("PENDIENTE").stream()
                .sorted(Comparator.comparing(Pedido::getFecha, POR_FECHA))
                .toList();

        try (Workbook wb = new XSSFWorkbook()) {
            Estilos est = crearEstilos(wb);
            String[] encabezados = {"Pedido", "Fecha", "Cliente", "Teléfono", "Producto", "Cantidad",
                    "Total", "Método de pago", "Días pendiente"};
            int[] anchos = {10, 18, 26, 14, 32, 10, 14, 20, 14};
            Sheet hoja = crearHoja(wb, est, "Pedidos pendientes",
                    "Medina Fragrances - Pedidos pendientes", encabezados, anchos);

            if (pendientes.isEmpty()) {
                filaVacia(hoja, encabezados.length, "No hay pedidos pendientes.");
                return exportar(wb);
            }

            int indice = FILA_DATOS;
            for (Pedido p : pendientes) {
                Row fila = hoja.createRow(indice++);
                long dias = p.getFecha() != null
                        ? ChronoUnit.DAYS.between(p.getFecha().toLocalDate(), LocalDate.now())
                        : 0;

                escribirNumero(fila, 0, p.getId() != null ? p.getId() : 0, est.centrado);
                escribirFecha(fila, 1, p.getFecha(), est.fecha);
                escribirTexto(fila, 2, nombreCliente(p), est.texto);
                escribirTexto(fila, 3, telefonoDe(p), est.texto);
                escribirTexto(fila, 4, nombreProducto(p), est.texto);
                escribirNumero(fila, 5, p.getCantidad() != null ? p.getCantidad() : 0, est.centrado);
                escribirNumero(fila, 6, p.getTotal() != null ? p.getTotal() : 0.0, est.moneda);
                escribirTexto(fila, 7, p.getMetodoPago() != null ? p.getMetodoPago() : "Manual", est.texto);
                escribirNumero(fila, 8, dias, est.centrado);
            }

            Row total = hoja.createRow(indice);
            escribirTexto(total, 4, "TOTAL", est.totalEtiqueta);
            totalSuma(total, 5, pendientes.size(), est.totalCentrado);
            totalSuma(total, 6, pendientes.size(), est.totalMoneda);

            return exportar(wb);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte de pedidos pendientes", e);
        }
    }

    // =====================================================================
    // 3. Reporte de envios y despachos (PAGADO, ENVIADO y ENTREGADO)
    // =====================================================================
    @Transactional(readOnly = true)
    public byte[] generarReporteEnvios() {
        List<Pedido> pedidos = new ArrayList<>();
        for (String estado : ESTADOS_ENVIO) {
            pedidos.addAll(pedidoRepository.findByEstado(estado));
        }
        pedidos.sort(Comparator.comparingInt((Pedido p) -> ESTADOS_ENVIO.indexOf(p.getEstado()))
                .thenComparing(Pedido::getFecha, POR_FECHA));

        try (Workbook wb = new XSSFWorkbook()) {
            Estilos est = crearEstilos(wb);
            String[] encabezados = {"Pedido", "Fecha", "Cliente", "Teléfono", "Dirección de entrega",
                    "Método de entrega", "Producto", "Cantidad", "Estado"};
            int[] anchos = {10, 18, 26, 14, 36, 20, 32, 10, 14};
            Sheet hoja = crearHoja(wb, est, "Envíos y despachos",
                    "Medina Fragrances - Envíos y despachos", encabezados, anchos);

            if (pedidos.isEmpty()) {
                filaVacia(hoja, encabezados.length, "No hay pedidos para despachar ni enviados.");
                return exportar(wb);
            }

            int indice = FILA_DATOS;
            for (Pedido p : pedidos) {
                Row fila = hoja.createRow(indice++);

                escribirNumero(fila, 0, p.getId() != null ? p.getId() : 0, est.centrado);
                escribirFecha(fila, 1, p.getFecha(), est.fecha);
                escribirTexto(fila, 2, nombreCliente(p), est.texto);
                escribirTexto(fila, 3, telefonoDe(p), est.texto);
                escribirTexto(fila, 4, direccionDe(p), est.texto);
                escribirTexto(fila, 5, p.getMetodoEntrega() != null ? p.getMetodoEntrega() : "No indicado", est.texto);
                escribirTexto(fila, 6, nombreProducto(p), est.texto);
                escribirNumero(fila, 7, p.getCantidad() != null ? p.getCantidad() : 0, est.centrado);
                escribirTexto(fila, 8, valor(p.getEstado()), est.centrado);
            }

            Row total = hoja.createRow(indice);
            escribirTexto(total, 6, "TOTAL", est.totalEtiqueta);
            totalSuma(total, 7, pedidos.size(), est.totalCentrado);

            return exportar(wb);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte de envios y despachos", e);
        }
    }

    // =====================================================================
    // 4. Reporte de clientes registrados (sin el administrador)
    // =====================================================================
    @Transactional(readOnly = true)
    public byte[] generarReporteClientes() {
        List<Usuario> clientes = usuarioService.listarTodos().stream()
                .filter(u -> !"ADMIN".equals(u.getRol()))
                .sorted(Comparator.comparing((Usuario u) -> valor(u.getNombre()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<Integer, Long> pedidosPorCliente = pedidoRepository.findAll().stream()
                .filter(p -> p.getUsuario() != null && p.getUsuario().getId() != null)
                .collect(Collectors.groupingBy(p -> p.getUsuario().getId(), Collectors.counting()));

        try (Workbook wb = new XSSFWorkbook()) {
            Estilos est = crearEstilos(wb);
            String[] encabezados = {"Cliente", "Email", "Teléfono", "DNI", "Dirección", "Pedidos"};
            int[] anchos = {28, 32, 14, 12, 36, 10};
            Sheet hoja = crearHoja(wb, est, "Clientes",
                    "Medina Fragrances - Clientes registrados (" + clientes.size() + ")", encabezados, anchos);

            if (clientes.isEmpty()) {
                filaVacia(hoja, encabezados.length, "No hay clientes registrados.");
                return exportar(wb);
            }

            int indice = FILA_DATOS;
            for (Usuario u : clientes) {
                Row fila = hoja.createRow(indice++);
                long pedidos = u.getId() != null ? pedidosPorCliente.getOrDefault(u.getId(), 0L) : 0L;

                escribirTexto(fila, 0, valor(u.getNombre()), est.texto);
                escribirTexto(fila, 1, valor(u.getEmail()), est.texto);
                escribirTexto(fila, 2, valor(u.getTelefono()), est.texto);
                escribirTexto(fila, 3, valor(u.getDni()), est.texto);
                escribirTexto(fila, 4, valor(u.getDireccion()), est.texto);
                escribirNumero(fila, 5, pedidos, est.centrado);
            }

            Row total = hoja.createRow(indice);
            escribirTexto(total, 4, "TOTAL", est.totalEtiqueta);
            totalSuma(total, 5, clientes.size(), est.totalCentrado);

            return exportar(wb);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte de clientes", e);
        }
    }

    // =====================================================================
    // 5. Reporte de productos mas solicitados (ranking por unidades vendidas)
    //    Cuenta todos los pedidos que no estan CANCELADO.
    // =====================================================================
    private static class ResumenProducto {
        final Producto producto;
        long unidades;
        long pedidos;
        double ingresos;

        ResumenProducto(Producto producto) {
            this.producto = producto;
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteProductosMasSolicitados() {
        List<Pedido> vigentes = pedidoRepository.findAll().stream()
                .filter(p -> !"CANCELADO".equals(p.getEstado()))
                .filter(p -> p.getProducto() != null)
                .toList();

        Map<Integer, ResumenProducto> resumen = new HashMap<>();
        for (Pedido p : vigentes) {
            Producto producto = p.getProducto();
            ResumenProducto r = resumen.computeIfAbsent(producto.getId(), k -> new ResumenProducto(producto));
            r.unidades += p.getCantidad() != null ? p.getCantidad() : 0;
            r.pedidos++;
            r.ingresos += p.getTotal() != null ? p.getTotal() : 0.0;
        }

        List<ResumenProducto> ranking = new ArrayList<>(resumen.values());
        ranking.sort(Comparator.comparingLong((ResumenProducto r) -> r.unidades).reversed()
                .thenComparing(r -> valor(r.producto.getNombre()), String.CASE_INSENSITIVE_ORDER));
        if (ranking.size() > TOP_PRODUCTOS) {
            ranking = new ArrayList<>(ranking.subList(0, TOP_PRODUCTOS));
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Estilos est = crearEstilos(wb);
            String[] encabezados = {"Posición", "Producto", "Marca", "Categoría", "Unidades vendidas",
                    "Pedidos", "Ingresos", "Stock actual"};
            int[] anchos = {10, 40, 20, 18, 18, 10, 16, 14};
            Sheet hoja = crearHoja(wb, est, "Más solicitados",
                    "Medina Fragrances - Productos más solicitados (Top " + TOP_PRODUCTOS + ")",
                    encabezados, anchos);

            if (ranking.isEmpty()) {
                filaVacia(hoja, encabezados.length, "Aún no hay pedidos registrados.");
                return exportar(wb);
            }

            int indice = FILA_DATOS;
            int posicion = 1;
            for (ResumenProducto r : ranking) {
                Row fila = hoja.createRow(indice++);
                Producto producto = r.producto;

                escribirNumero(fila, 0, posicion++, est.centrado);
                escribirTexto(fila, 1, valor(producto.getNombre()), est.texto);
                escribirTexto(fila, 2, valor(producto.getMarca()), est.texto);
                escribirTexto(fila, 3, valor(producto.getCategoria()), est.texto);
                escribirNumero(fila, 4, r.unidades, est.centrado);
                escribirNumero(fila, 5, r.pedidos, est.centrado);
                escribirNumero(fila, 6, r.ingresos, est.moneda);
                escribirNumero(fila, 7, producto.getStock() != null ? producto.getStock() : 0, est.centrado);
            }

            Row total = hoja.createRow(indice);
            escribirTexto(total, 3, "TOTAL", est.totalEtiqueta);
            totalSuma(total, 4, ranking.size(), est.totalCentrado);
            totalSuma(total, 5, ranking.size(), est.totalCentrado);
            totalSuma(total, 6, ranking.size(), est.totalMoneda);

            return exportar(wb);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte de productos mas solicitados", e);
        }
    }

    // =====================================================================
    // Utilidades de datos
    // =====================================================================
    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    private String nombreCliente(Pedido p) {
        return p.getUsuario() != null ? valor(p.getUsuario().getNombre()) : "-";
    }

    private String nombreProducto(Pedido p) {
        return p.getProducto() != null ? valor(p.getProducto().getNombre()) : "-";
    }

    private String telefonoDe(Pedido p) {
        if (p.getTelefonoContacto() != null && !p.getTelefonoContacto().isBlank()) {
            return p.getTelefonoContacto();
        }
        return p.getUsuario() != null ? valor(p.getUsuario().getTelefono()) : "-";
    }

    private String direccionDe(Pedido p) {
        if (p.getDireccionEntrega() != null && !p.getDireccionEntrega().isBlank()) {
            return p.getDireccionEntrega();
        }
        return p.getUsuario() != null ? valor(p.getUsuario().getDireccion()) : "-";
    }

    // =====================================================================
    // Utilidades de Excel
    // =====================================================================
    private static class Estilos {
        CellStyle titulo;
        CellStyle subtitulo;
        CellStyle encabezado;
        CellStyle texto;
        CellStyle centrado;
        CellStyle fecha;
        CellStyle moneda;
        CellStyle alertaTexto;
        CellStyle alertaCentrado;
        CellStyle alertaMoneda;
        CellStyle totalEtiqueta;
        CellStyle totalCentrado;
        CellStyle totalMoneda;
    }

    private Estilos crearEstilos(Workbook wb) {
        Font fuenteTitulo = wb.createFont();
        fuenteTitulo.setBold(true);
        fuenteTitulo.setFontHeightInPoints((short) 14);

        Font fuenteSubtitulo = wb.createFont();
        fuenteSubtitulo.setItalic(true);
        fuenteSubtitulo.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        Font fuenteEncabezado = wb.createFont();
        fuenteEncabezado.setBold(true);
        fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());

        Font fuenteNegrita = wb.createFont();
        fuenteNegrita.setBold(true);

        Estilos e = new Estilos();
        e.titulo = estilo(wb, fuenteTitulo, null, null, null, false);
        e.subtitulo = estilo(wb, fuenteSubtitulo, null, null, null, false);
        e.encabezado = estilo(wb, fuenteEncabezado, null, HorizontalAlignment.CENTER,
                IndexedColors.GREY_80_PERCENT, true);
        e.texto = estilo(wb, null, null, null, null, true);
        e.centrado = estilo(wb, null, null, HorizontalAlignment.CENTER, null, true);
        e.fecha = estilo(wb, null, FORMATO_FECHA_EXCEL, null, null, true);
        e.moneda = estilo(wb, null, FORMATO_MONEDA, null, null, true);
        e.alertaTexto = estilo(wb, null, null, null, IndexedColors.ROSE, true);
        e.alertaCentrado = estilo(wb, null, null, HorizontalAlignment.CENTER, IndexedColors.ROSE, true);
        e.alertaMoneda = estilo(wb, null, FORMATO_MONEDA, null, IndexedColors.ROSE, true);
        e.totalEtiqueta = estilo(wb, fuenteNegrita, null, HorizontalAlignment.RIGHT, null, true);
        e.totalCentrado = estilo(wb, fuenteNegrita, null, HorizontalAlignment.CENTER, null, true);
        e.totalMoneda = estilo(wb, fuenteNegrita, FORMATO_MONEDA, null, null, true);
        return e;
    }

    private CellStyle estilo(Workbook wb, Font fuente, String formato, HorizontalAlignment alineacion,
                             IndexedColors fondo, boolean bordes) {
        CellStyle estilo = wb.createCellStyle();
        if (fuente != null) {
            estilo.setFont(fuente);
        }
        if (formato != null) {
            estilo.setDataFormat(wb.createDataFormat().getFormat(formato));
        }
        if (alineacion != null) {
            estilo.setAlignment(alineacion);
        }
        if (fondo != null) {
            estilo.setFillForegroundColor(fondo.getIndex());
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (bordes) {
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
        }
        return estilo;
    }

    /** Crea la hoja con titulo, fecha de generacion y encabezados. */
    private Sheet crearHoja(Workbook wb, Estilos est, String nombreHoja, String titulo,
                            String[] encabezados, int[] anchos) {
        Sheet hoja = wb.createSheet(nombreHoja);
        for (int i = 0; i < anchos.length; i++) {
            hoja.setColumnWidth(i, anchos[i] * 256);
        }

        Row filaTitulo = hoja.createRow(0);
        Cell celdaTitulo = filaTitulo.createCell(0);
        celdaTitulo.setCellValue(titulo);
        celdaTitulo.setCellStyle(est.titulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, encabezados.length - 1));

        Row filaSubtitulo = hoja.createRow(1);
        Cell celdaSubtitulo = filaSubtitulo.createCell(0);
        celdaSubtitulo.setCellValue("Generado el " + LocalDateTime.now().format(FORMATO_FECHA));
        celdaSubtitulo.setCellStyle(est.subtitulo);

        Row filaEncabezado = hoja.createRow(FILA_ENCABEZADO);
        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = filaEncabezado.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(est.encabezado);
        }
        hoja.createFreezePane(0, FILA_DATOS);
        return hoja;
    }

    private void filaVacia(Sheet hoja, int columnas, String mensaje) {
        Row fila = hoja.createRow(FILA_DATOS);
        fila.createCell(0).setCellValue(mensaje);
        hoja.addMergedRegion(new CellRangeAddress(FILA_DATOS, FILA_DATOS, 0, columnas - 1));
    }

    private void escribirTexto(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private void escribirNumero(Row fila, int columna, double valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private void escribirFecha(Row fila, int columna, LocalDateTime valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        if (valor != null) {
            celda.setCellValue(valor);
        } else {
            celda.setCellValue("-");
        }
        celda.setCellStyle(estilo);
    }

    /** Escribe =SUM(...) sobre las filas de datos de la columna indicada. */
    private void totalSuma(Row fila, int columna, int cantidadFilas, CellStyle estilo) {
        String letra = CellReference.convertNumToColString(columna);
        int primeraFilaExcel = FILA_DATOS + 1;
        int ultimaFilaExcel = FILA_DATOS + cantidadFilas;
        Cell celda = fila.createCell(columna);
        celda.setCellFormula("SUM(" + letra + primeraFilaExcel + ":" + letra + ultimaFilaExcel + ")");
        celda.setCellStyle(estilo);
    }

    private byte[] exportar(Workbook wb) throws IOException {
        // Calcula las formulas para que el resultado se vea en cualquier visor
        wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            wb.write(salida);
            return salida.toByteArray();
        }
    }
}
