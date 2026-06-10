package com.magnossao.controller;

import com.magnossao.dto.request.ProdutoRequest;
import com.magnossao.dto.response.CorDto;
import com.magnossao.dto.response.TamanhoDto;
import com.magnossao.entity.Categoria;
import com.magnossao.entity.StatusProduto;
import com.magnossao.service.ProdutoService;
import com.magnossao.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

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

    @Autowired
    JdbcTemplate jdbc;

    MockMvcTester mvc;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE TABLE produto CASCADE");
        mvc = MockMvcTester.from(wac, builder -> builder.apply(springSecurity()).build());
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
    @WithMockUser(roles = "ADMIN")
    void buscarProdutoPublicadoPorSlugRetornaOk() {
        ProdutoRequest req = new ProdutoRequest(
            "test-slug-it", "Produto Teste IT", "Teste", "Col", BigDecimal.valueOf(100),
            "Desc", "Desc SEO", Categoria.POLO);
        var criado = produtoService.criar(req);
        // Satisfaz os requisitos de publicação (cor + tamanho + foto + SKUs).
        var cor = produtoService.adicionarCor(criado.id(),
            new CorDto(null, "Navy", "navy", "#001f3f", List.of()));
        produtoService.adicionarTamanho(criado.id(), new TamanhoDto(null, "M", 50, 70, 45));
        produtoService.confirmarImagem(criado.id(), cor.id(), "k1", "http://x/img.png", "img");
        assertThat(mvc.post().uri("/api/admin/produtos/{id}/skus/gerar", criado.id())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .hasStatusOk();
        produtoService.mudarStatus(criado.id(), StatusProduto.PUBLICADO.name());

        assertThat(mvc.get().uri("/api/produtos/test-slug-it"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.slug").isEqualTo("test-slug-it");
    }

    @Test
    void buscarProdutoRascunhoPorSlugRetorna404() {
        ProdutoRequest req = new ProdutoRequest(
            "rascunho-slug-it", "Rascunho IT", "Rascunho", "Col", BigDecimal.valueOf(50),
            "Desc", "SEO", Categoria.CAMISA);
        produtoService.criar(req);

        assertThat(mvc.get().uri("/api/produtos/rascunho-slug-it"))
            .hasStatus(404);
    }
}
