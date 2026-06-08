package com.magnossao.repository;

import com.magnossao.entity.Cupom;
import com.magnossao.entity.StatusCupom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CupomRepository extends JpaRepository<Cupom, Long> {

    List<Cupom> findByStatusAndExpiraEmBefore(StatusCupom status, LocalDateTime limite);

    List<Cupom> findByPedidoItemOrigemIdAndStatus(Long pedidoItemOrigemId, StatusCupom status);

    @Query("""
            SELECT c FROM Cupom c
            WHERE c.usuario.id = :usuarioId
            ORDER BY CASE WHEN c.status = com.magnossao.entity.StatusCupom.ATIVO THEN 0 ELSE 1 END,
                     c.expiraEm ASC NULLS LAST,
                     c.criadoEm DESC
            """)
    List<Cupom> findCarteiraByUsuarioId(@Param("usuarioId") Long usuarioId);
}
