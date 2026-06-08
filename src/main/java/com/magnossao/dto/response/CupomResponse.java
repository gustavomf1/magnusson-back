package com.magnossao.dto.response;

import com.magnossao.entity.Cupom;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CupomResponse(Long id, BigDecimal valor, String status, LocalDateTime expiraEm,
                             Long pedidoOrigemId, Long pedidoUsoId, String clienteNome) {

    public static CupomResponse from(Cupom cupom) {
        return new CupomResponse(
                cupom.getId(),
                cupom.getValor(),
                cupom.getStatus().name(),
                cupom.getExpiraEm(),
                cupom.getPedidoItemOrigem().getPedido().getId(),
                cupom.getPedidoItemUso() != null ? cupom.getPedidoItemUso().getPedido().getId() : null,
                cupom.getUsuario().getNome()
        );
    }
}
