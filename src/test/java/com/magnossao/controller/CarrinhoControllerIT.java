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
class CarrinhoControllerIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean StorageService storageService;

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

    private Long criarSkuComEstoque() {
        String slug = "carrinho-it-" + System.nanoTime();
        var req = new ProdutoRequest(slug, "Produto Carrinho IT", null, null,
                BigDecimal.valueOf(150), null, null, Categoria.POLO);
        var criado = produtoService.criar(req);
        produtoService.adicionarCor(criado.id(), new CorDto(null, "Preto", "preto", "#000000", java.util.List.of()));
        produtoService.adicionarTamanho(criado.id(), new TamanhoDto(null, "G", null, null, null));
        return txTemplate.execute(status -> {
            var produto = produtoRepository.findById(criado.id()).orElseThrow();
            var sku = skuService.gerarSkus(produto).getFirst();
            estoqueService.ajustarQuantidade(sku.getId(), 10);
            return sku.getId();
        });
    }

    @Test
    void adicionarItemSemAuthRetorna401() {
        assertThat(mvc.post().uri("/api/carrinho")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuId\": 1, \"quantidade\": 1}"))
                .hasStatus(401);
    }

    @Test
    @WithMockUser(username = "admin@magnossao.com.br", roles = "ADMIN")
    void adicionarItemLogadoRetorna201() {
        Long skuId = criarSkuComEstoque();
        assertThat(mvc.post().uri("/api/carrinho")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuId\": " + skuId + ", \"quantidade\": 2}"))
                .hasStatus(201)
                .bodyJson().extractingPath("$.quantidade").isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "admin@magnossao.com.br", roles = "ADMIN")
    void adicionarMesmoSkuDuasVezesSomaQuantidades() {
        Long skuId = criarSkuComEstoque();
        String body = "{\"skuId\": " + skuId + ", \"quantidade\": 1}";
        mvc.post().uri("/api/carrinho").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
        assertThat(mvc.post().uri("/api/carrinho").contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(201)
                .bodyJson().extractingPath("$.quantidade").isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "admin@magnossao.com.br", roles = "ADMIN")
    void mergeComItensDoLocalStorageSomaAoCarrinhoExistente() {
        Long skuId = criarSkuComEstoque();
        mvc.post().uri("/api/carrinho")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuId\": " + skuId + ", \"quantidade\": 1}")
                .exchange();
        String mergeBody = "{\"itens\": [{\"skuId\": " + skuId + ", \"quantidade\": 3}]}";
        assertThat(mvc.post().uri("/api/carrinho/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mergeBody))
                .hasStatusOk()
                .bodyJson().extractingPath("$[0].quantidade").isEqualTo(4);
    }

    @Test
    @WithMockUser(username = "admin@magnossao.com.br", roles = "ADMIN")
    void removerItemInexistenteRetorna404() {
        assertThat(mvc.delete().uri("/api/carrinho/99999"))
                .hasStatus(404);
    }
}
