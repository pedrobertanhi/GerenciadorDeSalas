-- AUD-01 (#39): registro append-only de alterações auditáveis.
CREATE TABLE tb_eventos_auditoria (
    id BIGSERIAL PRIMARY KEY,
    entidade VARCHAR(80) NOT NULL,
    entidade_id BIGINT NOT NULL,
    acao VARCHAR(80) NOT NULL,
    ator_id BIGINT,
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    estado_anterior VARCHAR(80),
    estado_novo VARCHAR(80),
    detalhes JSONB,
    CONSTRAINT fk_eventos_auditoria_ator
        FOREIGN KEY (ator_id) REFERENCES tb_usuarios (id)
);

CREATE INDEX idx_eventos_auditoria_entidade
    ON tb_eventos_auditoria (entidade, entidade_id, ocorrido_em DESC);

CREATE INDEX idx_eventos_auditoria_ator
    ON tb_eventos_auditoria (ator_id, ocorrido_em DESC);

CREATE INDEX idx_eventos_auditoria_ocorrido_em
    ON tb_eventos_auditoria (ocorrido_em DESC);

-- A aplicação usa somente INSERT/SELECT para este histórico.
-- UPDATE e DELETE não são expostos por repositório, serviço ou controlador.
