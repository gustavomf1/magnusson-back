package com.magnossao.service;

import com.magnossao.dto.response.EstornoResponse;
import com.magnossao.entity.Estorno;
import com.magnossao.entity.Pedido;
import com.magnossao.entity.PedidoItem;
import com.magnossao.entity.StatusPedido;
import com.magnossao.exception.EstornoInvalidoException;
import com.magnossao.repository.EstornoRepository;
import com.magnossao.repository.PedidoRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;
import com.mercadopago.resources.preference.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PreferenceClient preferenceClient;
    private final PaymentClient paymentClient;
    private final PaymentRefundClient paymentRefundClient;
    private final EstoqueService estoqueService;
    private final EstornoRepository estornoRepository;
    private final PedidoRepository pedidoRepository;
    private final String baseUrl;
    private final String frontendUrl;
    private final String webhookSecret;
    private final int expiracaoHoras;

    public PagamentoService(PreferenceClient preferenceClient, PaymentClient paymentClient,
                            PaymentRefundClient paymentRefundClient, EstoqueService estoqueService,
                            EstornoRepository estornoRepository, PedidoRepository pedidoRepository,
                            @Value("${app.base-url}") String baseUrl,
                            @Value("${app.cors.origin}") String frontendUrl,
                            @Value("${mercadopago.webhook-secret}") String webhookSecret,
                            @Value("${mercadopago.expiracao-horas}") int expiracaoHoras) {
        this.preferenceClient = preferenceClient;
        this.paymentClient = paymentClient;
        this.paymentRefundClient = paymentRefundClient;
        this.estoqueService = estoqueService;
        this.estornoRepository = estornoRepository;
        this.pedidoRepository = pedidoRepository;
        this.baseUrl = baseUrl;
        this.frontendUrl = frontendUrl;
        this.webhookSecret = webhookSecret;
        this.expiracaoHoras = expiracaoHoras;
    }

    public String criarPreferencia(Pedido pedido) throws com.mercadopago.exceptions.MPException, com.mercadopago.exceptions.MPApiException {
        List<PreferenceItemRequest> itens = pedido.getItens().stream()
            .map(this::toPreferenceItem)
            .toList();

        String urlPedido = frontendUrl + "/pedidos/" + pedido.getId();
        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
            .success(urlPedido)
            .pending(urlPedido)
            .failure(urlPedido)
            .build();

        PreferenceRequest request = PreferenceRequest.builder()
            .items(itens)
            .externalReference(pedido.getId().toString())
            .backUrls(backUrls)
            .autoReturn("approved")
            .notificationUrl(baseUrl + "/api/webhooks/mercadopago")
            .build();

        Preference preference = preferenceClient.create(request);
        pedido.setMpPreferenceId(preference.getId());
        return preference.getInitPoint();
    }

    private PreferenceItemRequest toPreferenceItem(PedidoItem item) {
        return PreferenceItemRequest.builder()
            .title(item.getNomeProduto() + " - " + item.getCor() + "/" + item.getTamanho())
            .quantity(item.getQuantidade())
            .unitPrice(item.getPrecoUnitario())
            .currencyId("BRL")
            .build();
    }

    public boolean validarAssinatura(String signatureHeader, String requestId, String dataId) {
        if (signatureHeader == null || requestId == null || dataId == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String parte : signatureHeader.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].trim().equals("ts")) ts = kv[1].trim();
            if (kv[0].trim().equals("v1")) v1 = kv[1].trim();
        }
        if (ts == null || v1 == null) {
            return false;
        }
        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + requestId + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String hashCalculado = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
            return hashCalculado.equalsIgnoreCase(v1);
        } catch (Exception e) {
            return false;
        }
    }

    public void processarNotificacao(String paymentIdStr) throws Exception {
        Long paymentId;
        try {
            paymentId = Long.valueOf(paymentIdStr);
        } catch (NumberFormatException e) {
            log.warn("Notificação de pagamento recebida com ID inválido: '{}'", paymentIdStr);
            return;
        }

        Payment payment = paymentClient.get(paymentId);
        log.info("Notificação de pagamento {} recebida com status '{}'", paymentId, payment.getStatus());

        Long pedidoId;
        try {
            pedidoId = Long.valueOf(payment.getExternalReference());
        } catch (NumberFormatException | NullPointerException e) {
            log.warn("Pagamento {} possui externalReference inválida: '{}'", paymentId, payment.getExternalReference());
            return;
        }

        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null) {
            log.warn("Pagamento {} referencia pedido {} que não foi encontrado", paymentId, pedidoId);
            return;
        }

        aplicarStatusDoPagamento(pedido, payment);
    }

    public void consultarEAtualizar(Long pedidoId, String mpPaymentId) throws Exception {
        Long paymentId;
        try {
            paymentId = Long.valueOf(mpPaymentId);
        } catch (NumberFormatException e) {
            log.warn("Pedido {} possui mpPaymentId inválido: '{}'", pedidoId, mpPaymentId);
            return;
        }

        Payment payment = paymentClient.get(paymentId);
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null) {
            return;
        }
        aplicarStatusDoPagamento(pedido, payment);
    }

    public boolean expirou(Pedido pedido) {
        return pedido.getCriadoEm().isBefore(OffsetDateTime.now().minusHours(expiracaoHoras));
    }

    public void cancelarPorExpiracao(Pedido pedido) {
        log.info("Pedido {} cancelado automaticamente por expiração ({}h sem confirmação de pagamento)",
            pedido.getId(), expiracaoHoras);
        pedido.setStatus(StatusPedido.CANCELADO);
        for (PedidoItem item : pedido.getItens()) {
            estoqueService.restaurarEstoque(item.getSku().getId(), item.getQuantidade());
        }
        pedidoRepository.save(pedido);
    }

    public EstornoResponse estornarItem(Pedido pedido, Long pedidoItemId, int quantidade) throws Exception {
        if (pedido.getStatus() != StatusPedido.PAGO && pedido.getStatus() != StatusPedido.PARCIALMENTE_ESTORNADO) {
            throw new EstornoInvalidoException("Pedido não está em status PAGO ou PARCIALMENTE_ESTORNADO");
        }

        PedidoItem item = pedido.getItens().stream()
            .filter(i -> i.getId().equals(pedidoItemId))
            .findFirst()
            .orElseThrow(() -> new EstornoInvalidoException("Item não pertence ao pedido: " + pedidoItemId));

        int jaEstornado = estornoRepository.somaQuantidadeEstornadaPorItem(pedidoItemId);
        if (quantidade <= 0 || quantidade > item.getQuantidade() - jaEstornado) {
            throw new EstornoInvalidoException("Quantidade a estornar excede o disponível para o item: " + pedidoItemId);
        }

        BigDecimal valor = item.getPrecoUnitario().multiply(BigDecimal.valueOf(quantidade));

        PaymentRefund refund = paymentRefundClient.refund(Long.valueOf(pedido.getMpPaymentId()), valor);

        Estorno estorno = new Estorno();
        estorno.setPedido(pedido);
        estorno.setPedidoItem(item);
        estorno.setSku(item.getSku());
        estorno.setQuantidade(quantidade);
        estorno.setValor(valor);
        estorno.setMpRefundId(refund.getId().toString());
        estornoRepository.save(estorno);

        estoqueService.restaurarEstoque(item.getSku().getId(), quantidade);

        pedido.setValorEstornado(pedido.getValorEstornado().add(valor));
        pedido.setStatus(pedido.getValorEstornado().compareTo(pedido.getTotal()) >= 0
            ? StatusPedido.ESTORNADO
            : StatusPedido.PARCIALMENTE_ESTORNADO);
        pedidoRepository.save(pedido);

        return EstornoResponse.from(estorno);
    }

    private void aplicarStatusDoPagamento(Pedido pedido, Payment payment) {
        String paymentIdStr = payment.getId().toString();
        String statusMp = payment.getStatus();

        boolean jaProcessado = paymentIdStr.equals(pedido.getMpPaymentId())
            && reflete(pedido.getStatus(), statusMp);
        if (jaProcessado) {
            log.info("Notificação de pagamento {} para o pedido {} já foi processada — ignorando",
                paymentIdStr, pedido.getId());
            return;
        }

        pedido.setMpPaymentId(paymentIdStr);
        switch (statusMp) {
            case "approved" -> pedido.setStatus(StatusPedido.PAGO);
            case "rejected", "cancelled" -> {
                pedido.setStatus(StatusPedido.CANCELADO);
                for (PedidoItem item : pedido.getItens()) {
                    estoqueService.restaurarEstoque(item.getSku().getId(), item.getQuantidade());
                }
            }
            default -> { /* pending/in_process: mantém AGUARDANDO_PAGAMENTO, só grava o paymentId */ }
        }
        log.info("Pedido {} alterado para status {} via notificação de pagamento {}",
            pedido.getId(), pedido.getStatus(), paymentIdStr);
        pedidoRepository.save(pedido);
    }

    private boolean reflete(StatusPedido status, String statusMp) {
        return switch (statusMp) {
            case "approved" -> status == StatusPedido.PAGO;
            case "rejected", "cancelled" -> status == StatusPedido.CANCELADO;
            default -> status == StatusPedido.AGUARDANDO_PAGAMENTO;
        };
    }
}
