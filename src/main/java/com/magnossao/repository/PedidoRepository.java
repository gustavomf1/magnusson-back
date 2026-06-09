package com.magnossao.repository;

import com.magnossao.entity.Pedido;
import com.magnossao.entity.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.OffsetDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {
    List<Pedido> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    List<Pedido> findByStatusAndCriadoEmBefore(StatusPedido status, OffsetDateTime limite);
}
