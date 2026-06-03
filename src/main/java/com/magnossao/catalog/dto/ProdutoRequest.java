package com.magnossao.catalog.dto;
import java.math.BigDecimal;

public record ProdutoRequest(
    String slug, String nome, String nomeCurto, String colecao,
    BigDecimal preco, String descricao, String descricaoSeo
) {}
