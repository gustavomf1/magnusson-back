package com.magnossao.dto.request;

import com.magnossao.entity.Categoria;
import java.math.BigDecimal;

public record ProdutoRequest(
    String slug, String nome, String nomeCurto, String colecao,
    BigDecimal preco, String descricao, String descricaoSeo,
    Categoria categoria
) {}
