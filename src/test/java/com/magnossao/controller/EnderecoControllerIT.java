package com.magnossao.controller;

import com.magnossao.dto.request.CadastroRequest;
import com.magnossao.dto.request.EnderecoRequest;
import com.magnossao.entity.Usuario;
import com.magnossao.repository.UsuarioRepository;
import com.magnossao.service.EnderecoService;
import com.magnossao.service.StorageService;
import com.magnossao.service.UsuarioService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class EnderecoControllerIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean StorageService storageService;

    @Autowired WebApplicationContext wac;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired UsuarioService usuarioService;
    @Autowired EnderecoService enderecoService;

    MockMvcTester mvc;
    Long adminEnderecoId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcTester.from(wac, builder -> builder.apply(springSecurity()).build());

        // Garante que "outro@usuario.com" existe no banco
        if (usuarioRepository.findByEmail("outro@usuario.com").isEmpty()) {
            usuarioService.cadastrar(new CadastroRequest(
                    "Outro Usuario", "outro@usuario.com", "senha123",
                    "999.999.999-99", "(11) 99999-9999"));
        }

        // Cria um endereço como admin para usar nos testes
        Usuario admin = (Usuario) usuarioRepository.findByEmail("admin@magnossao.com.br").orElseThrow();
        var end = enderecoService.criar(admin, new EnderecoRequest(
                "Rua Admin", "100", null, "Centro", "01310100", "São Paulo", "SP", true));
        adminEnderecoId = end.id();
    }

    private static final String ENDERECO_JSON = """
        {
          "logradouro": "Rua das Flores",
          "numero": "42",
          "complemento": "Apto 3",
          "bairro": "Centro",
          "cep": "01310100",
          "cidade": "São Paulo",
          "uf": "SP",
          "principal": true
        }
        """;

    @Test
    @WithMockUser(username = "admin@magnossao.com.br", roles = "ADMIN")
    void criarEnderecoLogadoRetorna201() {
        assertThat(mvc.post().uri("/api/enderecos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ENDERECO_JSON))
                .hasStatus(201)
                .bodyJson().extractingPath("$.logradouro").isEqualTo("Rua das Flores");
    }

    @Test
    @WithMockUser(username = "admin@magnossao.com.br", roles = "ADMIN")
    void listarEnderecosSoRetornaDoUsuarioAutenticado() {
        assertThat(mvc.get().uri("/api/enderecos"))
                .hasStatusOk()
                .bodyJson().extractingPath("$").asArray().hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @WithMockUser(username = "outro@usuario.com", roles = "USER")
    void deletarEnderecoDeOutroUsuarioRetorna403() {
        assertThat(mvc.delete().uri("/api/enderecos/" + adminEnderecoId))
                .hasStatus(403);
    }
}
