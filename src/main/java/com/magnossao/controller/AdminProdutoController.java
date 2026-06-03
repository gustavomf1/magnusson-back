package com.magnossao.controller;

import com.magnossao.dto.response.*;
import com.magnossao.dto.request.*;
import com.magnossao.repository.ProdutoRepository;
import com.magnossao.service.ProdutoService;
import com.magnossao.service.SkuService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/produtos")
public class AdminProdutoController {

    private final ProdutoService produtoService;
    private final SkuService skuService;
    private final ProdutoRepository produtoRepository;

    public AdminProdutoController(ProdutoService produtoService, SkuService skuService,
                                  ProdutoRepository produtoRepository) {
        this.produtoService = produtoService;
        this.skuService = skuService;
        this.produtoRepository = produtoRepository;
    }

    @GetMapping
    public List<ProdutoResumoResponse> listarTodos() {
        return produtoService.listarTodos();
    }

    @GetMapping("/{id}")
    public ProdutoResponse buscarPorId(@PathVariable Long id) {
        return produtoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(@RequestBody ProdutoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(req));
    }

    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable Long id, @RequestBody ProdutoRequest req) {
        return produtoService.atualizar(id, req);
    }

    @PatchMapping("/{id}/status")
    public ProdutoResponse mudarStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        return produtoService.mudarStatus(id, req.status());
    }

    @PostMapping("/{id}/imagens/upload-url")
    public PresignedUploadResponse gerarUrlUpload(@PathVariable Long id,
                                                   @RequestParam String contentType) {
        return produtoService.gerarUrlUpload(id, contentType);
    }

    @PostMapping("/{id}/imagens/confirmar")
    public ResponseEntity<Void> confirmarImagem(@PathVariable Long id,
                                                 @RequestBody ImagemConfirmacaoRequest req) {
        produtoService.confirmarImagem(id, req.chave(), req.url(), req.alt());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/imagens/{imagemId}")
    public ResponseEntity<Void> deletarImagem(@PathVariable Long id, @PathVariable Long imagemId) {
        produtoService.deletarImagem(id, imagemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/imagens/ordem")
    public ResponseEntity<Void> reordenarImagens(@PathVariable Long id,
                                                   @RequestBody OrdemImagemRequest req) {
        produtoService.reordenarImagens(id, req.ids());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cores")
    public ResponseEntity<CorDto> adicionarCor(@PathVariable Long id, @RequestBody CorDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.adicionarCor(id, req));
    }

    @DeleteMapping("/{id}/cores/{corId}")
    public ResponseEntity<Void> deletarCor(@PathVariable Long id, @PathVariable Long corId) {
        produtoService.deletarCor(id, corId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tamanhos")
    public ResponseEntity<TamanhoDto> adicionarTamanho(@PathVariable Long id,
                                                         @RequestBody TamanhoDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.adicionarTamanho(id, req));
    }

    @DeleteMapping("/{id}/tamanhos/{tamanhoId}")
    public ResponseEntity<Void> deletarTamanho(@PathVariable Long id,
                                                 @PathVariable Long tamanhoId) {
        produtoService.deletarTamanho(id, tamanhoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/skus/gerar")
    @Transactional
    public List<SkuDto> gerarSkus(@PathVariable Long id) {
        var produto = produtoRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));
        return skuService.gerarSkus(produto).stream()
            .map(s -> new SkuDto(s.getId(), s.getCor().getId(), s.getTamanho().getId(),
                                  s.getCodigo(), s.isAtivo()))
            .toList();
    }

    @PostMapping("/{id}/beneficios")
    public ResponseEntity<BeneficioDto> adicionarBeneficio(@PathVariable Long id,
                                                             @RequestBody BeneficioDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.adicionarBeneficio(id, req));
    }

    @DeleteMapping("/beneficios/{beneficioId}")
    public ResponseEntity<Void> deletarBeneficio(@PathVariable Long beneficioId) {
        produtoService.deletarBeneficio(beneficioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewDto> adicionarReview(@PathVariable Long id,
                                                       @RequestBody ReviewDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.adicionarReview(id, req));
    }

    @PostMapping("/{id}/faqs")
    public ResponseEntity<FaqDto> adicionarFaq(@PathVariable Long id, @RequestBody FaqDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.adicionarFaq(id, req));
    }
}
