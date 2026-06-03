package com.magnossao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "produto_review")
@Getter @Setter @NoArgsConstructor
public class ProdutoReview {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String citacao;

    private String nome;

    private String cidade;
}
