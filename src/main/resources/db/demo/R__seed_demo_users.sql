-- Dados de demonstracao SINTETICOS, carregados somente no perfil dev.
-- Senha de todos os usuarios de demonstracao: Demo#Salas2026 (nunca usar fora do ambiente local).
INSERT INTO tb_usuarios (nome, email, senha_hash, perfil, ativo) VALUES
    ('Solicitante Demonstracao', 'solicitante.demo@exemplo.test', '$2a$10$F9foeTOyhSFOjNGmdeoQTO5vPuif3OlpbEkv.ACoDocbDF7vZD7Du', 'SOLICITANTE', TRUE),
    ('Responsavel Demonstracao', 'responsavel.demo@exemplo.test', '$2a$10$F9foeTOyhSFOjNGmdeoQTO5vPuif3OlpbEkv.ACoDocbDF7vZD7Du', 'RESPONSAVEL', TRUE),
    ('Administrador Demonstracao', 'admin.demo@exemplo.test', '$2a$10$F9foeTOyhSFOjNGmdeoQTO5vPuif3OlpbEkv.ACoDocbDF7vZD7Du', 'ADMINISTRADOR', TRUE),
    ('Usuario Inativo Demonstracao', 'inativo.demo@exemplo.test', '$2a$10$F9foeTOyhSFOjNGmdeoQTO5vPuif3OlpbEkv.ACoDocbDF7vZD7Du', 'SOLICITANTE', FALSE)
ON CONFLICT (email) DO NOTHING;