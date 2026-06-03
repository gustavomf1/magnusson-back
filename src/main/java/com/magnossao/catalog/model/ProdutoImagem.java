package com.magnossao.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "produto_imagem")
@Getter @Setter @NoArgsConstructor
public class ProdutoImagem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    private String alt;

    @Column(nullable = false)
    private int ordem = 0;

    @Column(name = "storage_chave")
    private String storageChave;
}
