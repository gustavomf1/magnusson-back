package com.magnossao.service;

import com.magnossao.dto.request.RegraCashbackRequest;
import com.magnossao.dto.response.RegraCashbackResponse;
import com.magnossao.entity.Produto;
import com.magnossao.entity.RegraCashback;
import com.magnossao.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CashbackService {

    private final RegraCashbackRepository regraCashbackRepository;
    private final CupomRepository cupomRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public List<RegraCashbackResponse> listarRegras() {
        return regraCashbackRepository.findAll().stream()
                .map(RegraCashbackResponse::from)
                .toList();
    }

    @Transactional
    public RegraCashbackResponse salvarRegra(Long produtoId, RegraCashbackRequest request) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));

        RegraCashback regra = regraCashbackRepository.findByProdutoId(produtoId)
                .orElseGet(() -> {
                    RegraCashback nova = new RegraCashback();
                    nova.setProduto(produto);
                    return nova;
                });

        regra.setPercentual(request.percentual());
        regra.setPrazoValidadeDias(request.prazoValidadeDias());

        return RegraCashbackResponse.from(regraCashbackRepository.save(regra));
    }

    @Transactional
    public void removerRegra(Long produtoId) {
        RegraCashback regra = regraCashbackRepository.findByProdutoId(produtoId)
                .orElseThrow(() -> new NoSuchElementException("Regra de cashback não encontrada para o produto: " + produtoId));
        regraCashbackRepository.delete(regra);
    }
}
