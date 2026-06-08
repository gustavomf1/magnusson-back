package com.magnossao.controller;

import com.magnossao.service.PagamentoService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/mercadopago")
@RequiredArgsConstructor
public class WebhookMercadoPagoController {

    private final PagamentoService pagamentoService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> receber(@RequestHeader("x-signature") String signature,
                                         @RequestHeader("x-request-id") String requestId,
                                         @RequestBody String payload) throws Exception {
        JsonNode body = objectMapper.readTree(payload);
        String dataId = body.path("data").path("id").asText(null);

        if (!pagamentoService.validarAssinatura(signature, requestId, dataId)) {
            return ResponseEntity.status(401).build();
        }

        if ("payment".equals(body.path("type").asText(null)) && dataId != null) {
            pagamentoService.processarNotificacao(dataId);
        }

        return ResponseEntity.ok().build();
    }
}
