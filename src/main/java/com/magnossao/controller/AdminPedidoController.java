package com.magnossao.controller;

import com.magnossao.dto.request.AtualizarStatusRequest;
import com.magnossao.dto.request.EstornoRequest;
import com.magnossao.dto.response.EstornoResponse;
import com.magnossao.dto.response.PedidoResponse;
import com.magnossao.dto.response.PedidoResumoResponse;
import com.magnossao.entity.StatusPedido;
import com.magnossao.exception.PedidoNaoEncontradoException;
import com.magnossao.repository.PedidoRepository;
import com.magnossao.service.PagamentoService;
import com.magnossao.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
public class AdminPedidoController {

    private final PedidoService pedidoService;
    private final PagamentoService pagamentoService;
    private final PedidoRepository pedidoRepository;

    @GetMapping
    public Page<PedidoResumoResponse> listar(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataFim,
            @RequestParam(required = false) String cliente,
            Pageable pageable) {
        return pedidoService.listarAdmin(status, dataInicio, dataFim, cliente, pageable);
    }

    @GetMapping("/{id}")
    public PedidoResponse detalhe(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    @PatchMapping("/{id}/status")
    public PedidoResponse atualizarStatus(@PathVariable Long id,
                                           @RequestBody AtualizarStatusRequest req) {
        return pedidoService.atualizarStatus(id, req.status());
    }

    @PostMapping("/{id}/estorno")
    public ResponseEntity<EstornoResponse> estornar(@PathVariable Long id,
                                                      @RequestBody EstornoRequest req) throws Exception {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new PedidoNaoEncontradoException(id));
        var resposta = pagamentoService.estornarItem(pedido, req.pedidoItemId(), req.quantidade());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}
