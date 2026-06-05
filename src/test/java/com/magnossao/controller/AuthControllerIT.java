package com.magnossao.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class AuthControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    WebApplicationContext wac;

    MockMvcTester mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcTester.from(wac, builder -> builder.apply(springSecurity()).build());
    }

    @Test
    void cadastroRetorna201ComDadosDoUsuario() {
        String body = """
                {
                  "nome": "João Silva",
                  "email": "joao.auth.it@exemplo.com",
                  "senha": "senha123",
                  "cpf": "123.456.789-00",
                  "telefone": "(11) 91234-5678"
                }
                """;
        assertThat(mvc.post().uri("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(201)
                .bodyJson().extractingPath("$.email").isEqualTo("joao.auth.it@exemplo.com");
    }

    @Test
    void cadastroComEmailDuplicadoRetorna409() {
        String body = """
                {
                  "nome": "Ana Costa",
                  "email": "admin@magnossao.com.br",
                  "senha": "senha123",
                  "cpf": "999.888.777-66",
                  "telefone": "(11) 90000-0001"
                }
                """;
        assertThat(mvc.post().uri("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(409);
    }

    @Test
    void loginAdminComSenhaCorretaRetorna200ComRoleAdmin() {
        String body = """
                {
                  "email": "admin@magnossao.com.br",
                  "senha": "admin123"
                }
                """;
        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(200)
                .bodyJson().extractingPath("$.role").isEqualTo("ADMIN");
    }

    @Test
    void loginComSenhaErradaRetorna401() {
        String body = """
                {
                  "email": "admin@magnossao.com.br",
                  "senha": "senha-errada"
                }
                """;
        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(401);
    }

    @Test
    void meSemCookieRetorna401() {
        assertThat(mvc.get().uri("/api/auth/me"))
                .hasStatus(401);
    }

    @Test
    @WithMockUser(username = "admin@magnossao.com.br", roles = "ADMIN")
    void logoutRetorna204() {
        assertThat(mvc.post().uri("/api/auth/logout"))
                .hasStatus(204);
    }
}
