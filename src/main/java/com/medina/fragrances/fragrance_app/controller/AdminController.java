package com.medina.fragrances.fragrance_app.controller;

import com.medina.fragrances.fragrance_app.model.Pedido;
import com.medina.fragrances.fragrance_app.model.Producto;
import com.medina.fragrances.fragrance_app.model.Usuario;
import com.medina.fragrances.fragrance_app.service.ContactoService;
import com.medina.fragrances.fragrance_app.service.PedidoService;
import com.medina.fragrances.fragrance_app.service.ProductoService;
import com.medina.fragrances.fragrance_app.service.ReporteVentasService;
import com.medina.fragrances.fragrance_app.service.ReportesAdminService;
import com.medina.fragrances.fragrance_app.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    private final ProductoService productoService;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;
    private final ContactoService contactoService;
    private final ReporteVentasService reporteVentasService;
    private final ReportesAdminService reportesAdminService;

    public AdminController(ProductoService productoService,
                           PedidoService pedidoService,
                           UsuarioService usuarioService,
                           ContactoService contactoService,
                           ReporteVentasService reporteVentasService,
                           ReportesAdminService reportesAdminService) {
        this.productoService = productoService;
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
        this.contactoService = contactoService;
        this.reporteVentasService = reporteVentasService;
        this.reportesAdminService = reportesAdminService;
    }

    @GetMapping("/admin")
    public String dashboard(@RequestParam(value = "editarProducto", required = false) Integer editarProducto,
                            HttpSession session,
                            Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login?redirect=/admin";
        }

        productoService.cargarProductosIniciales();

        List<Producto> productos = productoService.listarTodos();
        List<Pedido> pedidos = pedidoService.listarTodos().stream()
                .sorted(Comparator.comparing(Pedido::getId, Comparator.nullsLast(Integer::compareTo)).reversed())
                .toList();
        List<Usuario> clientes = usuarioService.listarTodos().stream()
                .filter(usuario -> !"ADMIN".equals(usuario.getRol()))
                .toList();
        Map<Integer, Long> pedidosPorCliente = pedidoService.contarPedidosPorUsuario();

        Producto productoForm = editarProducto == null
                ? new Producto()
                : productoService.buscarPorId(editarProducto).orElse(new Producto());

        double ventasTotales = pedidos.stream()
                .map(Pedido::getTotal)
                .filter(total -> total != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        long stockBajo = productos.stream()
                .filter(producto -> producto.getStock() != null && producto.getStock() <= 5)
                .count();

        Usuario admin = obtenerAdminActual(session);

        model.addAttribute("admin", admin);
        model.addAttribute("productoForm", productoForm);
        model.addAttribute("productos", productos);
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("clientes", clientes);
        model.addAttribute("mensajes", contactoService.listarTodos());
        model.addAttribute("pedidosPorCliente", pedidosPorCliente);
        model.addAttribute("ventasTotales", ventasTotales);
        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("pedidosPendientes", pedidoService.listarPorEstado("PENDIENTE").size());
        model.addAttribute("mesActual", YearMonth.now().toString());
        return "admin/dashboard";
    }

    @PostMapping("/admin/productos/guardar")
    public String guardarProducto(@ModelAttribute Producto producto, HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/login?redirect=/admin";
        }
        productoService.guardar(producto);
        return "redirect:/admin#productos";
    }

    @PostMapping("/admin/productos/eliminar/{id}")
    public String eliminarProducto(@PathVariable Integer id, HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/login?redirect=/admin";
        }
        productoService.eliminar(id);
        return "redirect:/admin#productos";
    }

    @PostMapping("/admin/pedidos/{id}/estado")
    public String actualizarEstadoPedido(@PathVariable Integer id,
                                         @RequestParam String estado,
                                         HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/login?redirect=/admin";
        }
        pedidoService.actualizarEstado(id, estado);
        return "redirect:/admin#pedidos";
    }

    @PostMapping("/admin/pedidos/crear")
    public String crearPedido(@RequestParam Integer usuarioId,
                              @RequestParam Integer productoId,
                              @RequestParam Integer cantidad,
                              HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/login?redirect=/admin";
        }

        Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        Producto producto = productoService.buscarPorId(productoId).orElse(null);
        if (usuario != null && producto != null && cantidad != null && cantidad > 0) {
            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);
            pedido.setProducto(producto);
            pedido.setCantidad(cantidad);
            pedido.setEstado("PAGADO");
            pedido.setTotal(producto.getPrecio() * cantidad);
            pedidoService.guardar(pedido);

            int stockActual = producto.getStock() == null ? 0 : producto.getStock();
            producto.setStock(Math.max(0, stockActual - cantidad));
            productoService.guardar(producto);
        }
        return "redirect:/admin#pedidos";
    }

    @GetMapping("/admin/ventas/exportar")
    public ResponseEntity<byte[]> exportarVentasMes(@RequestParam(value = "mes", required = false) String mes,
                                                    HttpSession session) {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/login?redirect=/admin"))
                    .build();
        }

        // El input type="month" envia el valor como "2026-10"
        YearMonth periodo;
        try {
            periodo = (mes == null || mes.isBlank()) ? YearMonth.now() : YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            periodo = YearMonth.now();
        }

        byte[] contenido = reporteVentasService.generarExcelVentasMes(periodo);
        String nombreArchivo = "ventas-" + periodo + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(contenido.length)
                .body(contenido);
    }

    @GetMapping("/admin/reportes/inventario")
    public ResponseEntity<byte[]> reporteInventario(HttpSession session) {
        if (!esAdmin(session)) {
            return redirigirALogin();
        }
        return respuestaExcel(reportesAdminService.generarReporteInventario(), "inventario");
    }

    @GetMapping("/admin/reportes/productos-solicitados")
    public ResponseEntity<byte[]> reporteProductosSolicitados(HttpSession session) {
        if (!esAdmin(session)) {
            return redirigirALogin();
        }
        return respuestaExcel(reportesAdminService.generarReporteProductosMasSolicitados(),
                "productos-mas-solicitados");
    }

    @GetMapping("/admin/reportes/pedidos-pendientes")
    public ResponseEntity<byte[]> reportePedidosPendientes(HttpSession session) {
        if (!esAdmin(session)) {
            return redirigirALogin();
        }
        return respuestaExcel(reportesAdminService.generarReportePedidosPendientes(), "pedidos-pendientes");
    }

    @GetMapping("/admin/reportes/envios")
    public ResponseEntity<byte[]> reporteEnvios(HttpSession session) {
        if (!esAdmin(session)) {
            return redirigirALogin();
        }
        return respuestaExcel(reportesAdminService.generarReporteEnvios(), "envios-despachos");
    }

    @GetMapping("/admin/reportes/clientes")
    public ResponseEntity<byte[]> reporteClientes(HttpSession session) {
        if (!esAdmin(session)) {
            return redirigirALogin();
        }
        return respuestaExcel(reportesAdminService.generarReporteClientes(), "clientes-registrados");
    }

    @PostMapping("/admin/perfil")
    public String actualizarPerfil(@ModelAttribute Usuario adminForm, HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/login?redirect=/admin";
        }

        Usuario admin = usuarioService.buscarPorId(adminForm.getId())
                .orElse(usuarioService.asegurarAdminPrincipal());
        admin.setNombre(adminForm.getNombre());
        admin.setEmail(adminForm.getEmail());
        admin.setTelefono(adminForm.getTelefono());
        admin.setDni(adminForm.getDni());
        admin.setDireccion(adminForm.getDireccion());
        admin.setPassword(adminForm.getPassword());
        admin.setRol("ADMIN");
        usuarioService.guardar(admin);

        session.setAttribute("usuarioId", admin.getId());
        session.setAttribute("nombreUsuario", admin.getNombre());
        session.setAttribute("rolUsuario", "ADMIN");
        session.setAttribute("adminAutenticado", true);
        return "redirect:/admin#configuracion";
    }

    private ResponseEntity<byte[]> redirigirALogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/login?redirect=/admin"))
                .build();
    }

    private ResponseEntity<byte[]> respuestaExcel(byte[] contenido, String nombreBase) {
        String nombreArchivo = nombreBase + "-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(contenido.length)
                .body(contenido);
    }

    private boolean esAdmin(HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute("adminAutenticado"))
                && "ADMIN".equals(session.getAttribute("rolUsuario"))) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private Usuario obtenerAdminActual(HttpSession session) {
        Object id = session.getAttribute("usuarioId");
        if (id instanceof Integer adminId) {
            return usuarioService.buscarPorId(adminId)
                    .orElse(usuarioService.asegurarAdminPrincipal());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return usuarioService.buscarPorEmail(authentication.getName())
                    .orElse(usuarioService.asegurarAdminPrincipal());
        }

        return usuarioService.asegurarAdminPrincipal();
    }
}
