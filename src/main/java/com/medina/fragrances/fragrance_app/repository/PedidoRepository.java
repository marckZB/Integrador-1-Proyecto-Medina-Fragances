package com.medina.fragrances.fragrance_app.repository;

import com.medina.fragrances.fragrance_app.model.Pedido;
import com.medina.fragrances.fragrance_app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByUsuario(Usuario usuario);
    List<Pedido> findByEstado(String estado);
    List<Pedido> findByUsuarioOrderByFechaDesc(Usuario usuario);

    // Pedidos dentro de un rango de fechas: inicio incluido, fin excluido
    @Query("SELECT p FROM Pedido p WHERE p.fecha >= :inicio AND p.fecha < :fin ORDER BY p.fecha ASC")
    List<Pedido> findByRangoFecha(@Param("inicio") LocalDateTime inicio,
                                  @Param("fin") LocalDateTime fin);
}