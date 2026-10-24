package com.medina.fragrances.fragrance_app.service;

import com.medina.fragrances.fragrance_app.model.Pedido;
import com.medina.fragrances.fragrance_app.model.Usuario;
import com.medina.fragrances.fragrance_app.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public Pedido guardar(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPorUsuario(Usuario usuario) {
        return pedidoRepository.findByUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorId(Integer id) {
        return pedidoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPorEstado(String estado) {
        return pedidoRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> contarPedidosPorUsuario() {
        return pedidoRepository.findAll().stream()
                .filter(pedido -> pedido.getUsuario() != null && pedido.getUsuario().getId() != null)
                .collect(Collectors.groupingBy(pedido -> pedido.getUsuario().getId(), Collectors.counting()));
    }

    @Transactional
    public void actualizarEstado(Integer id, String estado) {
        pedidoRepository.findById(id).ifPresent(pedido -> {
            pedido.setEstado(estado);
            pedidoRepository.save(pedido);
        });
    }
<<<<<<< HEAD
=======

    /**
     * Historial de pedidos del cliente ordenado del mas reciente al mas antiguo.
     * Usado en /usuario/pedidos y en el dashboard del cliente.
     */
    @Transactional(readOnly = true)
    public List<Pedido> listarPorUsuarioOrdenado(Usuario usuario) {
        return pedidoRepository.findByUsuarioOrderByFechaDesc(usuario);
    }

    /**
     * Devuelve un pedido SOLO si pertenece al usuario indicado.
     * Evita que un cliente vea pedidos ajenos manipulando la URL
     * /usuario/pedidos/{id}.
     */
    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPedidoDeUsuario(Integer pedidoId, Usuario usuario) {
        if (pedidoId == null || usuario == null) {
            return Optional.empty();
        }
        return pedidoRepository.findById(pedidoId)
                .filter(pedido -> pedido.getUsuario() != null
                        && pedido.getUsuario().getId() != null
                        && pedido.getUsuario().getId().equals(usuario.getId()));
    }

>>>>>>> origin/seccion_usuario
}
