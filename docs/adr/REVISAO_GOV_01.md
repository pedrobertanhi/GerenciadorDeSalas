# Revisão formal do GOV-01

## Objetivo

Registrar o procedimento de revisão dos requisitos e das decisões arquiteturais antes de alterar os ADRs de `Proposta` para `Aceita`.

## Escopo da revisão

Cada integrante deverá conferir:

- os requisitos funcionais RF01 a RF14 no PRD;
- a ausência de funcionalidades fora do escopo;
- as decisões e consequências dos ADRs 001 a 007;
- as pendências registradas no índice de ADRs;
- a coerência entre arquitetura, segurança, concorrência e notificações.

## Documentos obrigatórios

- [PRD — Requisitos e escopo](../PRD.md);
- [Índice de ADRs e decisões pendentes](README.md);
- [ADR-001 — Stack tecnológica](0001-stack-tecnologica.md);
- [ADR-002 — Interface web e OpenAPI](0002-interface-web-e-openapi.md);
- [ADR-003 — Tempo e sobreposição](0003-tempo-e-sobreposicao.md);
- [ADR-004 — Arquitetura em camadas](0004-arquitetura-em-camadas.md);
- [ADR-005 — Autenticação e autorização](0005-autenticacao-e-autorizacao.md);
- [ADR-006 — Controle concorrente](0006-controle-concorrente-de-reservas.md);
- [ADR-007 — Estratégia de notificações](0007-estrategia-de-notificacoes.md).

## Evidência válida

Os quatro integrantes deverão conferir este protocolo e os documentos obrigatórios com suas próprias contas.

O autor do Pull Request evidenciará sua concordância por meio dos commits assinados pela própria autoria e de um comentário explícito no PR informando o escopo e os ADRs verificados. O GitHub não permite que o autor aprove o próprio Pull Request.

Cada um dos outros três integrantes deverá:

1. abrir a aba **Files changed**;
2. conferir este protocolo e os documentos vinculados;
3. selecionar **Review changes**;
4. registrar uma observação sobre o que foi verificado;
5. selecionar **Approve**;
6. enviar a revisão.

A evidência dos quatro integrantes será, portanto, composta pela declaração explícita do autor e por três revisões formais com estado `APPROVED`. Comentários isolados dos revisores e checkboxes marcados não substituem essas aprovações.

## Regra de estado

Os ADRs permanecerão como `Proposta` durante esta revisão.

Somente depois da evidência dos quatro integrantes será criado um Pull Request separado. No PR final, o autor deverá registrar novamente sua concordância explícita e Enzo, Matheus e Davis deverão enviar novas revisões formais com estado `APPROVED` antes de alterar os sete ADRs e o índice para `Aceita`. Esse PR também deverá preservar os commits e não utilizar squash.
