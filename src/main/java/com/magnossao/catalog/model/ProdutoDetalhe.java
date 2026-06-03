package com.magnossao.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "produto_detalhe")
@Getter @Setter @NoArgsConstructor
public class ProdutoDetalhe {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    private String label;

    @Column(name = "url_imagem", nullable = false, columnDefinition = "TEXT")
    private String urlImagem;

    private String alt;

    @Column(nullable = false)
    private int ordem = 0;
}
