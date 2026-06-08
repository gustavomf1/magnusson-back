package com.magnossao.service;

import com.magnossao.entity.*;
import com.magnossao.repository.PedidoRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;
import com.mercadopago.resources.preference.Preference;
import com.magnossao.exception.EstornoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock PreferenceClient preferenceClient;
    @Mock PaymentClient paymentClient;
    @Mock PaymentRefundClient paymentRefundClient;
    @Mock EstoqueService estoqueService;
    @Mock com.magnossao.repository.EstornoRepository estornoRepository;
    @Mock PedidoRepository pedidoRepository;
    @Mock Preference preferenceFake;
    @Mock CashbackService cashbackService;

    PagamentoService pagamentoService;

    @BeforeEach
    void setUp() {
        pagamentoService = new PagamentoService(preferenceClient, paymentClient, paymentRefundClient, estoqueService,
                estornoRepository, pedidoRepository, "http://localhost:8080", "http://localhost:3000",
                "dev-only-insecure-webhook-secret", 72, cashbackService);
    }

    private Pedido pedidoComUmItem() {
        Produto produto = new Produto();
        produto.setNome("Polo Classic");
        produto.setPreco(new BigDecimal("199.90"));

        Sku sku = new Sku();
        sku.setId(10L);
        sku.setProduto(produto);

        PedidoItem item = new PedidoItem();
        item.setId(1L);
        item.setSku(sku);
        item.setNomeProduto("Polo Classic");
        item.setPrecoUnitario(new BigDecimal("199.90"));
        item.setQuantidade(2);

        Pedido pedido = new Pedido();
        pedido.setId(42L);
        pedido.setTotal(new BigDecimal("399.80"));
        pedido.getItens().add(item);
        return pedido;
    }

    @Test
    void criaPreferenciaEGravaPreferenceIdNoPedido() throws Exception {
        Pedido pedido = pedidoComUmItem();

        when(preferenceFake.getId()).thenReturn("pref-123");
        when(preferenceFake.getInitPoint()).thenReturn("https://mp.example/checkout/pref-123");
        when(preferenceClient.create(org.mockito.ArgumentMatchers.any())).thenReturn(preferenceFake);

        String initPoint = pagamentoService.criarPreferencia(pedido);

        assertThat(initPoint).isEqualTo("https://mp.example/checkout/pref-123");
        assertThat(pedido.getMpPreferenceId()).isEqualTo("pref-123");
    }

    @Test
    void validaAssinaturaCorretamenteComHmacSha256() throws Exception {
        String secret = "dev-only-insecure-webhook-secret";
        String dataId = "123456";
        String requestId = "req-abc";
        String ts = "1700000000";
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        String hash = java.util.HexFormat.of().formatHex(mac.doFinal(manifest.getBytes()));

        String signatureHeader = "ts=" + ts + ",v1=" + hash;

        assertThat(pagamentoService.validarAssinatura(signatureHeader, requestId, dataId)).isTrue();
    }

    @Test
    void rejeitaAssinaturaComHashIncorreto() {
        assertThat(pagamentoService.validarAssinatura("ts=1700000000,v1=hash-invalido", "req-abc", "123456")).isFalse();
    }

    private Payment paymentFake(long id, String status, String externalReference) {
        Payment payment = org.mockito.Mockito.mock(Payment.class);
        org.mockito.Mockito.lenient().when(payment.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(payment.getStatus()).thenReturn(status);
        org.mockito.Mockito.lenient().when(payment.getExternalReference()).thenReturn(externalReference);
        return payment;
    }

    @Test
    void webhookAprovadoMudaPedidoParaPagoEGravaPaymentId() throws Exception {
        Pedido pedido = pedidoComUmItem();
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        when(pedidoRepository.findById(42L)).thenReturn(Optional.of(pedido));

        Payment payment = paymentFake(555L, "approved", "42");
        when(paymentClient.get(555L)).thenReturn(payment);

        pagamentoService.processarNotificacao("555");

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(pedido.getMpPaymentId()).isEqualTo("555");
        org.mockito.Mockito.verify(pedidoRepository).save(pedido);
    }

    @Test
    void webhookRejeitadoCancelaPedidoERestauraEstoque() throws Exception {
        Pedido pedido = pedidoComUmItem();
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        when(pedidoRepository.findById(42L)).thenReturn(Optional.of(pedido));

        Payment payment = paymentFake(556L, "rejected", "42");
        when(paymentClient.get(556L)).thenReturn(payment);

        pagamentoService.processarNotificacao("556");

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        org.mockito.Mockito.verify(estoqueService).restaurarEstoque(10L, 2);
    }

    @Test
    void webhookDuplicadoEhIdempotente() throws Exception {
        Pedido pedido = pedidoComUmItem();
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setMpPaymentId("555");
        when(pedidoRepository.findById(42L)).thenReturn(Optional.of(pedido));

        Payment payment = paymentFake(555L, "approved", "42");
        when(paymentClient.get(555L)).thenReturn(payment);

        pagamentoService.processarNotificacao("555");

        org.mockito.Mockito.verify(pedidoRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(estoqueService, org.mockito.Mockito.never()).restaurarEstoque(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void webhookDePedidoInexistenteNaoLancaErro() throws Exception {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        Payment payment = paymentFake(777L, "approved", "999");
        when(paymentClient.get(777L)).thenReturn(payment);

        org.assertj.core.api.Assertions.assertThatCode(() -> pagamentoService.processarNotificacao("777"))
            .doesNotThrowAnyException();
    }

    private PaymentRefund refundFake(long id) {
        PaymentRefund refund = org.mockito.Mockito.mock(PaymentRefund.class);
        org.mockito.Mockito.lenient().when(refund.getId()).thenReturn(id);
        return refund;
    }

    @Test
    void estornaItemParcialmenteEAtualizaStatusEEstoque() throws Exception {
        Pedido pedido = pedidoComUmItem();
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setMpPaymentId("555");
        PedidoItem item = pedido.getItens().get(0);

        when(estornoRepository.somaQuantidadeEstornadaPorItem(item.getId())).thenReturn(0);

        PaymentRefund refundFake = refundFake(9001L);
        when(paymentRefundClient.refund(eq(555L), any(BigDecimal.class))).thenReturn(refundFake);
        when(estornoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var resposta = pagamentoService.estornarItem(pedido, item.getId(), 1);

        assertThat(resposta.quantidade()).isEqualTo(1);
        assertThat(resposta.valor()).isEqualByComparingTo(new BigDecimal("199.90"));
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PARCIALMENTE_ESTORNADO);
        assertThat(pedido.getValorEstornado()).isEqualByComparingTo(new BigDecimal("199.90"));
        org.mockito.Mockito.verify(estoqueService).restaurarEstoque(10L, 1);
    }

    @Test
    void estornoQueAtinge100PorCentoMudaStatusParaEstornado() throws Exception {
        Pedido pedido = pedidoComUmItem();
        pedido.setStatus(StatusPedido.PARCIALMENTE_ESTORNADO);
        pedido.setMpPaymentId("555");
        pedido.setValorEstornado(new BigDecimal("199.90"));
        PedidoItem item = pedido.getItens().get(0);

        when(estornoRepository.somaQuantidadeEstornadaPorItem(item.getId())).thenReturn(1);

        PaymentRefund refundFake = refundFake(9002L);
        when(paymentRefundClient.refund(eq(555L), any(BigDecimal.class))).thenReturn(refundFake);
        when(estornoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        pagamentoService.estornarItem(pedido, item.getId(), 1);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ESTORNADO);
        assertThat(pedido.getValorEstornado()).isEqualByComparingTo(new BigDecimal("399.80"));
    }

    @Test
    void rejeitaEstornoDeQuantidadeMaiorQueADisponivel() {
        Pedido pedido = pedidoComUmItem();
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setMpPaymentId("555");
        PedidoItem item = pedido.getItens().get(0);

        when(estornoRepository.somaQuantidadeEstornadaPorItem(item.getId())).thenReturn(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> pagamentoService.estornarItem(pedido, item.getId(), 2))
            .isInstanceOf(EstornoInvalidoException.class);
    }

    @Test
    void rejeitaEstornoQuandoPedidoNaoTemMpPaymentId() {
        Pedido pedido = pedidoComUmItem();
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setMpPaymentId(null);
        PedidoItem item = pedido.getItens().get(0);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> pagamentoService.estornarItem(pedido, item.getId(), 1))
            .isInstanceOf(EstornoInvalidoException.class);
    }

    @Test
    void rejeitaEstornoQuandoPedidoTemMpPaymentIdInvalido() {
        Pedido pedido = pedidoComUmItem();
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setMpPaymentId("não-numérico");
        PedidoItem item = pedido.getItens().get(0);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> pagamentoService.estornarItem(pedido, item.getId(), 1))
            .isInstanceOf(EstornoInvalidoException.class);
    }

    @Test
    void rejeitaEstornoDePedidoQueNaoEstaPagoOuParcialmenteEstornado() {
        Pedido pedido = pedidoComUmItem();
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        PedidoItem item = pedido.getItens().get(0);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> pagamentoService.estornarItem(pedido, item.getId(), 1))
            .isInstanceOf(EstornoInvalidoException.class);
    }
}
