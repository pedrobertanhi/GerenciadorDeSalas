# Evidência de validação — Issue #16 (RES-02)

## Escopo confirmado
Issue: https://github.com/pedrobertanhi/GerenciadorDeSalas/issues/16
Pull Request (rascunho): https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/142

- Entidade Professor e migration V3
- Competências e filtro
- CRUD REST e telas Thymeleaf
- Validação de e-mail, estado ativo, duplicidade e dados inválidos
- Autorização por perfil e CSRF

## Execução informada pelo responsável
- Data: 21/10/2026, 21:11 (UTC-03:00)
- Branch: `feature/16-professores-competencias`
- Commit de referência após inclusão dos testes: `a689306cbc3d8040643fd28ff9119d91b15a66ce`
- Ambiente: Windows, Java 21, Maven Wrapper, PostgreSQL 16
- Comando: `.\mvnw.cmd -B verify`
- Resultado do terminal: **97 testes, 0 falhas, 0 erros, 0 ignorados, BUILD SUCCESS**
- Tempo total: 27.354 s
- Evidência fornecida: saída textual do terminal na conversa; **não substitui link de pipeline/artifact do mesmo commit**, requerido pela RTM.

## Testes localizáveis
- `src/test/java/br/com/senac/gerenciadordesalas/professor/`: testes de domínio, persistência, serviço, API, autorização e interface web
- `src/test/java/br/com/senac/gerenciadordesalas/seguranca/LoginWebControllerTest.java`: login web e contrato JSON da API

## Distinção de escopo
A Issue #16 não solicita expressamente matrícula institucional ou eventos de auditoria.
Entretanto, `docs/arquitetura/MODELO_DE_DOMINIO.md` prevê matrícula institucional única para Professor, e `docs/seguranca/MATRIZ_DE_PERMISSOES.md` prevê matrícula válida e auditoria em operações administrativas.
**Pendente de decisão da equipe/revisores:** incorporar esses requisitos nesta Issue ou abrir tarefas específicas, antes do merge.

## Pendências de aceite
- Confirmar evidência navegável de CI para o commit final do PR, conforme a RTM.
- Revisar manualmente o fluxo completo de cadastro, edição, filtro e desativação, ou complementar com teste automatizado de ponta a ponta.
- Obter revisão e aprovações exigidas antes do merge; não usar squash.
