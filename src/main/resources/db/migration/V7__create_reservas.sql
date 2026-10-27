-- RSV-01 (#23): tabelas de reservas e materiais reservados.
-- Convencoes: timestamps WITH TIME ZONE, periodo semiaberto validado por CHECK (termino > inicio),
-- controle de concorrencia por coluna versao, status restrito via CHECK.

CREATE TABLE tb_reservas (
    id BIGSERIAL PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    solicitante_id BIGINT NOT NULL,
    sala_id BIGINT NOT NULL,
    professor_id BIGINT,
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    termino TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CRIADA',
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reservas_solicitante
        FOREIGN KEY (solicitante_id) REFERENCES tb_usuarios (id),
    CONSTRAINT fk_reservas_sala
        FOREIGN KEY (sala_id) REFERENCES salas (id),
    CONSTRAINT fk_reservas_professor
        FOREIGN KEY (professor_id) REFERENCES tb_professores (id),
    CONSTRAINT ck_reservas_periodo CHECK (termino > inicio),
    CONSTRAINT ck_reservas_status CHECK (
        status IN ('CRIADA', 'APROVADA', 'REJEITADA', 'CANCELADA', 'EM_ANDAMENTO', 'CONCLUIDA')
    )
);

CREATE INDEX idx_reservas_sala_periodo
    ON tb_reservas (sala_id, inicio, termino);

CREATE INDEX idx_reservas_professor_periodo
    ON tb_reservas (professor_id, inicio, termino);

CREATE INDEX idx_reservas_solicitante
    ON tb_reservas (solicitante_id);

CREATE INDEX idx_reservas_status
    ON tb_reservas (status);

CREATE TABLE tb_reservas_materiais (
    id BIGSERIAL PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    reserva_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantidade INTEGER NOT NULL,
    CONSTRAINT fk_reservas_materiais_reserva
        FOREIGN KEY (reserva_id) REFERENCES tb_reservas (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservas_materiais_material
        FOREIGN KEY (material_id) REFERENCES tb_materiais (id),
    CONSTRAINT uq_reservas_materiais_reserva_material
        UNIQUE (reserva_id, material_id),
    CONSTRAINT ck_reservas_materiais_quantidade CHECK (quantidade > 0)
);

CREATE INDEX idx_reservas_materiais_reserva
    ON tb_reservas_materiais (reserva_id);