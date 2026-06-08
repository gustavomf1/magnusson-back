package com.magnossao.config;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PagamentoConfigTest {

    @Autowired PreferenceClient preferenceClient;
    @Autowired PaymentClient paymentClient;

    @Test
    void exposeOsClientesDoSdkComoBeans() {
        assertThat(preferenceClient).isNotNull();
        assertThat(paymentClient).isNotNull();
    }
}
