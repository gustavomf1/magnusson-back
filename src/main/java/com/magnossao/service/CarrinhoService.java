package com.magnossao.service;

import com.magnossao.dto.request.CarrinhoItemRequest;
import com.magnossao.dto.request.MergeCarrinhoRequest;
import com.magnossao.dto.response.CarrinhoItemResponse;
import com.magnossao.entity.CarrinhoItem;
import com.magnossao.entity.Sku;
import com.magnossao.entity.Usuario;
import com.magnossao.repository.CarrinhoItemRepository;
import com.magnossao.repository.SkuRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class CarrinhoService {

    private final CarrinhoItemRepository carrinhoItemRepository;
    private final SkuRepository skuRepository;

    public CarrinhoService(CarrinhoItemRepository carrinhoItemRepository, SkuRepository skuRepository) {
        this.carrinhoItemRepository = carrinhoItemRepository;
        this.skuRepository = skuRepository;
    }

    public List<CarrinhoItemResponse> listar(Long usuarioId) {
        return carrinhoItemRepository.findByUsuarioId(usuarioId).stream()
                .map(CarrinhoItemResponse::from).toList();
    }

    public CarrinhoItemResponse adicionar(Usuario usuario, CarrinhoItemRequest req) {
        Sku sku = buscarSku(req.skuId());
        var existing = carrinhoItemRepository.findByUsuarioIdAndSkuId(usuario.getId(), req.skuId());
        if (existing.isPresent()) {
            CarrinhoItem item = existing.get();
            item.setQuantidade(item.getQuantidade() + req.quantidade());
            item.setAtualizadoEm(OffsetDateTime.now());
            return CarrinhoItemResponse.from(carrinhoItemRepository.save(item));
        }
        CarrinhoItem item = new CarrinhoItem();
        item.setUsuario(usuario);
        item.setSku(sku);
        item.setQuantidade(req.quantidade());
        return CarrinhoItemResponse.from(carrinhoItemRepository.save(item));
    }

    public CarrinhoItemResponse atualizar(Long usuarioId, Long skuId, int quantidade) {
        CarrinhoItem item = carrinhoItemRepository.findByUsuarioIdAndSkuId(usuarioId, skuId)
                .orElseThrow(() -> new NoSuchElementException("Item não encontrado no carrinho"));
        item.setQuantidade(quantidade);
        item.setAtualizadoEm(OffsetDateTime.now());
        return CarrinhoItemResponse.from(carrinhoItemRepository.save(item));
    }

    public void remover(Long usuarioId, Long skuId) {
        CarrinhoItem item = carrinhoItemRepository.findByUsuarioIdAndSkuId(usuarioId, skuId)
                .orElseThrow(() -> new NoSuchElementException("Item não encontrado no carrinho"));
        carrinhoItemRepository.delete(item);
    }

    public List<CarrinhoItemResponse> merge(Usuario usuario, MergeCarrinhoRequest req) {
        for (CarrinhoItemRequest itemReq : req.itens()) {
            adicionar(usuario, itemReq);
        }
        return listar(usuario.getId());
    }

    public void limparCarrinho(Long usuarioId) {
        carrinhoItemRepository.deleteByUsuarioId(usuarioId);
    }

    private Sku buscarSku(Long skuId) {
        return skuRepository.findById(skuId)
                .orElseThrow(() -> new NoSuchElementException("SKU não encontrado: " + skuId));
    }
}
