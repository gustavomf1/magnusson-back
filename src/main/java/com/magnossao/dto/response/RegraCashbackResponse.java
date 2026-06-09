package com.magnossao.dto.response;

import com.magnossao.entity.RegraCashback;

import java.math.BigDecimal;

public record RegraCashbackResponse(Long id, Long produtoId, String produtoNome, BigDecimal percentual,
                                     Integer prazoValidadeDias) {

    public static RegraCashbackResponse from(RegraCashback regra) {
        return new RegraCashbackResponse(
                regra.getId(),
                regra.getProduto().getId(),
                regra.getProduto().getNome(),
                regra.getPercentual(),
                regra.getPrazoValidadeDias()
        );
    }
}
