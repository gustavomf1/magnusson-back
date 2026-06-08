package com.magnossao.service;

import com.magnossao.entity.Pedido;
import com.magnossao.entity.PedidoItem;
import com.magnossao.repository.EstornoRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

@Service
public class PagamentoService {

    private final PreferenceClient preferenceClient;
    private final PaymentClient paymentClient;
    private final PaymentRefundClient paymentRefundClient;
    private final EstoqueService estoqueService;
    private final EstornoRepository estornoRepository;
    private final String baseUrl;
    private final String frontendUrl;
    private final String webhookSecret;
    private final int expiracaoHoras;

    public PagamentoService(PreferenceClient preferenceClient, PaymentClient paymentClient,
                            PaymentRefundClient paymentRefundClient, EstoqueService estoqueService,
                            EstornoRepository estornoRepository,
                            @Value("${app.base-url}") String baseUrl,
                            @Value("${app.cors.origin}") String frontendUrl,
                            @Value("${mercadopago.webhook-secret}") String webhookSecret,
                            @Value("${mercadopago.expiracao-horas}") int expiracaoHoras) {
        this.preferenceClient = preferenceClient;
        this.paymentClient = paymentClient;
        this.paymentRefundClient = paymentRefundClient;
        this.estoqueService = estoqueService;
        this.estornoRepository = estornoRepository;
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

    /**
     * Stub temporário: a implementação real (consulta ao MP, atualização de status do pedido,
     * baixa de estoque, etc.) será feita na Task 6. Por ora apenas permite que o
     * WebhookMercadoPagoController compile e seja testável via @MockitoBean.
     */
    public void processarNotificacao(String paymentIdStr) throws Exception {
        throw new UnsupportedOperationException("processarNotificacao será implementado na Task 6");
    }
}
