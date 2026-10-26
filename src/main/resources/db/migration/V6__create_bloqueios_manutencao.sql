CREATE TABLE tb_bloqueios_manutencao (
    id BIGSERIAL PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    recurso_tipo VARCHAR(20) NOT NULL,
    recurso_id BIGINT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    fim TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    encerrado_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_bloqueios_recurso_tipo CHECK (recurso_tipo IN ('SALA', 'PROFESSOR', 'MATERIAL')),
    CONSTRAINT ck_bloqueios_recurso_id CHECK (recurso_id > 0),
    CONSTRAINT ck_bloqueios_motivo CHECK (motivo ~ '[^[:space:]]'),
    CONSTRAINT ck_bloqueios_periodo CHECK (fim > inicio),
    CONSTRAINT ck_bloqueios_status CHECK (status IN ('ATIVO', 'CONCLUIDO', 'CANCELADO')),
    CONSTRAINT ck_bloqueios_encerramento CHECK (
        (status = 'ATIVO' AND encerrado_em IS NULL)
        OR (status <> 'ATIVO' AND encerrado_em IS NOT NULL)
    )
);

CREATE INDEX idx_bloqueios_recurso_periodo
    ON tb_bloqueios_manutencao (recurso_tipo, recurso_id, inicio, fim);

CREATE INDEX idx_bloqueios_status
    ON tb_bloqueios_manutencao (status);