package com.magnossao.catalog;

import com.magnossao.catalog.dto.ProdutoRequest;
import com.magnossao.catalog.model.StatusProduto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class ProdutoControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    StorageService storageService;

    @Autowired
    WebApplicationContext wac;

    @Autowired
    ProdutoService produtoService;

    MockMvcTester mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcTester.from(wac);
    }

    @Test
    void listarProdutosPublicadosRetornaOk() {
        MvcTestResult result = mvc.get().uri("/api/produtos").exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().isLenientlyEqualTo("[]");
    }

    @Test
    void buscarProdutoPorSlugInexistenteRetorna404() {
        assertThat(mvc.get().uri("/api/produtos/nao-existe"))
            .hasStatus(404);
    }

    @Test
    void buscarProdutoPublicadoPorSlugRetornaOk() {
        ProdutoRequest req = new ProdutoRequest(
            "test-slug-it", "Produto Teste IT", "Teste", "Col", BigDecimal.valueOf(100),
            "Desc", "Desc SEO");
        var criado = produtoService.criar(req);
        produtoService.mudarStatus(criado.id(), StatusProduto.PUBLICADO.name());

        assertThat(mvc.get().uri("/api/produtos/test-slug-it"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.slug").isEqualTo("test-slug-it");
    }

    @Test
    void buscarProdutoRascunhoPorSlugRetorna404() {
        ProdutoRequest req = new ProdutoRequest(
            "rascunho-slug-it", "Rascunho IT", "Rascunho", "Col", BigDecimal.valueOf(50),
            "Desc", "SEO");
        produtoService.criar(req);

        assertThat(mvc.get().uri("/api/produtos/rascunho-slug-it"))
            .hasStatus(404);
    }
}
