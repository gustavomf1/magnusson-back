package com.magnossao.repository;

import com.magnossao.entity.CarrinhoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CarrinhoItemRepository extends JpaRepository<CarrinhoItem, Long> {
    List<CarrinhoItem> findByUsuarioId(Long usuarioId);
    Optional<CarrinhoItem> findByUsuarioIdAndSkuId(Long usuarioId, Long skuId);
    void deleteByUsuarioId(Long usuarioId);
}
