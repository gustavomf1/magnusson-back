CREATE TABLE usuario (
    id          BIGSERIAL    PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(180) NOT NULL UNIQUE,
    senha_hash  VARCHAR(60)  NOT NULL,
    cpf         VARCHAR(14)  NOT NULL UNIQUE,
    telefone    VARCHAR(20)  NOT NULL,
    role        VARCHAR(10)  NOT NULL DEFAULT 'CLIENT',
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Admin inicial — senha de dev: "admin123" (bcrypt cost=12)
-- IMPORTANTE: gere um novo hash antes do deploy de produção
INSERT INTO usuario (nome, email, senha_hash, cpf, telefone, role)
VALUES (
    'Admin',
    'admin@magnossao.com.br',
    '$2a$12$Ei4zjZFBSXPPdGAQ0qFhsOyBBfTlEahFqiRiqLhM6bF0U.8yLBMxu',
    '000.000.000-00',
    '(00) 00000-0000',
    'ADMIN'
);
