package com.magnossao.controller;

import com.magnossao.service.PagamentoService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class WebhookMercadoPagoControllerIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean PagamentoService pagamentoService;

    @Autowired WebApplicationContext wac;

    MockMvcTester mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcTester.from(wac, builder -> builder.apply(springSecurity()).build());
    }

    @Test
    void notificacaoSemAssinaturaValidaRetorna401() {
        when(pagamentoService.validarAssinatura(any(), any(), any())).thenReturn(false);

        assertThat(mvc.post().uri("/api/webhooks/mercadopago")
                .header("x-signature", "ts=123,v1=invalido")
                .header("x-request-id", "req-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"999\"}}"))
                .hasStatus(401);
    }

    @Test
    void notificacaoComAssinaturaValidaProcessaERetorna200() throws Exception {
        when(pagamentoService.validarAssinatura(any(), any(), any())).thenReturn(true);

        assertThat(mvc.post().uri("/api/webhooks/mercadopago")
                .header("x-signature", "ts=123,v1=valido")
                .header("x-request-id", "req-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"999\"}}"))
                .hasStatus(200);

        verify(pagamentoService).processarNotificacao("999");
    }
}
