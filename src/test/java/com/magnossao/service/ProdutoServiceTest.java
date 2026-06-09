package com.magnossao.service;

import com.magnossao.entity.Categoria;
import com.magnossao.entity.Produto;
import com.magnossao.entity.StatusProduto;
import com.magnossao.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private ProdutoService produtoService;

    private Produto produto(String nome, Categoria categoria) {
        Produto p = new Produto();
        p.setSlug(nome.toLowerCase().replace(" ", "-"));
        p.setNome(nome);
        p.setPreco(BigDecimal.valueOf(100));
        p.setStatus(StatusProduto.PUBLICADO);
        p.setCategoria(categoria);
        return p;
    }

    @Test
    void listarPublicados_semFiltro_retornaTodos() {
        var polo = produto("Polo Classic", Categoria.POLO);
        var camisa = produto("Camisa Oxford", Categoria.CAMISA);
        when(produtoRepository.findByStatusOrderByNomeAsc(StatusProduto.PUBLICADO))
            .thenReturn(List.of(polo, camisa));

        var resultado = produtoService.listarPublicados(null);

        assertThat(resultado).hasSize(2);
    }

    @Test
    void listarPublicados_comCategoria_filtraSomenteCategoriaEscolhida() {
        var polo = produto("Polo Classic", Categoria.POLO);
        var camisa = produto("Camisa Oxford", Categoria.CAMISA);
        when(produtoRepository.findByStatusAndCategoriaOrderByNomeAsc(
                StatusProduto.PUBLICADO, Categoria.POLO))
            .thenReturn(List.of(polo));

        var resultado = produtoService.listarPublicados(Categoria.POLO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).categoria()).isEqualTo(Categoria.POLO);
    }
}
