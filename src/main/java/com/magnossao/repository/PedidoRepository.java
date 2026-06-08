package com.magnossao.repository;

import com.magnossao.entity.Pedido;
import com.magnossao.entity.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    List<Pedido> findByStatusAndCriadoEmBefore(StatusPedido status, OffsetDateTime limite);

    @Query("""
        SELECT p FROM Pedido p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:inicio IS NULL OR p.criadoEm >= :inicio)
          AND (:fim IS NULL OR p.criadoEm <= :fim)
          AND (:cliente IS NULL OR LOWER(p.dadosNf.nomeCliente) LIKE LOWER(CONCAT('%', :cliente, '%')))
        ORDER BY p.criadoEm DESC
    """)
    Page<Pedido> findComFiltros(
        @Param("status") StatusPedido status,
        @Param("inicio") OffsetDateTime inicio,
        @Param("fim") OffsetDateTime fim,
        @Param("cliente") String cliente,
        Pageable pageable
    );
}
