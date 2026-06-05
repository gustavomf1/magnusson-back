package com.magnossao.dto.response;
public record SkuEstoqueResponse(
    Long id,
    String produtoNome,
    String corNome,
    String tamanhoLabel,
    String codigo,
    int quantidade,
    boolean disponivel
) {}
