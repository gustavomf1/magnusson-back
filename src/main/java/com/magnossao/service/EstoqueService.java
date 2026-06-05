package com.magnossao.service;

import com.magnossao.dto.response.SkuEstoqueResponse;
import com.magnossao.entity.Sku;
import com.magnossao.exception.EstoqueInsuficienteException;
import com.magnossao.repository.SkuRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class EstoqueService {

    private final SkuRepository skuRepository;

    public EstoqueService(SkuRepository skuRepository) {
        this.skuRepository = skuRepository;
    }

    public List<SkuEstoqueResponse> listarEstoque() {
        return skuRepository.findAllComRelacoes().stream()
                .map(this::toResponse)
                .toList();
    }

    public SkuEstoqueResponse ajustarQuantidade(Long skuId, int novaQuantidade) {
        if (novaQuantidade < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }
        Sku sku = buscarSku(skuId);
        sku.setQuantidade(novaQuantidade);
        return toResponse(skuRepository.save(sku));
    }

    public SkuEstoqueResponse ajustarRelativo(Long skuId, int delta) {
        Sku sku = buscarSku(skuId);
        int nova = sku.getQuantidade() + delta;
        if (nova < 0) {
            throw new IllegalArgumentException("Estoque não pode ficar negativo");
        }
        sku.setQuantidade(nova);
        return toResponse(skuRepository.save(sku));
    }

    @Retryable(retryFor = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 50))
    public void decrementarEstoque(Long skuId, int qtd) {
        Sku sku = buscarSku(skuId);
        if (sku.getQuantidade() < qtd) {
            throw new EstoqueInsuficienteException("Estoque insuficiente para SKU: " + skuId);
        }
        sku.setQuantidade(sku.getQuantidade() - qtd);
        skuRepository.save(sku);
    }

    public void restaurarEstoque(Long skuId, int qtd) {
        Sku sku = buscarSku(skuId);
        sku.setQuantidade(sku.getQuantidade() + qtd);
        skuRepository.save(sku);
    }

    private Sku buscarSku(Long skuId) {
        return skuRepository.findById(skuId)
                .orElseThrow(() -> new NoSuchElementException("SKU não encontrado: " + skuId));
    }

    private SkuEstoqueResponse toResponse(Sku s) {
        return new SkuEstoqueResponse(
                s.getId(),
                s.getProduto().getNome(),
                s.getCor().getNome(),
                s.getTamanho().getLabel(),
                s.getCodigo(),
                s.getQuantidade(),
                s.getQuantidade() > 0
        );
    }
}
