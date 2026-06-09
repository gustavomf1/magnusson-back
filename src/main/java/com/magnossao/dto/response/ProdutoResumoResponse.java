package com.magnossao.dto.response;

import com.magnossao.entity.Categoria;
import java.math.BigDecimal;

public record ProdutoResumoResponse(
    Long id, String slug, String nome, String nomeCurto, String colecao,
    BigDecimal preco, String status, String imagemPrincipal,
    Categoria categoria
) {}
