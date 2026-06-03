package com.magnossao.catalog;

import com.magnossao.catalog.dto.ProdutoResponse;
import com.magnossao.catalog.dto.ProdutoResumoResponse;
import com.magnossao.catalog.model.StatusProduto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public List<ProdutoResumoResponse> listar() {
        return produtoService.listarPublicados();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProdutoResponse> buscar(@PathVariable String slug) {
        try {
            ProdutoResponse produto = produtoService.buscarPorSlug(slug);
            if (!produto.status().equals(StatusProduto.PUBLICADO.name())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(produto);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
