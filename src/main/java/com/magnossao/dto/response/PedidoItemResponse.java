package com.magnossao.dto.response;

import com.magnossao.entity.PedidoItem;
import java.math.BigDecimal;

public record PedidoItemResponse(Long id, Long skuId, String nomeProduto, String cor, String tamanho,
                                  BigDecimal precoUnitario, int quantidade, CupomAplicadoResponse cupomAplicado) {

    public static PedidoItemResponse from(PedidoItem item) {
        return new PedidoItemResponse(
                item.getId(),
                item.getSku().getId(),
                item.getNomeProduto(),
                item.getCor(),
                item.getTamanho(),
                item.getPrecoUnitario(),
                item.getQuantidade(),
                item.getCupomAplicado() != null ? CupomAplicadoResponse.from(item.getCupomAplicado()) : null
        );
    }
}
