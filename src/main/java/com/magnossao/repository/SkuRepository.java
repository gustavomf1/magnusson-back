package com.magnossao.repository;

import com.magnossao.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    List<Sku> findByProdutoId(Long produtoId);

    boolean existsByCorIdAndTamanhoId(Long corId, Long tamanhoId);
}
