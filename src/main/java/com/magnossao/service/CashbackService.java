package com.magnossao.service;

import com.magnossao.dto.request.RegraCashbackRequest;
import com.magnossao.dto.response.CupomResponse;
import com.magnossao.dto.response.RegraCashbackResponse;
import com.magnossao.entity.Cupom;
import com.magnossao.entity.Pedido;
import com.magnossao.entity.PedidoItem;
import com.magnossao.entity.Produto;
import com.magnossao.entity.RegraCashback;
import com.magnossao.entity.StatusCupom;
import com.magnossao.entity.Usuario;
import com.magnossao.exception.CupomInvalidoException;
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
import java.util.Set;

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

    public record ResultadoCupom(BigDecimal desconto, Cupom cupom) {
    }

    @Transactional
    public ResultadoCupom validarEAplicar(Usuario usuario, Long cupomId, BigDecimal totalItem, Set<Long> cupomIdsAplicados) {
        Cupom cupom = cupomRepository.findById(cupomId)
                .orElseThrow(() -> new CupomInvalidoException("Cupom não encontrado"));

        if (!cupom.getUsuario().getId().equals(usuario.getId())) {
            throw new CupomInvalidoException("Cupom não pertence a este usuário");
        }
        if (cupom.getStatus() != StatusCupom.ATIVO) {
            throw new CupomInvalidoException("Cupom não está ativo");
        }
        if (cupom.getExpiraEm() != null && cupom.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new CupomInvalidoException("Cupom expirado");
        }
        if (!cupomIdsAplicados.add(cupomId)) {
            throw new CupomInvalidoException("Cupom já aplicado neste pedido");
        }

        BigDecimal desconto = cupom.getValor().min(totalItem);
        cupom.setStatus(StatusCupom.USADO);
        cupomRepository.save(cupom);

        return new ResultadoCupom(desconto, cupom);
    }

    @Transactional
    public void confirmarUso(Cupom cupom, PedidoItem itemPersistido) {
        cupom.setPedidoItemUso(itemPersistido);
        cupomRepository.save(cupom);
        itemPersistido.setCupomAplicado(cupom);
        // itemPersistido não precisa de save explícito: é entidade gerenciada nesta sessão JPA,
        // e o dirty-check na hora do flush persistirá a mudança automaticamente
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelarCuponsDoItem(Long pedidoItemOrigemId) {
        List<Cupom> ativos = cupomRepository.findByPedidoItemOrigemIdAndStatus(pedidoItemOrigemId, StatusCupom.ATIVO);
        for (Cupom cupom : ativos) {
            cupom.setStatus(StatusCupom.CANCELADO);
            cupomRepository.save(cupom);
        }
    }

    @Transactional
    public List<CupomResponse> listarCarteira(Long usuarioId) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado: " + usuarioId));
        return cupomRepository.findCarteiraByUsuarioId(usuarioId).stream()
                .map(CupomResponse::from)
                .toList();
    }

    @Transactional
    public List<CupomResponse> listarCuponsAdmin() {
        return cupomRepository.findAll().stream()
                .map(CupomResponse::from)
                .toList();
    }
}
