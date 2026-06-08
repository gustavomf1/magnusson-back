package com.magnossao.service;

import com.magnossao.dto.request.RegraCashbackRequest;
import com.magnossao.entity.Produto;
import com.magnossao.entity.RegraCashback;
import com.magnossao.repository.CupomRepository;
import com.magnossao.repository.PedidoRepository;
import com.magnossao.repository.ProdutoRepository;
import com.magnossao.repository.RegraCashbackRepository;
import com.magnossao.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashbackServiceTest {

    @Mock
    private RegraCashbackRepository regraCashbackRepository;
    @Mock
    private CupomRepository cupomRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CashbackService cashbackService;

    @Test
    void salvarRegraCriaNovaRegraQuandoProdutoNaoTemUma() {
        Produto produto = new Produto();
        produto.setId(7L);
        produto.setNome("Camisa Polo");

        when(produtoRepository.findById(7L)).thenReturn(Optional.of(produto));
        when(regraCashbackRepository.findByProdutoId(7L)).thenReturn(Optional.empty());
        when(regraCashbackRepository.save(any(RegraCashback.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = cashbackService.salvarRegra(7L, new RegraCashbackRequest(BigDecimal.valueOf(10), 30));

        ArgumentCaptor<RegraCashback> captor = ArgumentCaptor.forClass(RegraCashback.class);
        verify(regraCashbackRepository).save(captor.capture());
        RegraCashback salva = captor.getValue();
        assertThat(salva.getProduto()).isSameAs(produto);
        assertThat(salva.getPercentual()).isEqualByComparingTo("10");
        assertThat(salva.getPrazoValidadeDias()).isEqualTo(30);
        assertThat(resposta.percentual()).isEqualByComparingTo("10");
    }

    @Test
    void salvarRegraSubstituiValoresDeRegraExistente() {
        Produto produto = new Produto();
        produto.setId(7L);
        produto.setNome("Camisa Polo");

        RegraCashback existente = new RegraCashback();
        existente.setId(1L);
        existente.setProduto(produto);
        existente.setPercentual(BigDecimal.valueOf(5));
        existente.setPrazoValidadeDias(15);

        when(produtoRepository.findById(7L)).thenReturn(Optional.of(produto));
        when(regraCashbackRepository.findByProdutoId(7L)).thenReturn(Optional.of(existente));
        when(regraCashbackRepository.save(any(RegraCashback.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = cashbackService.salvarRegra(7L, new RegraCashbackRequest(BigDecimal.valueOf(20), null));

        assertThat(existente.getPercentual()).isEqualByComparingTo("20");
        assertThat(existente.getPrazoValidadeDias()).isNull();
        assertThat(resposta.id()).isEqualTo(1L);
    }

    @Test
    void removerRegraDeletaQuandoExiste() {
        Produto produto = new Produto();
        produto.setId(7L);
        RegraCashback existente = new RegraCashback();
        existente.setId(1L);
        existente.setProduto(produto);

        when(regraCashbackRepository.findByProdutoId(7L)).thenReturn(Optional.of(existente));

        cashbackService.removerRegra(7L);

        verify(regraCashbackRepository).delete(existente);
    }
}
