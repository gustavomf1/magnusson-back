package com.magnossao.service;

import com.magnossao.dto.response.SkuEstoqueResponse;
import com.magnossao.entity.*;
import com.magnossao.exception.EstoqueInsuficienteException;
import com.magnossao.repository.SkuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock SkuRepository skuRepository;
    @InjectMocks EstoqueService estoqueService;

    @Test
    void ajustarQuantidadeDefineSaldo() {
        Sku sku = skuDeTeste(10L, 5);
        when(skuRepository.findById(10L)).thenReturn(Optional.of(sku));
        when(skuRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SkuEstoqueResponse resp = estoqueService.ajustarQuantidade(10L, 20);

        assertThat(resp.quantidade()).isEqualTo(20);
        assertThat(resp.disponivel()).isTrue();
    }

    @Test
    void ajustarQuantidadeNegativaLancaIllegalArgument() {
        assertThatThrownBy(() -> estoqueService.ajustarQuantidade(1L, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativa");
    }

    @Test
    void ajustarRelativoIncrementaCorretamente() {
        Sku sku = skuDeTeste(1L, 10);
        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SkuEstoqueResponse resp = estoqueService.ajustarRelativo(1L, 5);

        assertThat(resp.quantidade()).isEqualTo(15);
    }

    @Test
    void ajustarRelativoNaoPermiteEstoqueNegativo() {
        Sku sku = skuDeTeste(1L, 3);
        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));

        assertThatThrownBy(() -> estoqueService.ajustarRelativo(1L, -10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrementarComEstoqueSuficienteDecrementa() {
        Sku sku = skuDeTeste(1L, 10);
        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        estoqueService.decrementarEstoque(1L, 3);

        assertThat(sku.getQuantidade()).isEqualTo(7);
    }

    @Test
    void decrementarComEstoqueInsuficienteLancaException() {
        Sku sku = skuDeTeste(1L, 2);
        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));

        assertThatThrownBy(() -> estoqueService.decrementarEstoque(1L, 5))
                .isInstanceOf(EstoqueInsuficienteException.class);
    }

    @Test
    void restaurarIncrementaQuantidade() {
        Sku sku = skuDeTeste(1L, 5);
        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        estoqueService.restaurarEstoque(1L, 3);

        assertThat(sku.getQuantidade()).isEqualTo(8);
    }

    @Test
    void disponiveIsFalseQuandoQuantidadeZero() {
        Sku sku = skuDeTeste(1L, 0);
        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SkuEstoqueResponse resp = estoqueService.ajustarQuantidade(1L, 0);

        assertThat(resp.disponivel()).isFalse();
    }

    private Sku skuDeTeste(Long id, int quantidade) {
        Sku sku = new Sku();
        sku.setId(id);
        Produto p = new Produto(); p.setNome("Produto Teste");
        ProdutoCor c = new ProdutoCor(); c.setNome("Navy");
        ProdutoTamanho t = new ProdutoTamanho(); t.setLabel("M");
        sku.setProduto(p); sku.setCor(c); sku.setTamanho(t);
        sku.setCodigo("test-navy-m");
        sku.setQuantidade(quantidade);
        return sku;
    }
}
