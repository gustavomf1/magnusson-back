package com.magnossao.repository;

import com.magnossao.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class EstornoRepositoryIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired EstornoRepository estornoRepository;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired ProdutoRepository produtoRepository;

    private PedidoItem pedidoItem;
    private Pedido pedido;
    private Sku sku;

    @BeforeEach
    void preparar() {
        estornoRepository.deleteAll();
        pedidoRepository.deleteAll();

        Produto produto = new Produto();
        produto.setSlug("camiseta-teste-" + System.nanoTime());
        produto.setNome("Camiseta Teste");
        produto.setPreco(BigDecimal.valueOf(100));

        ProdutoCor cor = new ProdutoCor();
        cor.setProduto(produto);
        cor.setNome("Preto");
        cor.setToken("preto");
        cor.setHex("#000000");
        produto.getCores().add(cor);

        ProdutoTamanho tamanho = new ProdutoTamanho();
        tamanho.setProduto(produto);
        tamanho.setLabel("M");
        produto.getTamanhos().add(tamanho);

        sku = new Sku();
        sku.setProduto(produto);
        sku.setCor(cor);
        sku.setTamanho(tamanho);
        sku.setCodigo("SKU-TESTE-" + System.nanoTime());
        sku.setQuantidade(10);
        produto.getSkus().add(sku);

        produtoRepository.save(produto);

        pedido = novoPedido();

        pedidoItem = new PedidoItem();
        pedidoItem.setPedido(pedido);
        pedidoItem.setSku(sku);
        pedidoItem.setNomeProduto("Camiseta Teste");
        pedidoItem.setCor("Preto");
        pedidoItem.setTamanho("M");
        pedidoItem.setPrecoUnitario(BigDecimal.valueOf(100));
        pedidoItem.setQuantidade(3);
        pedido.getItens().add(pedidoItem);

        pedidoRepository.save(pedido);
    }

    private Pedido novoPedido() {
        Pedido p = new Pedido();
        p.setStatus(StatusPedido.PAGO);
        p.setTotal(BigDecimal.valueOf(300));

        DadosNf dadosNf = new DadosNf();
        dadosNf.setNomeCliente("Cliente Teste");
        dadosNf.setCpfCnpj("12345678900");
        dadosNf.setEmail("cliente@teste.com");
        dadosNf.setTelefone("11999999999");
        p.setDadosNf(dadosNf);

        EnderecoSnapshot endereco = new EnderecoSnapshot();
        endereco.setLogradouro("Rua Teste");
        endereco.setNumero("100");
        endereco.setBairro("Bairro Teste");
        endereco.setCep("01000-000");
        endereco.setCidade("São Paulo");
        endereco.setUf("SP");
        p.setEndereco(endereco);

        return p;
    }

    private Estorno novoEstorno(int quantidade, OffsetDateTime criadoEm) {
        Estorno estorno = new Estorno();
        estorno.setPedido(pedido);
        estorno.setPedidoItem(pedidoItem);
        estorno.setSku(sku);
        estorno.setQuantidade(quantidade);
        estorno.setValor(BigDecimal.valueOf(100L * quantidade));
        estorno.setMpRefundId("refund-" + System.nanoTime());
        estorno.setCriadoEm(criadoEm);
        return estorno;
    }

    @Test
    void somaQuantidadeEstornadaPorItemSomaEstornosParciais() {
        estornoRepository.save(novoEstorno(1, OffsetDateTime.now().minusMinutes(10)));
        estornoRepository.save(novoEstorno(2, OffsetDateTime.now()));

        int soma = estornoRepository.somaQuantidadeEstornadaPorItem(pedidoItem.getId());

        assertThat(soma).isEqualTo(3);
    }

    @Test
    void somaQuantidadeEstornadaPorItemRetornaZeroQuandoNaoHaEstornos() {
        int soma = estornoRepository.somaQuantidadeEstornadaPorItem(pedidoItem.getId());

        assertThat(soma).isZero();
    }

    @Test
    void findByPedidoIdOrderByCriadoEmDescRetornaEstornosDoPedidoEmOrdemDecrescente() {
        Estorno antigo = estornoRepository.save(novoEstorno(1, OffsetDateTime.now().minusHours(2)));
        Estorno recente = estornoRepository.save(novoEstorno(1, OffsetDateTime.now()));

        List<Estorno> resultado = estornoRepository.findByPedidoIdOrderByCriadoEmDesc(pedido.getId());

        assertThat(resultado).extracting(Estorno::getId).containsExactly(recente.getId(), antigo.getId());
    }
}
