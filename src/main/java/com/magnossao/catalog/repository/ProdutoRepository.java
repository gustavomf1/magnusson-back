package com.magnossao.catalog.repository;

import com.magnossao.catalog.model.Produto;
import com.magnossao.catalog.model.StatusProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByStatusOrderByNomeAsc(StatusProduto status);

    Optional<Produto> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
