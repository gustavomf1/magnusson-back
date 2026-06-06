package com.magnossao.dto.response;

import com.magnossao.entity.Pedido;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PedidoResumoResponse(
    Long id, String status, BigDecimal total,
    String nomeCliente, OffsetDateTime criadoEm
) {
    public static PedidoResumoResponse from(Pedido p) {
        return new PedidoResumoResponse(p.getId(), p.getStatus().name(), p.getTotal(),
            p.getDadosNf().getNomeCliente(), p.getCriadoEm());
    }
}
