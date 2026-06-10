package com.magnossao.controller;

import com.magnossao.dto.response.CorDto;
import com.magnossao.dto.request.ProdutoRequest;
import com.magnossao.entity.Categoria;
import com.magnossao.dto.response.TamanhoDto;
import com.magnossao.service.ProdutoService;
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
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

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
        mvc = MockMvcTester.from(wac, builder -> builder.apply(springSecurity()).build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
    @WithMockUser(roles = "ADMIN")
    void listarRetorna200ComPagina() {
        assertThat(mvc.get().uri("/api/admin/produtos"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.conteudo").asArray().isNotNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarTrazCamposDePaginacao() {
        assertThat(mvc.get().uri("/api/admin/produtos?page=0&size=5"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.pagina").isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void filtrarPorStatusRascunhoNaoTrazPublicados() {
        produtoService.criar(new ProdutoRequest(
            "filtro-rascunho-it", "Filtro Rascunho IT", "FR", "Col",
            BigDecimal.valueOf(100), "Desc", "SEO", Categoria.POLO));
        assertThat(mvc.get().uri("/api/admin/produtos?status=RASCUNHO"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.conteudo").asArray().isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarPorNomeFiltraResultado() {
        produtoService.criar(new ProdutoRequest(
            "busca-zebra-it", "Produto Zebra Unico", "Zebra", "Col",
            BigDecimal.valueOf(100), "Desc", "SEO", Categoria.POLO));
        assertThat(mvc.get().uri("/api/admin/produtos?busca=zebra"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.conteudo[0].nome").asString().contains("Zebra");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void filtrarPorCategoriaShortsSemResultadosRetornaVazio() {
        assertThat(mvc.get().uri("/api/admin/produtos?categoria=SHORTS&busca=__inexistente__"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.conteudo").asArray().isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void mudarStatusRetorna200ComStatusPublicado() {
        ProdutoRequest req = new ProdutoRequest(
            "admin-status-it", "Status IT", "Status", "Col", BigDecimal.valueOf(100),
            "Desc", "SEO", Categoria.POLO);
        var criado = produtoService.criar(req);
        var cor = produtoService.adicionarCor(criado.id(),
            new CorDto(null, "Navy", "navy", "#001f3f", java.util.List.of()));
        produtoService.adicionarTamanho(criado.id(), new TamanhoDto(null, "M", 50, 70, 45));
        produtoService.confirmarImagem(criado.id(), cor.id(), "k1", "http://x/img.png", "img");
        assertThat(mvc.post().uri("/api/admin/produtos/{id}/skus/gerar", criado.id())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .hasStatusOk();

        assertThat(mvc.patch().uri("/api/admin/produtos/{id}/status", criado.id())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLICADO\"}"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.status").isEqualTo("PUBLICADO");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void gerarSkusComCorETamanhoRetornaArrayComUmSku() {
        ProdutoRequest req = new ProdutoRequest(
            "admin-skus-it", "Skus IT", "Skus", "Col", BigDecimal.valueOf(100),
            "Desc", "SEO", Categoria.POLO);
        var criado = produtoService.criar(req);
        produtoService.adicionarCor(criado.id(), new CorDto(null, "Navy", "navy", "#001f3f", java.util.List.of()));
        produtoService.adicionarTamanho(criado.id(),
            new TamanhoDto(null, "M", 50, 70, 45));

        assertThat(mvc.post().uri("/api/admin/produtos/{id}/skus/gerar", criado.id())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .hasStatusOk();
    }

    @Test
    void semAutenticacaoRetorna401() {
        assertThat(mvc.get().uri("/api/admin/produtos"))
                .hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void comRoleClientRetorna403() {
        assertThat(mvc.get().uri("/api/admin/produtos"))
                .hasStatus(403);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void confirmarImagemAninhaFotoNaCor() {
        ProdutoRequest req = new ProdutoRequest(
            "admin-foto-cor-it", "Foto Cor IT", "Foto", "Col", BigDecimal.valueOf(100),
            "Desc", "SEO", Categoria.POLO);
        var criado = produtoService.criar(req);
        var cor = produtoService.adicionarCor(criado.id(),
            new CorDto(null, "Navy", "navy", "#001f3f", java.util.List.of()));

        String body = """
            {"corId": %d, "chave": "k1", "url": "http://x/img.png", "alt": "img"}
            """.formatted(cor.id());

        assertThat(mvc.post().uri("/api/admin/produtos/{id}/imagens/confirmar", criado.id())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .hasStatusOk();

        assertThat(mvc.get().uri("/api/admin/produtos/{id}", criado.id()))
            .hasStatusOk()
            .bodyJson().extractingPath("$.cores[0].imagens[0].url").isEqualTo("http://x/img.png");
    }
}
