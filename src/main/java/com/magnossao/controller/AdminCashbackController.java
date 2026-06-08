package com.magnossao.controller;

import com.magnossao.dto.request.RegraCashbackRequest;
import com.magnossao.dto.response.RegraCashbackResponse;
import com.magnossao.service.CashbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cashback")
@RequiredArgsConstructor
public class AdminCashbackController {

    private final CashbackService cashbackService;

    @GetMapping("/regras")
    public List<RegraCashbackResponse> listarRegras() {
        return cashbackService.listarRegras();
    }

    @PutMapping("/regras/{produtoId}")
    public RegraCashbackResponse salvarRegra(@PathVariable Long produtoId, @RequestBody RegraCashbackRequest request) {
        return cashbackService.salvarRegra(produtoId, request);
    }

    @DeleteMapping("/regras/{produtoId}")
    public ResponseEntity<Void> removerRegra(@PathVariable Long produtoId) {
        cashbackService.removerRegra(produtoId);
        return ResponseEntity.noContent().build();
    }
}
