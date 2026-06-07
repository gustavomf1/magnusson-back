package com.magnossao.dto.response;

import com.magnossao.entity.Pedido;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PedidoResponse(
    Long id, String status, BigDecimal total,
    String nomeCliente, String cpfCnpj, String email, String telefone,
    String endLogradouro, String endNumero, String endComplemento,
    String endBairro, String endCep, String endCidade, String endUf,
    List<PedidoItemResponse> itens, OffsetDateTime criadoEm, String initPoint
) {
    public static PedidoResponse from(Pedido p) {
        return from(p, null);
    }

    public static PedidoResponse from(Pedido p, String initPoint) {
        return new PedidoResponse(
            p.getId(), p.getStatus().name(), p.getTotal(),
            p.getDadosNf().getNomeCliente(), p.getDadosNf().getCpfCnpj(),
            p.getDadosNf().getEmail(), p.getDadosNf().getTelefone(),
            p.getEndereco().getLogradouro(), p.getEndereco().getNumero(),
            p.getEndereco().getComplemento(), p.getEndereco().getBairro(),
            p.getEndereco().getCep(), p.getEndereco().getCidade(), p.getEndereco().getUf(),
            p.getItens().stream().map(PedidoItemResponse::from).toList(),
            p.getCriadoEm(), initPoint
        );
    }
}
