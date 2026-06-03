package com.magnossao.service;

import com.magnossao.entity.*;
import com.magnossao.repository.SkuRepository;
import com.magnossao.service.SkuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkuServiceTest {

    @Mock SkuRepository skuRepository;
    @InjectMocks SkuService skuService;

    @Test
    void geraCodigoSkuCorretamente() {
        Produto produto = new Produto();
        produto.setSlug("classic");

        ProdutoCor cor = new ProdutoCor();
        cor.setToken("navy");

        ProdutoTamanho tamanho = new ProdutoTamanho();
        tamanho.setLabel("M");

        String codigo = skuService.gerarCodigo(produto, cor, tamanho);

        assertThat(codigo).isEqualTo("classic-navy-m");
    }

    @Test
    void geraSkusParaTodasAsCombinacoes() {
        Produto produto = new Produto();
        produto.setSlug("classic");
        produto.setId(1L);

        ProdutoCor navy = new ProdutoCor(); navy.setId(1L); navy.setToken("navy");
        ProdutoCor preto = new ProdutoCor(); preto.setId(2L); preto.setToken("black");

        ProdutoTamanho p = new ProdutoTamanho(); p.setId(1L); p.setLabel("P");
        ProdutoTamanho m = new ProdutoTamanho(); m.setId(2L); m.setLabel("M");

        produto.setCores(List.of(navy, preto));
        produto.setTamanhos(List.of(p, m));

        when(skuRepository.existsByCorIdAndTamanhoId(any(), any())).thenReturn(false);
        when(skuRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<Sku> skus = skuService.gerarSkus(produto);

        assertThat(skus).hasSize(4);
        assertThat(skus).extracting(Sku::getCodigo)
            .containsExactlyInAnyOrder("classic-navy-p", "classic-navy-m",
                                       "classic-black-p", "classic-black-m");
    }

    @Test
    void naoGeraSkuDuplicado() {
        Produto produto = new Produto();
        produto.setSlug("classic");
        produto.setId(1L);

        ProdutoCor cor = new ProdutoCor(); cor.setId(1L); cor.setToken("navy");
        ProdutoTamanho tam = new ProdutoTamanho(); tam.setId(1L); tam.setLabel("M");
        produto.setCores(List.of(cor));
        produto.setTamanhos(List.of(tam));

        when(skuRepository.existsByCorIdAndTamanhoId(1L, 1L)).thenReturn(true);

        List<Sku> skus = skuService.gerarSkus(produto);

        assertThat(skus).isEmpty();
        verify(skuRepository, never()).saveAll(any());
    }
}
