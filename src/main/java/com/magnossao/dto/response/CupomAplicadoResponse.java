package com.magnossao.dto.response;

import com.magnossao.entity.Cupom;

import java.math.BigDecimal;

public record CupomAplicadoResponse(Long id, BigDecimal valor) {

    public static CupomAplicadoResponse from(Cupom cupom) {
        return new CupomAplicadoResponse(cupom.getId(), cupom.getValor());
    }
}
