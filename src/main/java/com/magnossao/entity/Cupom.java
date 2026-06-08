package com.magnossao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupom")
@Getter
@Setter
public class Cupom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_item_origem_id", nullable = false)
    private PedidoItem pedidoItemOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_item_uso_id")
    private PedidoItem pedidoItemUso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCupom status = StatusCupom.ATIVO;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
