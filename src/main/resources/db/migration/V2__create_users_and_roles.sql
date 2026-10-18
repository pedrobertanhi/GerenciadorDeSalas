CREATE TABLE tb_usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_email_minusculo CHECK (email = LOWER(email)),
    CONSTRAINT ck_usuarios_email_sem_espacos CHECK (email ~ '^[^[:space:]]+$'),
    CONSTRAINT ck_usuarios_nome_preenchido CHECK (nome ~ '[^[:space:]]'),
    CONSTRAINT ck_usuarios_senha_hash_preenchida CHECK (senha_hash ~ '[^[:space:]]'),
    CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR'))
);