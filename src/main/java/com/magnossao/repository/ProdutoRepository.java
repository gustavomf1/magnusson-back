package com.magnossao.repository;

import com.magnossao.entity.Produto;
import com.magnossao.entity.StatusProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByStatusOrderByNomeAsc(StatusProduto status);

    Optional<Produto> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
