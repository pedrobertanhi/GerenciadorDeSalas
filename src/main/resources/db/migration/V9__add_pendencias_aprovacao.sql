-- APR-01 (#34): pendencias de decisao da reserva.
-- aprovacao_recurso: exigida quando a reserva usa material restrito.
-- validacao_docente: exigida quando a reserva tem professor (APR-03).
-- Sem nenhuma pendencia, a reserva e aprovada automaticamente pelo sistema.
-- O status CRIADA corresponde ao SOLICITADA do modelo de estados.

ALTER TABLE tb_reservas
    ADD COLUMN aprovacao_recurso VARCHAR(20) NOT NULL DEFAULT 'NAO_EXIGIDA';

ALTER TABLE tb_reservas
    ADD COLUMN validacao_docente VARCHAR(20) NOT NULL DEFAULT 'NAO_EXIGIDA';

ALTER TABLE tb_reservas
    ADD CONSTRAINT ck_reservas_aprovacao_recurso
        CHECK (aprovacao_recurso IN ('NAO_EXIGIDA', 'PENDENTE', 'APROVADA', 'REJEITADA'));

ALTER TABLE tb_reservas
    ADD CONSTRAINT ck_reservas_validacao_docente
        CHECK (validacao_docente IN ('NAO_EXIGIDA', 'PENDENTE', 'APROVADA', 'REJEITADA'));

-- Reservas CRIADA ja existentes passam a seguir a regra nova.
UPDATE tb_reservas r
   SET aprovacao_recurso = 'PENDENTE'
 WHERE r.status = 'CRIADA'
   AND EXISTS (SELECT 1
                 FROM tb_reservas_materiais rm
                 JOIN tb_materiais m ON m.id = rm.material_id
                WHERE rm.reserva_id = r.id
                  AND m.restrito);

UPDATE tb_reservas
   SET validacao_docente = 'PENDENTE'
 WHERE status = 'CRIADA'
   AND professor_id IS NOT NULL;

UPDATE tb_reservas
   SET status = 'APROVADA'
 WHERE status = 'CRIADA'
   AND aprovacao_recurso = 'NAO_EXIGIDA'
   AND validacao_docente = 'NAO_EXIGIDA';

-- Fila de aprovacao: reservas CRIADA ordenadas pela data da solicitacao.
CREATE INDEX idx_reservas_fila_aprovacao
    ON tb_reservas (criado_em)
    WHERE status = 'CRIADA';
