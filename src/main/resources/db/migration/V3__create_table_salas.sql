CREATE TABLE salas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    capacidade INT NOT NULL CHECK (capacidade > 0),
    localizacao VARCHAR(255) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);