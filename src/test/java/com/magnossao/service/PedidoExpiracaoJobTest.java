package com.magnossao.service;

import com.magnossao.entity.*;
import com.magnossao.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoExpiracaoJobTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock PagamentoService pagamentoService;
    @InjectMocks PedidoExpiracaoJob job;

    private Pedido pedidoExpirado(boolean comPaymentId) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setTotal(BigDecimal.TEN);
        pedido.setCriadoEm(OffsetDateTime.now().minusHours(80));
        if (comPaymentId) pedido.setMpPaymentId("999");
        return pedido;
    }

    @Test
    void cancelaPedidoExpiradoSemPagamentoConfirmado() throws Exception {
        Pedido pedido = pedidoExpirado(false);
        when(pedidoRepository.findByStatusAndCriadoEmBefore(eq(StatusPedido.AGUARDANDO_PAGAMENTO), any()))
            .thenReturn(List.of(pedido));
        when(pagamentoService.expirou(pedido)).thenReturn(true);

        job.expirarPedidosAbandonados();

        verify(pagamentoService).cancelarPorExpiracao(pedido);
    }

    @Test
    void atualizaParaPagoSeMpJaAprovouMasWebhookSePerdeu() throws Exception {
        Pedido pedido = pedidoExpirado(true);
        when(pedidoRepository.findByStatusAndCriadoEmBefore(eq(StatusPedido.AGUARDANDO_PAGAMENTO), any()))
            .thenReturn(List.of(pedido));
        when(pagamentoService.expirou(pedido)).thenReturn(true);

        job.expirarPedidosAbandonados();

        verify(pagamentoService).consultarEAtualizar(pedido.getId(), "999");
        verify(pagamentoService, never()).cancelarPorExpiracao(any());
    }
}
