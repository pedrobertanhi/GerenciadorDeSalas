CREATE TABLE tb_materiais (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    tipo VARCHAR(20) NOT NULL,
    patrimonio VARCHAR(100),
    quantidade_total INTEGER NOT NULL,
    localizacao VARCHAR(150) NOT NULL,
    restrito BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_materiais_nome CHECK (BTRIM(nome) <> ''),
    CONSTRAINT ck_materiais_tipo CHECK (tipo IN ('MATERIAL', 'EQUIPAMENTO')),
    CONSTRAINT ck_materiais_patrimonio CHECK (
        patrimonio IS NULL OR (BTRIM(patrimonio) <> '' AND patrimonio = UPPER(patrimonio))
    ),
    CONSTRAINT ck_materiais_quantidade CHECK (quantidade_total >= 0),
    CONSTRAINT ck_materiais_localizacao CHECK (BTRIM(localizacao) <> '')
);

CREATE UNIQUE INDEX uq_materiais_patrimonio
    ON tb_materiais (patrimonio)
    WHERE patrimonio IS NOT NULL;

CREATE INDEX idx_materiais_busca_ativos
    ON tb_materiais (ativo, tipo, restrito);
