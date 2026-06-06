package com.magnossao.dto.response;

import com.magnossao.entity.Endereco;

public record EnderecoResponse(
    Long id, String logradouro, String numero, String complemento,
    String bairro, String cep, String cidade, String uf, boolean principal
) {
    public static EnderecoResponse from(Endereco e) {
        return new EnderecoResponse(e.getId(), e.getLogradouro(), e.getNumero(),
            e.getComplemento(), e.getBairro(), e.getCep(), e.getCidade(), e.getUf(), e.isPrincipal());
    }
}
