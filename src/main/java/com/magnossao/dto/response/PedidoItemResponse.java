package com.magnossao.dto.response;

import com.magnossao.entity.PedidoItem;
import java.math.BigDecimal;

public record PedidoItemResponse(
    Long id, Long skuId, String nomeProduto,
    String cor, String tamanho, BigDecimal precoUnitario, int quantidade
) {
    public static PedidoItemResponse from(PedidoItem i) {
        return new PedidoItemResponse(i.getId(), i.getSku().getId(),
            i.getNomeProduto(), i.getCor(), i.getTamanho(), i.getPrecoUnitario(), i.getQuantidade());
    }
}
