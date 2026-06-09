package com.magnossao.service;

import com.magnossao.dto.request.CheckoutItemRequest;
import com.magnossao.dto.request.CheckoutRequest;
import com.magnossao.dto.request.DadosNfRequest;
import com.magnossao.dto.request.EnderecoRequest;
import com.magnossao.entity.*;
import com.magnossao.repository.PedidoRepository;
import com.magnossao.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private SkuRepository skuRepository;
    @Mock private EstoqueService estoqueService;
    @Mock private CarrinhoService carrinhoService;
    @Mock private TransactionTemplate txTemplate;
    @Mock private PagamentoService pagamentoService;
    @Mock private CashbackService cashbackService;

    private PedidoService pedidoService;

    private Usuario usuario;
    private Sku sku;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        pedidoService = new PedidoService(
                pedidoRepository, skuRepository, estoqueService,
                carrinhoService, txTemplate, pagamentoService, cashbackService);

        // Fixture: usuário autenticado
        usuario = new Usuario();
        usuario.setId(9L);

        // Fixture: produto com preço R$ 100
        Produto produto = new Produto();
        produto.setNome("Camisa Polo");
        produto.setPreco(BigDecimal.valueOf(100));

        ProdutoCor cor = new ProdutoCor();
        cor.setNome("Azul");

        ProdutoTamanho tamanho = new ProdutoTamanho();
        tamanho.setLabel("M");

        sku = new Sku();
        sku.setId(1L);
        sku.setProduto(produto);
        sku.setCor(cor);
        sku.setTamanho(tamanho);
        sku.setAtivo(true);
        sku.setQuantidade(10);

        // txTemplate executa o callback diretamente
        when(txTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private CheckoutRequest checkoutRequest(Long cupomId) {
        var item = new CheckoutItemRequest(1L, 2, cupomId);
        var nf = new DadosNfRequest("João", "123.456.789-00", "joao@email.com", "11999999999");
        var end = new EnderecoRequest("Rua A", "10", null, "Centro", "01001-000", "São Paulo", "SP", true);
        return new CheckoutRequest(List.of(item), nf, end);
    }

    private Cupom cupomDeTeste(Long id) {
        Cupom cupom = new Cupom();
        cupom.setId(id);
        cupom.setUsuario(usuario);
        cupom.setValor(BigDecimal.valueOf(20));
        cupom.setStatus(StatusCupom.ATIVO);
        return cupom;
    }

    private Pedido pedidoSalvo(BigDecimal total) {
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setTotal(total);
        DadosNf nf = new DadosNf();
        nf.setNomeCliente("João");
        nf.setCpfCnpj("123.456.789-00");
        nf.setEmail("joao@email.com");
        nf.setTelefone("11999999999");
        pedido.setDadosNf(nf);
        EnderecoSnapshot end = new EnderecoSnapshot();
        end.setLogradouro("Rua A");
        end.setNumero("10");
        end.setBairro("Centro");
        end.setCep("01001-000");
        end.setCidade("São Paulo");
        end.setUf("SP");
        pedido.setEndereco(end);
        return pedido;
    }

    @Test
    void checkoutAplicaCupomEDescontaDoTotal() {
        // Preço unitário R$100 x 2 = R$200 total; cupom desconta R$20 → total esperado R$180
        BigDecimal totalEsperado = BigDecimal.valueOf(180);

        Cupom cupom = cupomDeTeste(900L);
        var resultado = new CashbackService.ResultadoCupom(BigDecimal.valueOf(20), cupom);

        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(cashbackService.validarEAplicar(eq(usuario), eq(900L), any(BigDecimal.class), any()))
                .thenReturn(resultado);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            // Simula persistência: atribui id ao pedido e aos itens
            p.setId(10L);
            p.getItens().forEach(item -> item.setId(501L));
            return p;
        });

        var resposta = pedidoService.checkout(checkoutRequest(900L), usuario);

        verify(cashbackService).validarEAplicar(eq(usuario), eq(900L), eq(BigDecimal.valueOf(200)), any());
        verify(cashbackService).confirmarUso(eq(cupom), any(PedidoItem.class));
        assertThat(resposta.total()).isEqualByComparingTo(totalEsperado);
    }

    @Test
    void checkoutSemCupomNaoInvocaCashbackService() {
        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(10L);
            p.getItens().forEach(item -> item.setId(501L));
            return p;
        });

        var resposta = pedidoService.checkout(checkoutRequest(null), usuario);

        verify(cashbackService, never()).validarEAplicar(any(), any(), any(), any());
        verify(cashbackService, never()).confirmarUso(any(), any());
        assertThat(resposta.total()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }
}
