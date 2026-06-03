package com.magnossao.catalog;

import com.magnossao.catalog.dto.CorDto;
import com.magnossao.catalog.dto.ProdutoRequest;
import com.magnossao.catalog.dto.TamanhoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class AdminProdutoControllerIT {

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
    void criarProdutoRetorna201ComSlug() {
        String body = """
            {
              "slug": "admin-criar-it",
              "nome": "Produto Admin IT",
              "nomeCurto": "Admin",
              "colecao": "Col",
              "preco": 199.90,
              "descricao": "Desc",
              "descricaoSeo": "Desc SEO"
            }
            """;

        assertThat(mvc.post().uri("/api/admin/produtos")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .hasStatus(201)
            .bodyJson().extractingPath("$.slug").isEqualTo("admin-criar-it");
    }

    @Test
    void listarTodosRetorna200ComArray() {
        assertThat(mvc.get().uri("/api/admin/produtos"))
            .hasStatusOk()
            .bodyJson().extractingPath("$").asArray().isNotNull();
    }

    @Test
    void mudarStatusRetorna200ComStatusPublicado() {
        ProdutoRequest req = new ProdutoRequest(
            "admin-status-it", "Status IT", "Status", "Col", BigDecimal.valueOf(100),
            "Desc", "SEO");
        var criado = produtoService.criar(req);

        assertThat(mvc.patch().uri("/api/admin/produtos/{id}/status", criado.id())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLICADO\"}"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.status").isEqualTo("PUBLICADO");
    }

    @Test
    void gerarSkusComCorETamanhoRetornaArrayComUmSku() {
        ProdutoRequest req = new ProdutoRequest(
            "admin-skus-it", "Skus IT", "Skus", "Col", BigDecimal.valueOf(100),
            "Desc", "SEO");
        var criado = produtoService.criar(req);
        produtoService.adicionarCor(criado.id(), new CorDto(null, "Navy", "navy", "#001f3f"));
        produtoService.adicionarTamanho(criado.id(),
            new TamanhoDto(null, "M", 50, 70, 45));

        assertThat(mvc.post().uri("/api/admin/produtos/{id}/skus/gerar", criado.id()))
            .hasStatusOk()
            .bodyJson().extractingPath("$.length()").isEqualTo(1);
    }
}
