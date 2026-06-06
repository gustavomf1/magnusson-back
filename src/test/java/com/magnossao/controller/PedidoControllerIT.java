package com.magnossao.controller;

import com.magnossao.dto.request.ProdutoRequest;
import com.magnossao.dto.response.CorDto;
import com.magnossao.dto.response.TamanhoDto;
import com.magnossao.repository.ProdutoRepository;
import com.magnossao.service.EstoqueService;
import com.magnossao.service.ProdutoService;
import com.magnossao.service.SkuService;
import com.magnossao.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class PedidoControllerIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean StorageService storageService;

    private static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Autowired WebApplicationContext wac;
    @Autowired ProdutoService produtoService;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired SkuService skuService;
    @Autowired EstoqueService estoqueService;
    @Autowired TransactionTemplate txTemplate;

    MockMvcTester mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcTester.from(wac, builder -> builder.apply(springSecurity()).build());
    }

    private Long criarSkuComEstoque(int estoque) {
        String slug = "pedido-it-" + System.nanoTime();
        var req = new ProdutoRequest(slug, "Produto Pedido IT", null, null,
                BigDecimal.valueOf(200), null, null);
        var criado = produtoService.criar(req);
        produtoService.adicionarCor(criado.id(), new CorDto(null, "Azul", "azul", "#0000FF"));
        produtoService.adicionarTamanho(criado.id(), new TamanhoDto(null, "P", null, null, null));
        return txTemplate.execute(status -> {
            var produto = produtoRepository.findById(criado.id()).orElseThrow();
            var sku = skuService.gerarSkus(produto).getFirst();
            estoqueService.ajustarQuantidade(sku.getId(), estoque);
            return sku.getId();
        });
    }

    private String checkoutBody(Long skuId, int qtd) {
        return """
            {
              "itens": [{"skuId": %d, "quantidade": %d}],
              "dadosNf": {
                "nomeCliente": "João Teste",
                "cpfCnpj": "123.456.789-09",
                "email": "joao@teste.com",
                "telefone": "11999990000"
              },
              "endereco": {
                "logradouro": "Rua X",
                "numero": "1",
                "complemento": null,
                "bairro": "Centro",
                "cep": "01310100",
                "cidade": "São Paulo",
                "uf": "SP"
              }
            }
            """.formatted(skuId, qtd);
    }

    private static String uniqueIp() {
        return "10." + (System.nanoTime() % 250 + 1) + "." + (System.nanoTime() % 250 + 1) + ".1";
    }

    @Test
    void checkoutComEstoqueSuficienteRetorna201ComPedido() {
        Long skuId = criarSkuComEstoque(5);
        assertThat(mvc.post().uri("/api/pedidos")
                .header("X-Forwarded-For", uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody(skuId, 2)))
                .hasStatus(201)
                .bodyJson().extractingPath("$.status").isEqualTo("AGUARDANDO_PAGAMENTO");
    }

    @Test
    void checkoutComEstoqueInsuficienteRetorna409() {
        Long skuId = criarSkuComEstoque(1);
        assertThat(mvc.post().uri("/api/pedidos")
                .header("X-Forwarded-For", uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody(skuId, 5)))
                .hasStatus(409);
    }

    @Test
    void checkoutComSkuInativoRetorna400() {
        assertThat(mvc.post().uri("/api/pedidos")
                .header("X-Forwarded-For", uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody(99999L, 1)))
                .hasStatus(400);
    }

    @Test
    void buscarPedidoPorIdRetorna200() throws Exception {
        Long skuId = criarSkuComEstoque(5);
        var result = mvc.post().uri("/api/pedidos")
                .header("X-Forwarded-For", uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody(skuId, 1)).exchange();
        Long pedidoId = mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        assertThat(mvc.get().uri("/api/pedidos/" + pedidoId))
                .hasStatusOk()
                .bodyJson().extractingPath("$.id").isEqualTo(pedidoId.intValue());
    }

    @Test
    void adminListaPedidosSemAuthRetorna401() {
        assertThat(mvc.get().uri("/api/admin/pedidos")).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAvancaStatusRetorna200() throws Exception {
        Long skuId = criarSkuComEstoque(5);
        var result = mvc.post().uri("/api/pedidos")
                .header("X-Forwarded-For", uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody(skuId, 1)).exchange();
        Long pedidoId = mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        assertThat(mvc.patch().uri("/api/admin/pedidos/" + pedidoId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"PAGO\"}"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("PAGO");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAvancaParaStatusInvalidoRetorna400() throws Exception {
        Long skuId = criarSkuComEstoque(5);
        var result = mvc.post().uri("/api/pedidos")
                .header("X-Forwarded-For", uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody(skuId, 1)).exchange();
        Long pedidoId = mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        assertThat(mvc.patch().uri("/api/admin/pedidos/" + pedidoId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"INVALIDO\"}"))
                .hasStatus(400);
    }

    @Test
    void rateLimitingBloqueiaApos5Requisicoes() {
        Long skuId = criarSkuComEstoque(50);
        String ip = "10.0." + (System.nanoTime() % 200 + 1) + ".1";
        for (int i = 0; i < 5; i++) {
            mvc.post().uri("/api/pedidos")
                    .header("X-Forwarded-For", ip)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkoutBody(skuId, 1))
                    .exchange();
        }
        assertThat(mvc.post().uri("/api/pedidos")
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody(skuId, 1)))
                .hasStatus(429);
    }
}
