-- APR-03 (#34): validacao docente feita pelo Responsavel.
-- Guarda quem validou ou rejeitou a alocacao do professor, quando e o motivo da rejeicao.

ALTER TABLE tb_reservas
    ADD COLUMN docente_decidido_por_id BIGINT;

ALTER TABLE tb_reservas
    ADD COLUMN docente_decidido_em TIMESTAMP WITH TIME ZONE;

ALTER TABLE tb_reservas
    ADD COLUMN docente_motivo_rejeicao VARCHAR(500);

ALTER TABLE tb_reservas
    ADD CONSTRAINT fk_reservas_docente_decidido_por
        FOREIGN KEY (docente_decidido_por_id) REFERENCES tb_usuarios (id);

-- Validacao decidida (APROVADA ou REJEITADA) sempre registra autor e instante.
ALTER TABLE tb_reservas
    ADD CONSTRAINT ck_reservas_docente_decisao_registrada
        CHECK (validacao_docente NOT IN ('APROVADA', 'REJEITADA')
               OR (docente_decidido_por_id IS NOT NULL AND docente_decidido_em IS NOT NULL));

-- Rejeicao da alocacao docente sempre tem motivo nao vazio.
ALTER TABLE tb_reservas
    ADD CONSTRAINT ck_reservas_docente_motivo_rejeicao
        CHECK (validacao_docente <> 'REJEITADA'
               OR (docente_motivo_rejeicao IS NOT NULL AND docente_motivo_rejeicao ~ '[^[:space:]]'));
