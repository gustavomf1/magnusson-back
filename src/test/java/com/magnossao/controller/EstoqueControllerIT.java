package com.magnossao.controller;

import com.magnossao.dto.request.ProdutoRequest;
import com.magnossao.entity.Categoria;
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
class EstoqueControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    StorageService storageService;

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

    private Long criarSkuComProduto() {
        String slug = "estoque-it-" + System.nanoTime();
        ProdutoRequest req = new ProdutoRequest(slug, "Produto Estoque IT", null, null,
                BigDecimal.valueOf(100), null, null, Categoria.POLO);
        var criado = produtoService.criar(req);
        produtoService.adicionarCor(criado.id(),
                new CorDto(null, "Navy", "navy", "#1B3A5C", java.util.List.of()));
        produtoService.adicionarTamanho(criado.id(),
                new TamanhoDto(null, "M", null, null, null));
        return txTemplate.execute(status -> {
            var produto = produtoRepository.findById(criado.id()).orElseThrow();
            return skuService.gerarSkus(produto).getFirst().getId();
        });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarEstoqueRetorna200ComArray() {
        assertThat(mvc.get().uri("/api/admin/estoque"))
                .hasStatusOk()
                .bodyJson().extractingPath("$").asArray().isNotNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ajustarQuantidadeRetorna200ComNovaQuantidade() {
        Long skuId = criarSkuComProduto();

        assertThat(mvc.patch().uri("/api/admin/skus/{id}/estoque", skuId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\": 50}"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.quantidade").isEqualTo(50);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ajusteRelativoIncrementaQuantidade() {
        Long skuId = criarSkuComProduto();
        estoqueService.ajustarQuantidade(skuId, 10);

        assertThat(mvc.post().uri("/api/admin/skus/{id}/estoque/ajuste", skuId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\": 5}"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.quantidade").isEqualTo(15);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void quantidadeNegativaRetorna400() {
        Long skuId = criarSkuComProduto();

        assertThat(mvc.patch().uri("/api/admin/skus/{id}/estoque", skuId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\": -5}"))
                .hasStatus(400);
    }

    @Test
    void semAutenticacaoRetorna401() {
        assertThat(mvc.get().uri("/api/admin/estoque"))
                .hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void comRoleClientRetorna403() {
        assertThat(mvc.get().uri("/api/admin/estoque"))
                .hasStatus(403);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void skuInexistenteRetorna404() {
        assertThat(mvc.patch().uri("/api/admin/skus/99999/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\": 10}"))
                .hasStatus(404);
    }
}
