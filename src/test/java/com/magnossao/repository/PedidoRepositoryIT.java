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
class PedidoRepositoryIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired PedidoRepository pedidoRepository;

    @BeforeEach
    void limpar() {
        pedidoRepository.deleteAll();
    }

    private Pedido novoPedido(StatusPedido status, OffsetDateTime criadoEm) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setTotal(BigDecimal.TEN);
        pedido.setCriadoEm(criadoEm);

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

        return pedido;
    }

    @Test
    void encontraPedidosAguardandoPagamentoCriadosAntesDeUmInstante() {
        Pedido antigo = novoPedido(StatusPedido.AGUARDANDO_PAGAMENTO, OffsetDateTime.now().minusHours(100));
        pedidoRepository.save(antigo);

        Pedido recente = novoPedido(StatusPedido.AGUARDANDO_PAGAMENTO, OffsetDateTime.now().minusHours(1));
        pedidoRepository.save(recente);

        List<Pedido> resultado = pedidoRepository.findByStatusAndCriadoEmBefore(
            StatusPedido.AGUARDANDO_PAGAMENTO, OffsetDateTime.now().minusHours(72));

        assertThat(resultado).extracting(Pedido::getId).containsExactly(antigo.getId());
    }
}
