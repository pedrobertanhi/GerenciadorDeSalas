-- NOT-01 (#41): histórico técnico de notificações sem armazenar resposta externa sensível.
CREATE TABLE tb_registros_notificacao (
    id BIGSERIAL PRIMARY KEY,
    evento VARCHAR(80) NOT NULL,
    entidade VARCHAR(80) NOT NULL,
    entidade_id BIGINT NOT NULL,
    destino VARCHAR(254) NOT NULL,
    status VARCHAR(20) NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 1,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    concluido_em TIMESTAMP WITH TIME ZONE,
    codigo_resposta INTEGER,
    erro_codigo VARCHAR(80),
    CONSTRAINT ck_registros_notificacao_status
        CHECK (status IN ('PENDENTE', 'ENVIADA', 'FALHA_PERMANENTE', 'FALHA_TRANSITORIA')),
    CONSTRAINT ck_registros_notificacao_tentativas CHECK (tentativas > 0)
);

CREATE INDEX idx_registros_notificacao_entidade
    ON tb_registros_notificacao (entidade, entidade_id, criado_em DESC);
