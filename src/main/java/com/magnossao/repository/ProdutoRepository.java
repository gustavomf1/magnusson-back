package com.magnossao.repository;

import com.magnossao.entity.Categoria;
import com.magnossao.entity.Produto;
import com.magnossao.entity.StatusProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository
        extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    List<Produto> findByStatusOrderByNomeAsc(StatusProduto status);

    List<Produto> findByStatusAndCategoriaOrderByNomeAsc(StatusProduto status, Categoria categoria);

    Optional<Produto> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
