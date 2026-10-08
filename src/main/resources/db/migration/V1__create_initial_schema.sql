CREATE TABLE tb_salas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    capacidade INTEGER NOT NULL,
    localizacao VARCHAR(150)
);
