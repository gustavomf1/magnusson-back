package com.magnossao.service;

import com.magnossao.entity.Pedido;
import com.magnossao.entity.StatusPedido;
import com.magnossao.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class PedidoExpiracaoJob {

    private static final Logger log = LoggerFactory.getLogger(PedidoExpiracaoJob.class);

    private final PedidoRepository pedidoRepository;
    private final PagamentoService pagamentoService;

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void expirarPedidosAbandonados() {
        var candidatos = pedidoRepository.findByStatusAndCriadoEmBefore(
            StatusPedido.AGUARDANDO_PAGAMENTO, OffsetDateTime.now());

        for (Pedido pedido : candidatos) {
            if (!pagamentoService.expirou(pedido)) {
                continue;
            }
            try {
                if (pedido.getMpPaymentId() != null) {
                    pagamentoService.consultarEAtualizar(pedido.getId(), pedido.getMpPaymentId());
                } else {
                    pagamentoService.cancelarPorExpiracao(pedido);
                }
            } catch (Exception e) {
                log.warn("Falha ao processar expiração do pedido {}: {}", pedido.getId(), e.getMessage());
            }
        }
    }
}
