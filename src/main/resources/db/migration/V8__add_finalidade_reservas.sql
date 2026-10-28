-- RSV-02 (#24): adiciona finalidade obrigatoria a reservas.
-- A coluna fica NOT NULL com DEFAULT '' durante o ALTER para suportar linhas pre-existentes,
-- depois o DEFAULT eh removido para que novas reservas sempre informem a finalidade explicitamente.

ALTER TABLE tb_reservas
    ADD COLUMN finalidade VARCHAR(500) NOT NULL DEFAULT '';

ALTER TABLE tb_reservas
    ALTER COLUMN finalidade DROP DEFAULT;

ALTER TABLE tb_reservas
    ADD CONSTRAINT ck_reservas_finalidade CHECK (finalidade ~ '[^[:space:]]');