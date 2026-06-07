package com.magnossao.repository;

import com.magnossao.entity.Estorno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EstornoRepository extends JpaRepository<Estorno, Long> {
    @Query("SELECT COALESCE(SUM(e.quantidade), 0) FROM Estorno e WHERE e.pedidoItem.id = :pedidoItemId")
    int somaQuantidadeEstornadaPorItem(@Param("pedidoItemId") Long pedidoItemId);

    List<Estorno> findByPedidoIdOrderByCriadoEmDesc(Long pedidoId);
}
