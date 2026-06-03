package com.magnossao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "produto_faq")
@Getter @Setter @NoArgsConstructor
public class ProdutoFaq {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pergunta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resposta;

    @Column(nullable = false)
    private int ordem = 0;
}
