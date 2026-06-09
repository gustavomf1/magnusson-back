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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class CupomRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private CupomRepository cupomRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ProdutoRepository produtoRepository;

    private Usuario usuario;
    private PedidoItem itemOrigem;

    @BeforeEach
    void setUp() {
        cupomRepository.deleteAll();
        pedidoRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = new Usuario();
        usuario.setNome("Cliente Teste");
        usuario.setEmail("cliente-cupom-" + System.nanoTime() + "@teste.com");
        usuario.setSenhaHash("hash");
        usuario.setCpf(String.valueOf(System.nanoTime()).substring(0, 11));
        usuario.setTelefone("11999999999");
        usuario.setRole(Role.CLIENT);
        usuario = usuarioRepository.save(usuario);

        Produto produto = new Produto();
        produto.setSlug("camiseta-cupom-teste-" + System.nanoTime());
        produto.setNome("Camiseta Teste");
        produto.setPreco(BigDecimal.valueOf(100));

        ProdutoCor cor = new ProdutoCor();
        cor.setProduto(produto);
        cor.setNome("Azul");
        cor.setToken("azul");
        cor.setHex("#0000ff");
        produto.getCores().add(cor);

        ProdutoTamanho tamanho = new ProdutoTamanho();
        tamanho.setProduto(produto);
        tamanho.setLabel("M");
        produto.getTamanhos().add(tamanho);

        Sku sku = new Sku();
        sku.setProduto(produto);
        sku.setCor(cor);
        sku.setTamanho(tamanho);
        sku.setCodigo("SKU-CUPOM-TESTE-" + System.nanoTime());
        sku.setQuantidade(10);
        produto.getSkus().add(sku);

        produtoRepository.save(produto);

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setTotal(BigDecimal.valueOf(100));

        DadosNf dadosNf = new DadosNf();
        dadosNf.setNomeCliente("Cliente Teste");
        dadosNf.setCpfCnpj("12345678900");
        dadosNf.setEmail("cliente@teste.com");
        dadosNf.setTelefone("11999999999");
        pedido.setDadosNf(dadosNf);

        EnderecoSnapshot endereco = new EnderecoSnapshot();
        endereco.setLogradouro("Rua Teste");
        endereco.setNumero("100");
        endereco.setBairro("Bairro Teste");
        endereco.setCep("01000-000");
        endereco.setCidade("São Paulo");
        endereco.setUf("SP");
        pedido.setEndereco(endereco);

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setSku(sku);
        item.setNomeProduto("Produto Teste");
        item.setCor("Azul");
        item.setTamanho("M");
        item.setPrecoUnitario(BigDecimal.valueOf(100));
        item.setQuantidade(1);
        pedido.getItens().add(item);

        pedido = pedidoRepository.save(pedido);
        itemOrigem = pedido.getItens().get(0);
    }

    private Cupom novoCupom(StatusCupom status, LocalDateTime expiraEm) {
        Cupom cupom = new Cupom();
        cupom.setUsuario(usuario);
        cupom.setValor(BigDecimal.TEN);
        cupom.setPedidoItemOrigem(itemOrigem);
        cupom.setStatus(status);
        cupom.setExpiraEm(expiraEm);
        return cupomRepository.save(cupom);
    }

    @Test
    void naoEncontraCupomAtivoComExpiracaoNoFuturo() {
        novoCupom(StatusCupom.ATIVO, LocalDateTime.now().plusDays(10));

        List<Cupom> vencidos = cupomRepository.findByStatusAndExpiraEmBefore(StatusCupom.ATIVO, LocalDateTime.now());

        assertThat(vencidos).isEmpty();
    }

    @Test
    void naoEncontraCupomAtivoSemDataDeExpiracao() {
        novoCupom(StatusCupom.ATIVO, null);

        List<Cupom> vencidos = cupomRepository.findByStatusAndExpiraEmBefore(StatusCupom.ATIVO, LocalDateTime.now());

        assertThat(vencidos).isEmpty();
    }

    @Test
    void encontraCupomAtivoComExpiracaoNoPassado() {
        Cupom esperado = novoCupom(StatusCupom.ATIVO, LocalDateTime.now().minusDays(1));

        List<Cupom> vencidos = cupomRepository.findByStatusAndExpiraEmBefore(StatusCupom.ATIVO, LocalDateTime.now());

        assertThat(vencidos).extracting(Cupom::getId).containsExactly(esperado.getId());
    }
}
