package com.magnossao.controller;

import com.magnossao.dto.request.AtualizarStatusRequest;
import com.magnossao.dto.response.PedidoResponse;
import com.magnossao.dto.response.PedidoResumoResponse;
import com.magnossao.entity.StatusPedido;
import com.magnossao.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
public class AdminPedidoController {

    private final PedidoService pedidoService;

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
}
