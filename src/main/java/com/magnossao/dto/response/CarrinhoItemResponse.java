package com.magnossao.dto.response;

import com.magnossao.entity.CarrinhoItem;
import java.math.BigDecimal;

public record CarrinhoItemResponse(
    Long id, Long skuId, String nomeProduto,
    String cor, String tamanho, int quantidade, BigDecimal precoUnitario
) {
    public static CarrinhoItemResponse from(CarrinhoItem item) {
        return new CarrinhoItemResponse(
            item.getId(),
            item.getSku().getId(),
            item.getSku().getProduto().getNome(),
            item.getSku().getCor().getNome(),
            item.getSku().getTamanho().getLabel(),
            item.getQuantidade(),
            item.getSku().getProduto().getPreco()
        );
    }
}
