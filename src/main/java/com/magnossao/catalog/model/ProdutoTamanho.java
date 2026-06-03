package com.magnossao.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "produto_tamanho")
@Getter @Setter @NoArgsConstructor
public class ProdutoTamanho {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, length = 5)
    private String label;

    private Integer peito;
    private Integer comprimento;
    private Integer ombro;
}
