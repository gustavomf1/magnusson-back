package com.magnossao.repository;

import com.magnossao.entity.RegraCashback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegraCashbackRepository extends JpaRepository<RegraCashback, Long> {

    Optional<RegraCashback> findByProdutoId(Long produtoId);
}
