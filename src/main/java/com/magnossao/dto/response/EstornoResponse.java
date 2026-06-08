package com.magnossao.dto.response;

import com.magnossao.entity.Estorno;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EstornoResponse(
    Long id, Long pedidoId, Long pedidoItemId, Long skuId,
    int quantidade, BigDecimal valor, String mpRefundId,
    String statusPedido, OffsetDateTime criadoEm
) {
    public static EstornoResponse from(Estorno e) {
        return new EstornoResponse(
            e.getId(), e.getPedido().getId(), e.getPedidoItem().getId(), e.getSku().getId(),
            e.getQuantidade(), e.getValor(), e.getMpRefundId(),
            e.getPedido().getStatus().name(), e.getCriadoEm()
        );
    }
}
