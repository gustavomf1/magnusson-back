CREATE TABLE produto (
    id            BIGSERIAL PRIMARY KEY,
    slug          VARCHAR(100) UNIQUE NOT NULL,
    nome          VARCHAR(200) NOT NULL,
    nome_curto    VARCHAR(100),
    colecao       VARCHAR(100),
    preco         NUMERIC(10,2) NOT NULL,
    descricao     TEXT,
    descricao_seo TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO'
                  CHECK (status IN ('RASCUNHO', 'PUBLICADO', 'ARQUIVADO')),
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE produto_imagem (
    id         BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    url        TEXT NOT NULL,
    alt        TEXT,
    ordem      INT NOT NULL DEFAULT 0
);

CREATE TABLE produto_cor (
    id         BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    nome       VARCHAR(100) NOT NULL,
    token      VARCHAR(50) NOT NULL,
    hex        VARCHAR(7) NOT NULL
);

CREATE TABLE produto_tamanho (
    id          BIGSERIAL PRIMARY KEY,
    produto_id  BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    label       VARCHAR(5) NOT NULL CHECK (label IN ('P', 'M', 'G', 'GG')),
    peito       INT,
    comprimento INT,
    ombro       INT
);

CREATE TABLE sku (
    id           BIGSERIAL PRIMARY KEY,
    produto_id   BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    cor_id       BIGINT NOT NULL REFERENCES produto_cor(id),
    tamanho_id   BIGINT NOT NULL REFERENCES produto_tamanho(id),
    codigo       VARCHAR(100) UNIQUE NOT NULL,
    ativo        BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (cor_id, tamanho_id)
);

CREATE TABLE produto_beneficio (
    id         BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    icone_nome VARCHAR(50) NOT NULL,
    titulo     VARCHAR(100) NOT NULL,
    corpo      TEXT NOT NULL,
    ordem      INT NOT NULL DEFAULT 0
);

CREATE TABLE produto_detalhe (
    id         BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    label      VARCHAR(100),
    url_imagem TEXT NOT NULL,
    alt        TEXT,
    ordem      INT NOT NULL DEFAULT 0
);

CREATE TABLE produto_review (
    id         BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    citacao    TEXT NOT NULL,
    nome       VARCHAR(100),
    cidade     VARCHAR(100)
);

CREATE TABLE produto_faq (
    id         BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    pergunta   TEXT NOT NULL,
    resposta   TEXT NOT NULL,
    ordem      INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_produto_status ON produto(status);
CREATE INDEX idx_produto_slug   ON produto(slug);
CREATE INDEX idx_imagem_ordem   ON produto_imagem(produto_id, ordem);
