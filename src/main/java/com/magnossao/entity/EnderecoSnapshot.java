package com.magnossao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor
public class EnderecoSnapshot {

    @Column(name = "end_logradouro", nullable = false, length = 255)
    private String logradouro;

    @Column(name = "end_numero", nullable = false, length = 20)
    private String numero;

    @Column(name = "end_complemento", length = 100)
    private String complemento;

    @Column(name = "end_bairro", nullable = false, length = 100)
    private String bairro;

    @Column(name = "end_cep", nullable = false, length = 9)
    private String cep;

    @Column(name = "end_cidade", nullable = false, length = 100)
    private String cidade;

    @Column(name = "end_uf", nullable = false, length = 2)
    private String uf;
}
