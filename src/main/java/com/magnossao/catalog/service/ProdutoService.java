package com.magnossao.catalog.service;

import com.magnossao.catalog.dto.*;
import com.magnossao.catalog.repository.ProdutoRepository;
import com.magnossao.catalog.repository.SkuRepository;
import com.magnossao.catalog.model.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final SkuRepository skuRepository;
    private final StorageService storageService;

    public ProdutoService(ProdutoRepository produtoRepository,
                          SkuRepository skuRepository,
                          StorageService storageService) {
        this.produtoRepository = produtoRepository;
        this.skuRepository = skuRepository;
        this.storageService = storageService;
    }

    public List<ProdutoResumoResponse> listarPublicados() {
        return produtoRepository
            .findByStatusOrderByNomeAsc(StatusProduto.PUBLICADO)
            .stream().map(this::toResumo).toList();
    }

    public List<ProdutoResumoResponse> listarTodos() {
        return produtoRepository.findAll()
            .stream().map(this::toResumo).toList();
    }

    public ProdutoResponse buscarPorSlug(String slug) {
        Produto p = produtoRepository.findBySlug(slug)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + slug));
        return toResponse(p);
    }

    public ProdutoResponse buscarPorId(Long id) {
        Produto p = produtoRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));
        return toResponse(p);
    }

    public ProdutoResponse criar(ProdutoRequest req) {
        if (produtoRepository.existsBySlug(req.slug())) {
            throw new IllegalArgumentException("Slug já existe: " + req.slug());
        }
        Produto p = new Produto();
        aplicarRequest(p, req);
        return toResponse(produtoRepository.save(p));
    }

    public ProdutoResponse atualizar(Long id, ProdutoRequest req) {
        Produto p = produtoRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));
        aplicarRequest(p, req);
        return toResponse(produtoRepository.save(p));
    }

    public ProdutoResponse mudarStatus(Long id, String status) {
        Produto p = produtoRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));
        p.setStatus(StatusProduto.valueOf(status));
        return toResponse(produtoRepository.save(p));
    }

    public PresignedUploadResponse gerarUrlUpload(Long produtoId, String contentType) {
        produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        String chave = "produtos/%d/%d".formatted(produtoId, System.currentTimeMillis());
        StorageService.PresignedUploadResult result = storageService.gerarUrlUpload(chave, contentType);
        return new PresignedUploadResponse(result.uploadUrl(), result.chave(), result.urlPublica());
    }

    public void confirmarImagem(Long produtoId, String chave, String url, String alt) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        int proxOrdem = p.getImagens().stream().mapToInt(ProdutoImagem::getOrdem).max().orElse(-1) + 1;
        ProdutoImagem img = new ProdutoImagem();
        img.setProduto(p);
        img.setUrl(url);
        img.setAlt(alt);
        img.setOrdem(proxOrdem);
        img.setStorageChave(chave);
        p.getImagens().add(img);
        produtoRepository.save(p);
    }

    public void deletarImagem(Long produtoId, Long imagemId) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        p.getImagens().stream()
            .filter(i -> i.getId().equals(imagemId))
            .findFirst()
            .ifPresent(img -> {
                if (img.getStorageChave() != null) {
                    storageService.deletar(img.getStorageChave());
                }
                p.getImagens().remove(img);
            });
        produtoRepository.save(p);
    }

    public void reordenarImagens(Long produtoId, List<Long> ids) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        for (int i = 0; i < ids.size(); i++) {
            final int ordem = i;
            final Long imagemId = ids.get(i);
            p.getImagens().stream()
                .filter(img -> img.getId().equals(imagemId))
                .findFirst()
                .ifPresent(img -> img.setOrdem(ordem));
        }
        produtoRepository.save(p);
    }

    public CorDto adicionarCor(Long produtoId, CorDto req) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        ProdutoCor cor = new ProdutoCor();
        cor.setProduto(p); cor.setNome(req.nome()); cor.setToken(req.token()); cor.setHex(req.hex());
        p.getCores().add(cor);
        produtoRepository.save(p);
        ProdutoCor saved = p.getCores().getLast();
        return new CorDto(saved.getId(), saved.getNome(), saved.getToken(), saved.getHex());
    }

    public void deletarCor(Long produtoId, Long corId) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        p.getCores().removeIf(c -> c.getId().equals(corId));
        produtoRepository.save(p);
    }

    public TamanhoDto adicionarTamanho(Long produtoId, TamanhoDto req) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        ProdutoTamanho t = new ProdutoTamanho();
        t.setProduto(p); t.setLabel(req.label()); t.setPeito(req.peito());
        t.setComprimento(req.comprimento()); t.setOmbro(req.ombro());
        p.getTamanhos().add(t);
        produtoRepository.save(p);
        ProdutoTamanho saved = p.getTamanhos().getLast();
        return new TamanhoDto(saved.getId(), saved.getLabel(), saved.getPeito(), saved.getComprimento(), saved.getOmbro());
    }

    public void deletarTamanho(Long produtoId, Long tamanhoId) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        p.getTamanhos().removeIf(t -> t.getId().equals(tamanhoId));
        produtoRepository.save(p);
    }

    public BeneficioDto adicionarBeneficio(Long produtoId, BeneficioDto req) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        ProdutoBeneficio b = new ProdutoBeneficio();
        b.setProduto(p); b.setIconeNome(req.iconeNome()); b.setTitulo(req.titulo());
        b.setCorpo(req.corpo()); b.setOrdem(req.ordem());
        p.getBeneficios().add(b);
        produtoRepository.save(p);
        ProdutoBeneficio saved = p.getBeneficios().getLast();
        return new BeneficioDto(saved.getId(), saved.getIconeNome(), saved.getTitulo(), saved.getCorpo(), saved.getOrdem());
    }

    public void deletarBeneficio(Long beneficioId) {
        // Deletado via orphanRemoval — remove direto do JPA
    }

    public ReviewDto adicionarReview(Long produtoId, ReviewDto req) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        ProdutoReview r = new ProdutoReview();
        r.setProduto(p); r.setCitacao(req.citacao()); r.setNome(req.nome()); r.setCidade(req.cidade());
        p.getReviews().add(r);
        produtoRepository.save(p);
        ProdutoReview saved = p.getReviews().getLast();
        return new ReviewDto(saved.getId(), saved.getCitacao(), saved.getNome(), saved.getCidade());
    }

    public FaqDto adicionarFaq(Long produtoId, FaqDto req) {
        Produto p = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));
        ProdutoFaq f = new ProdutoFaq();
        f.setProduto(p); f.setPergunta(req.pergunta()); f.setResposta(req.resposta()); f.setOrdem(req.ordem());
        p.getFaqs().add(f);
        produtoRepository.save(p);
        ProdutoFaq saved = p.getFaqs().getLast();
        return new FaqDto(saved.getId(), saved.getPergunta(), saved.getResposta(), saved.getOrdem());
    }

    private void aplicarRequest(Produto p, ProdutoRequest req) {
        p.setSlug(req.slug()); p.setNome(req.nome()); p.setNomeCurto(req.nomeCurto());
        p.setColecao(req.colecao()); p.setPreco(req.preco());
        p.setDescricao(req.descricao()); p.setDescricaoSeo(req.descricaoSeo());
    }

    private ProdutoResumoResponse toResumo(Produto p) {
        String imagemPrincipal = p.getImagens().isEmpty() ? null : p.getImagens().getFirst().getUrl();
        return new ProdutoResumoResponse(p.getId(), p.getSlug(), p.getNome(), p.getNomeCurto(),
            p.getColecao(), p.getPreco(), p.getStatus().name(), imagemPrincipal);
    }

    ProdutoResponse toResponse(Produto p) {
        return new ProdutoResponse(
            p.getId(), p.getSlug(), p.getNome(), p.getNomeCurto(),
            p.getColecao(), p.getPreco(), p.getDescricao(), p.getDescricaoSeo(),
            p.getStatus().name(),
            p.getImagens().stream().map(i -> new ImagemDto(i.getId(), i.getUrl(), i.getAlt(), i.getOrdem())).toList(),
            p.getCores().stream().map(c -> new CorDto(c.getId(), c.getNome(), c.getToken(), c.getHex())).toList(),
            p.getTamanhos().stream().map(t -> new TamanhoDto(t.getId(), t.getLabel(), t.getPeito(), t.getComprimento(), t.getOmbro())).toList(),
            p.getSkus().stream().map(s -> new SkuDto(s.getId(), s.getCor().getId(), s.getTamanho().getId(), s.getCodigo(), s.isAtivo())).toList(),
            p.getBeneficios().stream().map(b -> new BeneficioDto(b.getId(), b.getIconeNome(), b.getTitulo(), b.getCorpo(), b.getOrdem())).toList(),
            p.getDetalhes().stream().map(d -> new DetalheDto(d.getId(), d.getLabel(), d.getUrlImagem(), d.getAlt(), d.getOrdem())).toList(),
            p.getReviews().stream().map(r -> new ReviewDto(r.getId(), r.getCitacao(), r.getNome(), r.getCidade())).toList(),
            p.getFaqs().stream().map(f -> new FaqDto(f.getId(), f.getPergunta(), f.getResposta(), f.getOrdem())).toList()
        );
    }
}
