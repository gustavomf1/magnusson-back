package com.magnossao.service;

import com.magnossao.dto.request.RegraCashbackRequest;
import com.magnossao.entity.Cupom;
import com.magnossao.entity.Pedido;
import com.magnossao.entity.PedidoItem;
import com.magnossao.entity.Produto;
import com.magnossao.entity.RegraCashback;
import com.magnossao.entity.Sku;
import com.magnossao.entity.StatusCupom;
import com.magnossao.entity.StatusPedido;
import com.magnossao.entity.Usuario;
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

    private Pedido pedidoComItem(Usuario usuario, Produto produto, BigDecimal precoUnitario, int quantidade) {
        Pedido pedido = new Pedido();
        pedido.setId(50L);
        pedido.setUsuario(usuario);
        pedido.setStatus(StatusPedido.PAGO);

        Sku sku = new Sku();
        sku.setProduto(produto);

        PedidoItem item = new PedidoItem();
        item.setId(500L);
        item.setPedido(pedido);
        item.setSku(sku);
        item.setPrecoUnitario(precoUnitario);
        item.setQuantidade(quantidade);
        pedido.getItens().add(item);
        return pedido;
    }

    @Test
    void gerarCuponsCriaUmCupomPorItemElegivel() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);

        Produto produto = new Produto();
        produto.setId(7L);
        RegraCashback regra = new RegraCashback();
        regra.setProduto(produto);
        regra.setPercentual(BigDecimal.valueOf(10));
        regra.setPrazoValidadeDias(30);
        produto.setRegraCashback(regra);

        Pedido pedido = pedidoComItem(usuario, produto, BigDecimal.valueOf(100), 2);

        when(pedidoRepository.findById(50L)).thenReturn(Optional.of(pedido));
        when(cupomRepository.save(any(Cupom.class))).thenAnswer(inv -> inv.getArgument(0));

        cashbackService.gerarCupons(pedido);

        ArgumentCaptor<Cupom> captor = ArgumentCaptor.forClass(Cupom.class);
        verify(cupomRepository).save(captor.capture());
        Cupom gerado = captor.getValue();
        assertThat(gerado.getValor()).isEqualByComparingTo("10.00");
        assertThat(gerado.getUsuario()).isSameAs(usuario);
        assertThat(gerado.getPedidoItemOrigem()).isSameAs(pedido.getItens().get(0));
        assertThat(gerado.getStatus()).isEqualTo(StatusCupom.ATIVO);
        assertThat(gerado.getExpiraEm()).isNotNull();
    }

    @Test
    void gerarCuponsNaoGeraQuandoProdutoNaoTemRegra() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        Produto produto = new Produto();
        produto.setId(7L);

        Pedido pedido = pedidoComItem(usuario, produto, BigDecimal.valueOf(100), 1);
        when(pedidoRepository.findById(50L)).thenReturn(Optional.of(pedido));

        cashbackService.gerarCupons(pedido);

        verify(cupomRepository, never()).save(any());
    }

    @Test
    void gerarCuponsNaoGeraParaPedidoAnonimo() {
        Produto produto = new Produto();
        produto.setId(7L);
        RegraCashback regra = new RegraCashback();
        regra.setProduto(produto);
        regra.setPercentual(BigDecimal.valueOf(10));
        produto.setRegraCashback(regra);

        Pedido pedido = pedidoComItem(null, produto, BigDecimal.valueOf(100), 1);

        cashbackService.gerarCupons(pedido);

        verify(pedidoRepository, never()).findById(any());
        verify(cupomRepository, never()).save(any());
    }
}
