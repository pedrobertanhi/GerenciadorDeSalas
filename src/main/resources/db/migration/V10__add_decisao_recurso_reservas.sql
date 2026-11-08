-- APR-02 (#34): decisao do Responsavel sobre a aprovacao de recurso restrito.
-- Guarda quem decidiu, quando e o motivo da rejeicao, preservando o historico.

ALTER TABLE tb_reservas
    ADD COLUMN recurso_decidido_por_id BIGINT;

ALTER TABLE tb_reservas
    ADD COLUMN recurso_decidido_em TIMESTAMP WITH TIME ZONE;

ALTER TABLE tb_reservas
    ADD COLUMN recurso_motivo_rejeicao VARCHAR(500);

ALTER TABLE tb_reservas
    ADD CONSTRAINT fk_reservas_recurso_decidido_por
        FOREIGN KEY (recurso_decidido_por_id) REFERENCES tb_usuarios (id);

-- Decisao tomada (APROVADA ou REJEITADA) sempre registra autor e instante.
ALTER TABLE tb_reservas
    ADD CONSTRAINT ck_reservas_recurso_decisao_registrada
        CHECK (aprovacao_recurso NOT IN ('APROVADA', 'REJEITADA')
               OR (recurso_decidido_por_id IS NOT NULL AND recurso_decidido_em IS NOT NULL));

-- Rejeicao sempre tem motivo nao vazio.
ALTER TABLE tb_reservas
    ADD CONSTRAINT ck_reservas_recurso_motivo_rejeicao
        CHECK (aprovacao_recurso <> 'REJEITADA'
               OR (recurso_motivo_rejeicao IS NOT NULL AND recurso_motivo_rejeicao ~ '[^[:space:]]'));
