package com.magnossao.dto.request;

public record EnderecoRequest(
    String logradouro, String numero, String complemento,
    String bairro, String cep, String cidade, String uf,
    Boolean principal
) {}
