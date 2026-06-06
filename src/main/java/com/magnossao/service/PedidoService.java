package com.magnossao.service;

import com.magnossao.dto.request.CheckoutRequest;
import com.magnossao.dto.response.PedidoResponse;
import com.magnossao.dto.response.PedidoResumoResponse;
import com.magnossao.entity.*;
import com.magnossao.exception.EstoqueInsuficienteException;
import com.magnossao.exception.PedidoNaoEncontradoException;
import com.magnossao.repository.PedidoRepository;
import com.magnossao.repository.SkuRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final SkuRepository skuRepository;
    private final EstoqueService estoqueService;
    private final CarrinhoService carrinhoService;
    private final TransactionTemplate txTemplate;

    public PedidoService(PedidoRepository pedidoRepository, SkuRepository skuRepository,
                         EstoqueService estoqueService, CarrinhoService carrinhoService,
                         TransactionTemplate txTemplate) {
        this.pedidoRepository = pedidoRepository;
        this.skuRepository = skuRepository;
        this.estoqueService = estoqueService;
        this.carrinhoService = carrinhoService;
        this.txTemplate = txTemplate;
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

        return txTemplate.execute(status -> {
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

            BigDecimal total = BigDecimal.ZERO;
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
                total = total.add(sku.getProduto().getPreco().multiply(BigDecimal.valueOf(itemReq.quantidade())));
            }
            pedido.setTotal(total);

            Pedido salvo = pedidoRepository.save(pedido);

            if (usuario != null) {
                carrinhoService.limparCarrinho(usuario.getId());
            }

            return PedidoResponse.from(salvo);
        });
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
        return pedidoRepository.findComFiltros(status, inicio, fim, cliente, pageable)
                .map(PedidoResumoResponse::from);
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
