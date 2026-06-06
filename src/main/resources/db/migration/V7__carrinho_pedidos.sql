CREATE TABLE endereco (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL REFERENCES usuario(id),
    logradouro    VARCHAR(255) NOT NULL,
    numero        VARCHAR(20)  NOT NULL,
    complemento   VARCHAR(100),
    bairro        VARCHAR(100) NOT NULL,
    cep           VARCHAR(9)   NOT NULL,
    cidade        VARCHAR(100) NOT NULL,
    uf            VARCHAR(2)   NOT NULL,
    principal     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE carrinho_item (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT NOT NULL REFERENCES usuario(id),
    sku_id        BIGINT NOT NULL REFERENCES sku(id),
    quantidade    INT    NOT NULL CHECK (quantidade > 0),
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, sku_id)
);

CREATE TABLE pedido (
    id              BIGSERIAL    PRIMARY KEY,
    usuario_id      BIGINT       REFERENCES usuario(id),
    status          VARCHAR(30)  NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
    total           NUMERIC(10,2) NOT NULL,
    nome_cliente    VARCHAR(255) NOT NULL,
    cpf_cnpj        VARCHAR(18)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    telefone        VARCHAR(20)  NOT NULL,
    end_logradouro  VARCHAR(255) NOT NULL,
    end_numero      VARCHAR(20)  NOT NULL,
    end_complemento VARCHAR(100),
    end_bairro      VARCHAR(100) NOT NULL,
    end_cep         VARCHAR(9)   NOT NULL,
    end_cidade      VARCHAR(100) NOT NULL,
    end_uf          VARCHAR(2)   NOT NULL,
    criado_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE pedido_item (
    id             BIGSERIAL      PRIMARY KEY,
    pedido_id      BIGINT         NOT NULL REFERENCES pedido(id),
    sku_id         BIGINT         NOT NULL REFERENCES sku(id),
    nome_produto   VARCHAR(255)   NOT NULL,
    cor            VARCHAR(100)   NOT NULL,
    tamanho        VARCHAR(50)    NOT NULL,
    preco_unitario NUMERIC(10,2)  NOT NULL,
    quantidade     INT            NOT NULL CHECK (quantidade > 0)
);
