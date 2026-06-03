package com.magnossao.catalog;

import com.magnossao.catalog.model.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    List<Sku> findByProdutoId(Long produtoId);

    boolean existsByCorIdAndTamanhoId(Long corId, Long tamanhoId);
}
