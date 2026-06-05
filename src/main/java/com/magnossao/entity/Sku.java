package com.magnossao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "sku")
@Getter @Setter @NoArgsConstructor
public class Sku {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cor_id", nullable = false)
    private ProdutoCor cor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tamanho_id", nullable = false)
    private ProdutoTamanho tamanho;

    @Column(unique = true, nullable = false, length = 100)
    private String codigo;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private int quantidade = 0;

    @Version
    private long version;
}
