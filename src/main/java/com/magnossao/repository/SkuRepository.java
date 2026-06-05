package com.magnossao.repository;

import com.magnossao.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    List<Sku> findByProdutoId(Long produtoId);

    boolean existsByCorIdAndTamanhoId(Long corId, Long tamanhoId);

    @Query("SELECT s FROM Sku s JOIN FETCH s.produto JOIN FETCH s.cor JOIN FETCH s.tamanho ORDER BY s.produto.nome ASC, s.cor.nome ASC")
    List<Sku> findAllComRelacoes();
}
