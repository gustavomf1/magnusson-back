package com.magnossao.catalog.dto;
import java.math.BigDecimal;

public record ProdutoResumoResponse(
    Long id, String slug, String nome, String nomeCurto, String colecao,
    BigDecimal preco, String status, String imagemPrincipal
) {}
