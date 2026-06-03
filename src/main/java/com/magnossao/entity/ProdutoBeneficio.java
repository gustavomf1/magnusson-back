package com.magnossao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "produto_beneficio")
@Getter @Setter @NoArgsConstructor
public class ProdutoBeneficio {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "icone_nome", nullable = false, length = 50)
    private String iconeNome;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String corpo;

    @Column(nullable = false)
    private int ordem = 0;
}
