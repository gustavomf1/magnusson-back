ALTER TABLE pedido
    ADD COLUMN mp_preference_id VARCHAR(100),
    ADD COLUMN mp_payment_id    VARCHAR(100),
    ADD COLUMN valor_estornado  NUMERIC(10,2) NOT NULL DEFAULT 0;

CREATE TABLE estorno (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT NOT NULL REFERENCES pedido(id),
    pedido_item_id  BIGINT NOT NULL REFERENCES pedido_item(id),
    sku_id          BIGINT NOT NULL REFERENCES sku(id),
    quantidade      INT            NOT NULL CHECK (quantidade > 0),
    valor           NUMERIC(10,2)  NOT NULL,
    mp_refund_id    VARCHAR(100)   NOT NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_estorno_pedido_item ON estorno(pedido_item_id);
CREATE INDEX idx_estorno_pedido ON estorno(pedido_id);
