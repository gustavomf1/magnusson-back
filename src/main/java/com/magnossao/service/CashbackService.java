package com.magnossao.service;

import com.magnossao.dto.request.RegraCashbackRequest;
import com.magnossao.dto.response.RegraCashbackResponse;
import com.magnossao.entity.Cupom;
import com.magnossao.entity.Pedido;
import com.magnossao.entity.PedidoItem;
import com.magnossao.entity.Produto;
import com.magnossao.entity.RegraCashback;
import com.magnossao.entity.StatusCupom;
import com.magnossao.entity.Usuario;
import com.magnossao.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void gerarCupons(Pedido pedido) {
        if (pedido.getUsuario() == null) {
            return;
        }

        Pedido carregado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        Usuario usuario = carregado.getUsuario();

        for (PedidoItem item : carregado.getItens()) {
            Produto produto = item.getSku().getProduto();
            RegraCashback regra = produto.getRegraCashback();
            if (regra == null) {
                continue;
            }

            BigDecimal valorCupom = item.getPrecoUnitario()
                    .multiply(regra.getPercentual())
                    .divide(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            Cupom cupom = new Cupom();
            cupom.setUsuario(usuario);
            cupom.setValor(valorCupom);
            cupom.setPedidoItemOrigem(item);
            cupom.setStatus(StatusCupom.ATIVO);
            if (regra.getPrazoValidadeDias() != null) {
                cupom.setExpiraEm(LocalDateTime.now().plusDays(regra.getPrazoValidadeDias()));
            }

            cupomRepository.save(cupom);
        }
    }
}
