-- RSV-02 (#24): adiciona finalidade obrigatoria a reservas.
-- A coluna entra anulavel, as reservas que ja existirem recebem uma finalidade
-- padrao e so entao a coluna vira NOT NULL e ganha o CHECK de nao-branco.
-- Assim a migration funciona com a tabela vazia ou com dados, sem deixar DEFAULT:
-- novas reservas precisam informar a finalidade explicitamente.

ALTER TABLE tb_reservas
    ADD COLUMN finalidade VARCHAR(500);

UPDATE tb_reservas
    SET finalidade = 'Finalidade nao informada'
    WHERE finalidade IS NULL;

ALTER TABLE tb_reservas
    ALTER COLUMN finalidade SET NOT NULL;

ALTER TABLE tb_reservas
    ADD CONSTRAINT ck_reservas_finalidade CHECK (finalidade ~ '[^[:space:]]');
