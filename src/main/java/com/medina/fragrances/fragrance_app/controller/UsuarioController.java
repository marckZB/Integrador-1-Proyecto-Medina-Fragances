package com.medina.fragrances.fragrance_app.controller;

import com.medina.fragrances.fragrance_app.dto.CambioPasswordDto;
import com.medina.fragrances.fragrance_app.model.Pedido;
import com.medina.fragrances.fragrance_app.model.Usuario;
import com.medina.fragrances.fragrance_app.service.PedidoService;
import com.medina.fragrances.fragrance_app.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PedidoService pedidoService;

    public UsuarioController(UsuarioService usuarioService, PedidoService pedidoService) {
        this.usuarioService = usuarioService;
        this.pedidoService = pedidoService;
    }

    // DASHBOARD DEL CLIENTE

    @GetMapping({ "", "/" })
public String dashboard(HttpSession session, Model model) {
    Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(session);
    if (usuarioOpt.isEmpty()) {
        return "redirect:/login?redirect=/usuario";
    }

    Usuario usuario = usuarioOpt.get();
    List<Pedido> pedidos = pedidoService.listarPorUsuarioOrdenado(usuario);

    long totalPedidos = pedidos.size();
    long pedidosEntregados = pedidos.stream()
            .filter(p -> "ENTREGADO".equals(p.getEstado()))
            .count();
    long pedidosPendientes = pedidos.stream()
            .filter(p -> "PENDIENTE".equals(p.getEstado()))
            .count();
    long pedidosEnProceso = pedidos.stream()
            .filter(p -> "PAGADO".equals(p.getEstado()) || "ENVIADO".equals(p.getEstado()))
            .count();
    List<Pedido> ultimosPedidos = pedidos.stream()
            .limit(3)
            .toList();

    model.addAttribute("usuario", usuario);
    model.addAttribute("totalPedidos", totalPedidos);
    model.addAttribute("pedidosEntregados", pedidosEntregados);
    model.addAttribute("pedidosPendientes", pedidosPendientes);
    model.addAttribute("pedidosEnProceso", pedidosEnProceso);
    model.addAttribute("ultimosPedidos", ultimosPedidos);
    return "usuario/dashboard";
}

    // PERFIL: VER Y EDITAR

    @GetMapping("/perfil")
    public String verPerfil(HttpSession session, Model model) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(session);
        if (usuarioOpt.isEmpty()) {
            return "redirect:/login?redirect=/usuario/perfil";
        }
        model.addAttribute("usuario", usuarioOpt.get());
        return "usuario/perfil";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(@RequestParam String nombre,
            @RequestParam String telefono,
            @RequestParam String direccion,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(session);
        if (usuarioOpt.isEmpty()) {
            return "redirect:/login?redirect=/usuario/perfil";
        }

        Usuario actual = usuarioOpt.get();
        try {
            Usuario actualizado = usuarioService.actualizarPerfil(actual.getId(), nombre, telefono, direccion);
            refrescarSesion(session, actualizado);
            redirectAttributes.addFlashAttribute("mensajeExito", "Perfil actualizado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo actualizar el perfil.");
        }
        return "redirect:/usuario/perfil";
    }

    // CAMBIO DE CONTRASENA

    @GetMapping("/password")
    public String verCambioPassword(HttpSession session, Model model) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(session);
        if (usuarioOpt.isEmpty()) {
            return "redirect:/login?redirect=/usuario/password";
        }
        model.addAttribute("usuario", usuarioOpt.get());
        model.addAttribute("cambioPassword", new CambioPasswordDto());
        return "usuario/password";
    }

    @PostMapping("/password")
    public String procesarCambioPassword(@Valid @ModelAttribute("cambioPassword") CambioPasswordDto cambioPassword,
            BindingResult result,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(session);
        if (usuarioOpt.isEmpty()) {
            return "redirect:/login?redirect=/usuario/password";
        }

        Usuario usuario = usuarioOpt.get();
        model.addAttribute("usuario", usuario);

        if (result.hasErrors()) {
            return "usuario/password";
        }

        // La confirmacion no coincide
        if (!cambioPassword.getPasswordNueva().equals(cambioPassword.getConfirmarPassword())) {
            model.addAttribute("errorCoincidencia", "Las contrasenas nuevas no coinciden.");
            return "usuario/password";
        }

        // La contrasena nueva no debe ser igual a la actual
        if (cambioPassword.getPasswordNueva().equals(cambioPassword.getPasswordActual())) {
            model.addAttribute("errorNueva", "La nueva contrasena debe ser diferente a la actual.");
            return "usuario/password";
        }

        // Verificar contrasena actual contra BCrypt
        boolean cambiado = usuarioService.cambiarPassword(
                usuario.getId(),
                cambioPassword.getPasswordActual(),
                cambioPassword.getPasswordNueva());

        if (!cambiado) {
            model.addAttribute("errorActual", "La contrasena actual no es correcta.");
            return "usuario/password";
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Contrasena actualizada correctamente.");
        return "redirect:/usuario/perfil";
    }

    @GetMapping("/pedidos")
    public String listarPedidos(HttpSession session, Model model) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(session);
        if (usuarioOpt.isEmpty()) {
            return "redirect:/login?redirect=/usuario/pedidos";
        }

        Usuario usuario = usuarioOpt.get();
        List<Pedido> pedidos = pedidoService.listarPorUsuarioOrdenado(usuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("pedidos", pedidos);
        return "usuario/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String detallePedido(@PathVariable Integer id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(session);
        if (usuarioOpt.isEmpty()) {
            return "redirect:/login?redirect=/usuario/pedidos";
        }

        Usuario usuario = usuarioOpt.get();
        Optional<Pedido> pedidoOpt = pedidoService.buscarPedidoDeUsuario(id, usuario);

        if (pedidoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Pedido no encontrado.");
            return "redirect:/usuario/pedidos";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("pedido", pedidoOpt.get());
        return "usuario/detalle-pedido";
    }

    /**
     * Obtiene el usuario autenticado desde la sesion HTTP o desde Spring Security.
     * Mismo patron que CheckoutController para consistencia.
     */
    private Optional<Usuario> obtenerUsuarioSesion(HttpSession session) {
        Object usuarioId = session.getAttribute("usuarioId");
        if (usuarioId instanceof Integer id) {
            return usuarioService.buscarPorId(id);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return usuarioService.buscarPorEmail(authentication.getName());
        }

        return Optional.empty();
    }

    /**
     * Refresca los datos de sesion tras una actualizacion de perfil
     */
    private void refrescarSesion(HttpSession session, Usuario usuario) {
        session.setAttribute("usuarioId", usuario.getId());
        session.setAttribute("nombreUsuario", usuario.getNombre());
        session.setAttribute("rolUsuario", usuario.getRol());
        session.setAttribute("adminAutenticado", "ADMIN".equals(usuario.getRol()));
    }
}