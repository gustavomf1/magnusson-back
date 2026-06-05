package com.magnossao.controller;

import com.magnossao.dto.request.AjusteEstoqueRequest;
import com.magnossao.dto.response.SkuEstoqueResponse;
import com.magnossao.service.EstoqueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminEstoqueController {

    private final EstoqueService estoqueService;

    public AdminEstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping("/estoque")
    public List<SkuEstoqueResponse> listarEstoque() {
        return estoqueService.listarEstoque();
    }

    @PatchMapping("/skus/{id}/estoque")
    public SkuEstoqueResponse ajustarQuantidade(@PathVariable Long id,
                                                 @RequestBody AjusteEstoqueRequest req) {
        return estoqueService.ajustarQuantidade(id, req.quantidade());
    }

    @PostMapping("/skus/{id}/estoque/ajuste")
    public SkuEstoqueResponse ajustarRelativo(@PathVariable Long id,
                                               @RequestBody AjusteEstoqueRequest req) {
        return estoqueService.ajustarRelativo(id, req.quantidade());
    }
}
