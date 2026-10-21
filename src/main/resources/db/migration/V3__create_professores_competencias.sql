CREATE TABLE tb_professores (
 id BIGSERIAL PRIMARY KEY,
 nome VARCHAR(150) NOT NULL,
 email VARCHAR(254) NOT NULL,
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT uq_professores_email UNIQUE (email),
 CONSTRAINT ck_professores_email_minusculo CHECK (email = LOWER(email)),
 CONSTRAINT ck_professores_nome CHECK (BTRIM(nome) <> ''),
 CONSTRAINT ck_professores_email CHECK (email ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$')
);
CREATE TABLE tb_competencias (
 id BIGSERIAL PRIMARY KEY,
 nome VARCHAR(100) NOT NULL,
 CONSTRAINT uq_competencias_nome UNIQUE (nome),
 CONSTRAINT ck_competencias_nome CHECK (BTRIM(nome) <> '')
);
CREATE TABLE tb_professor_competencias (
 professor_id BIGINT NOT NULL REFERENCES tb_professores(id),
 competencia_id BIGINT NOT NULL REFERENCES tb_competencias(id),
 PRIMARY KEY (professor_id, competencia_id)
);
CREATE INDEX idx_professor_competencias_competencia ON tb_professor_competencias(competencia_id);