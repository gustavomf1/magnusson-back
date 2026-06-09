package com.magnossao.service;

import com.magnossao.dto.request.CheckoutRequest;
import com.magnossao.dto.response.PedidoResponse;
import com.magnossao.dto.response.PedidoResumoResponse;
import com.magnossao.entity.*;
import com.magnossao.exception.EstoqueInsuficienteException;
import com.magnossao.exception.PedidoNaoEncontradoException;
import com.magnossao.repository.PedidoRepository;
import com.magnossao.repository.SkuRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PedidoService {

    private record ItemCupom(PedidoItem item, CashbackService.ResultadoCupom cupom) {
    }

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final SkuRepository skuRepository;
    private final EstoqueService estoqueService;
    private final CarrinhoService carrinhoService;
    private final TransactionTemplate txTemplate;
    private final PagamentoService pagamentoService;
    private final CashbackService cashbackService;

    public PedidoService(PedidoRepository pedidoRepository, SkuRepository skuRepository,
                         EstoqueService estoqueService, CarrinhoService carrinhoService,
                         TransactionTemplate txTemplate, PagamentoService pagamentoService,
                         CashbackService cashbackService) {
        this.pedidoRepository = pedidoRepository;
        this.skuRepository = skuRepository;
        this.estoqueService = estoqueService;
        this.carrinhoService = carrinhoService;
        this.txTemplate = txTemplate;
        this.pagamentoService = pagamentoService;
        this.cashbackService = cashbackService;
    }

    // SEM @Transactional externo: @Retryable do EstoqueService precisa de transação própria.
    public PedidoResponse checkout(CheckoutRequest req, Usuario usuario) {
        for (var itemReq : req.itens()) {
            Sku sku = skuRepository.findById(itemReq.skuId())
                    .orElseThrow(() -> new IllegalArgumentException("SKU não encontrado: " + itemReq.skuId()));
            if (!sku.isAtivo()) {
                throw new IllegalArgumentException("SKU inativo: " + itemReq.skuId());
            }
        }

        List<long[]> decrementados = new ArrayList<>();
        try {
            for (int i = 0; i < req.itens().size(); i++) {
                var itemReq = req.itens().get(i);
                estoqueService.decrementarEstoque(itemReq.skuId(), itemReq.quantidade());
                decrementados.add(new long[]{itemReq.skuId(), itemReq.quantidade()});
            }
        } catch (EstoqueInsuficienteException e) {
            for (long[] d : decrementados) {
                estoqueService.restaurarEstoque(d[0], (int) d[1]);
            }
            throw e;
        }

        Pedido salvo = txTemplate.execute(status -> {
            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);

            DadosNf nf = new DadosNf();
            nf.setNomeCliente(req.dadosNf().nomeCliente());
            nf.setCpfCnpj(req.dadosNf().cpfCnpj());
            nf.setEmail(req.dadosNf().email());
            nf.setTelefone(req.dadosNf().telefone());
            pedido.setDadosNf(nf);

            EnderecoSnapshot end = new EnderecoSnapshot();
            end.setLogradouro(req.endereco().logradouro());
            end.setNumero(req.endereco().numero());
            end.setComplemento(req.endereco().complemento());
            end.setBairro(req.endereco().bairro());
            end.setCep(req.endereco().cep());
            end.setCidade(req.endereco().cidade());
            end.setUf(req.endereco().uf());
            pedido.setEndereco(end);

            List<ItemCupom> itensComCupom = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            Set<Long> cupomIdsAplicados = new HashSet<>();

            for (var itemReq : req.itens()) {
                // Reload SKU inside transaction so lazy associations are available
                Sku sku = skuRepository.findById(itemReq.skuId()).orElseThrow();
                PedidoItem item = new PedidoItem();
                item.setPedido(pedido);
                item.setSku(sku);
                item.setNomeProduto(sku.getProduto().getNome());
                item.setCor(sku.getCor().getNome());
                item.setTamanho(sku.getTamanho().getLabel());
                item.setPrecoUnitario(sku.getProduto().getPreco());
                item.setQuantidade(itemReq.quantidade());
                pedido.getItens().add(item);

                BigDecimal totalItem = sku.getProduto().getPreco().multiply(BigDecimal.valueOf(itemReq.quantidade()));
                total = total.add(totalItem);

                // Fase 1: validar e marcar cupom como USADO (antes do save — entidade transiente)
                // DEVE executar dentro do txTemplate para que o rollback do cupom acompanhe o rollback do pedido
                if (itemReq.cupomId() != null && usuario != null) {
                    var resultado = cashbackService.validarEAplicar(usuario, itemReq.cupomId(), totalItem, cupomIdsAplicados);
                    total = total.subtract(resultado.desconto());
                    itensComCupom.add(new ItemCupom(item, resultado));
                }
            }
            pedido.setTotal(total);

            Pedido salvoNaTx = pedidoRepository.save(pedido);

            // Fase 2: ligar cupom ao item persistido (após o save — item já tem id)
            for (ItemCupom ic : itensComCupom) {
                cashbackService.confirmarUso(ic.cupom().cupom(), ic.item());
            }

            if (usuario != null) {
                carrinhoService.limparCarrinho(usuario.getId());
            }

            return salvoNaTx;
        });

        String initPoint = null;
        try {
            initPoint = pagamentoService.criarPreferencia(salvo);
            pedidoRepository.save(salvo);
        } catch (Exception e) {
            // Falha ao criar a preferência não desfaz o pedido — o job de expiração
            // eventualmente cancela e restaura o estoque (ver PagamentoService spec, fluxo 1).
            log.warn("Falha ao criar preferência de pagamento no Mercado Pago para o pedido {}: {}", salvo.getId(), e.getMessage());
        }

        return PedidoResponse.from(salvo, initPoint);
    }

    @Transactional
    public PedidoResponse buscarPorId(Long id) {
        return PedidoResponse.from(pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id)));
    }

    @Transactional
    public List<PedidoResumoResponse> historico(Long usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream().map(PedidoResumoResponse::from).toList();
    }

    @Transactional
    public Page<PedidoResumoResponse> listarAdmin(StatusPedido status, OffsetDateTime inicio,
                                                   OffsetDateTime fim, String cliente, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "criadoEm"));
        }
        Specification<Pedido> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (inicio != null) predicates.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), inicio));
            if (fim != null) predicates.add(cb.lessThanOrEqualTo(root.get("criadoEm"), fim));
            if (cliente != null && !cliente.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("dadosNf").get("nomeCliente")),
                        "%" + cliente.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return pedidoRepository.findAll(spec, pageable).map(PedidoResumoResponse::from);
    }

    @Transactional
    public PedidoResponse atualizarStatus(Long pedidoId, String statusStr) {
        StatusPedido novoStatus;
        try {
            novoStatus = StatusPedido.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + statusStr);
        }
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));
        pedido.setStatus(novoStatus);
        pedido.setAtualizadoEm(OffsetDateTime.now());
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }
}
