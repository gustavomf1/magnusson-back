package com.magnossao.controller;

import com.mercadopago.client.payment.PaymentRefundClient;
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
class EstornoControllerIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean PaymentRefundClient paymentRefundClient;

    @Autowired WebApplicationContext wac;

    MockMvcTester mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcTester.from(wac, builder -> builder.apply(springSecurity()).build());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void semRoleAdminRetorna403() {
        assertThat(mvc.post().uri("/api/admin/pedidos/1/estorno")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoItemId\":1,\"quantidade\":1}"))
                .hasStatus(403);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void estornarPedidoInexistenteRetorna404() {
        assertThat(mvc.post().uri("/api/admin/pedidos/999999/estorno")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoItemId\":1,\"quantidade\":1}"))
                .hasStatus(404);
    }
}
