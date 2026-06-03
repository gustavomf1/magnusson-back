package com.magnossao.catalog;

import com.magnossao.catalog.model.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class SkuService {

    private final SkuRepository skuRepository;

    public SkuService(SkuRepository skuRepository) {
        this.skuRepository = skuRepository;
    }

    public String gerarCodigo(Produto produto, ProdutoCor cor, ProdutoTamanho tamanho) {
        return "%s-%s-%s".formatted(
            produto.getSlug(),
            cor.getToken().toLowerCase(),
            tamanho.getLabel().toLowerCase()
        );
    }

    public List<Sku> gerarSkus(Produto produto) {
        List<Sku> novos = new ArrayList<>();
        for (ProdutoCor cor : produto.getCores()) {
            for (ProdutoTamanho tamanho : produto.getTamanhos()) {
                if (skuRepository.existsByCorIdAndTamanhoId(cor.getId(), tamanho.getId())) {
                    continue;
                }
                Sku sku = new Sku();
                sku.setProduto(produto);
                sku.setCor(cor);
                sku.setTamanho(tamanho);
                sku.setCodigo(gerarCodigo(produto, cor, tamanho));
                novos.add(sku);
            }
        }
        if (!novos.isEmpty()) {
            skuRepository.saveAll(novos);
        }
        return novos;
    }
}
