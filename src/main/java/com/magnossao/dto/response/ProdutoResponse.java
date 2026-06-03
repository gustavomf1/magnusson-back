package com.magnossao.dto.response;
import java.math.BigDecimal;
import java.util.List;

public record ProdutoResponse(
    Long id, String slug, String nome, String nomeCurto, String colecao,
    BigDecimal preco, String descricao, String descricaoSeo, String status,
    List<ImagemDto> imagens, List<CorDto> cores, List<TamanhoDto> tamanhos,
    List<SkuDto> skus, List<BeneficioDto> beneficios, List<DetalheDto> detalhes,
    List<ReviewDto> reviews, List<FaqDto> faqs
) {}
