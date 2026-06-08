CREATE TABLE regra_cashback (
    id                  BIGSERIAL PRIMARY KEY,
    produto_id          BIGINT        NOT NULL UNIQUE REFERENCES produto(id),
    percentual          NUMERIC(5,2)  NOT NULL CHECK (percentual > 0 AND percentual <= 100),
    prazo_validade_dias INT           CHECK (prazo_validade_dias > 0),
    criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE cupom (
    id                     BIGSERIAL     PRIMARY KEY,
    usuario_id             BIGINT        NOT NULL REFERENCES usuario(id),
    valor                  NUMERIC(10,2) NOT NULL CHECK (valor > 0),
    pedido_item_origem_id  BIGINT        NOT NULL REFERENCES pedido_item(id),
    pedido_item_uso_id     BIGINT        REFERENCES pedido_item(id),
    status                 VARCHAR(20)   NOT NULL DEFAULT 'ATIVO',
    expira_em              TIMESTAMP,
    criado_em              TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em          TIMESTAMP     NOT NULL DEFAULT NOW()
);

ALTER TABLE pedido_item
    ADD COLUMN cupom_aplicado_id BIGINT REFERENCES cupom(id);

CREATE INDEX idx_cupom_usuario_status ON cupom(usuario_id, status);
CREATE INDEX idx_cupom_expira_em ON cupom(expira_em);
