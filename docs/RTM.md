# Matriz de Rastreabilidade de Requisitos (RTM)

> Status: planejamento. “Issue aberta” não é evidência. A coluna de execução permanece **PENDENTE** até apontar para pipeline/artifact do mesmo commit ou tag.

## Regras da matriz

1. IDs são iguais no PRD, riscos, casos, testes e relatório.
2. Evidência contém automação localizável, comando, ambiente, commit/tag e resultado.
3. Mudança de requisito revisa os casos e invalida execução anterior.
4. Status deriva da execução.
5. #107 audita requisito órfão, teste fantasma, evidência vencida e link quebrado.

## Requisitos funcionais

| Requisito | Risco principal | Casos/automação planejados | Issues | Execução | Status |
|---|---|---|---|---|---|
| RF-01 | acesso indevido horizontal/vertical | CT-AUTH-01 + matriz de perfis | #11–#14, #110 | PENDENTE | planejado |
| RF-02 | cadastro inválido/duplicado | CRUD positivos, negativos e constraints | #15–#18 | PENDENTE | planejado |
| RF-03 | disponibilidade incorreta | filtros combinados e limites | #19, #20 | PENDENTE | planejado |
| RF-04 | reserva em estado inválido | CT-TEMPO-01, estados e propriedade | #23–#27, #87 | PENDENTE | planejado |
| RF-05 | sobreposição não detectada | CT-TEMPO-02, CT-PROF-01, CT-MAT-01 | #28–#31 | PENDENTE | planejado |
| RF-06 | duas reservas aceitas | CT-CONC-01 | #32, #33, #54, #106 | PENDENTE | planejado |
| RF-07 | restrito aprovado por perfil errado | CT-APR-01 | #34, #35, #105 | PENDENTE | planejado |
| RF-08 | reserva durante manutenção | CT-MNT-01 | #21, #22, #105 | PENDENTE | planejado |
| RF-09 | saldo/movimentação incorretos | retirada, parcial, total e atraso | #36–#38 | PENDENTE | planejado |
| RF-10 | histórico incompleto/adulterável | CT-AUD-01 | #39, #40, #88 | PENDENTE | planejado |
| RF-11 | falha externa derruba/vaza | CT-INT-01 e matriz WireMock | #41, #42, #104 | PENDENTE | planejado |
| RF-12 | relatório inconsistente | consultas, período e agregações | #43, #44 | PENDENTE | planejado |
| RF-13 | fluxo inacessível/erro inseguro | E2E responsivo + assertions de ausência | #9, #45–#47, #62–#75, #110 | PENDENTE | planejado |
| RF-14 | contrato desatualizado | geração/validação OpenAPI e clone limpo | #10, #80 | PENDENTE | planejado |

## Regras críticas

| Regra | Risco/ATAM | Caso | Automação | Execução | Status |
|---|---|---|---|---|---|
| RC-01 término > início | tempo inválido | CT-TEMPO-01 | PENDENTE | PENDENTE | planejado |
| RC-02 adjacência permitida | falso conflito | CT-TEMPO-02 | PENDENTE | PENDENTE | planejado |
| RC-03 sala sem sobreposição | conflito de sala | CT-CONC-01 | PENDENTE | PENDENTE | planejado |
| RC-04 professor sem sobreposição | conflito docente | CT-PROF-01 | PENDENTE | PENDENTE | planejado |
| RC-05 material dentro do saldo | quantidade negativa | CT-MAT-01 | PENDENTE | PENDENTE | planejado |
| RC-06 uma aceitação concorrente | QA-CON-01 | CT-CONC-01 | PENDENTE | PENDENTE | planejado |
| RC-07 manutenção bloqueia | recurso indisponível | CT-MNT-01 | PENDENTE | PENDENTE | planejado |
| RC-08 fluxo principal | estado inválido | CT-STATE-01 | PENDENTE | PENDENTE | planejado |
| RC-09 estados alternativos | transição perdida | CT-STATE-01 | PENDENTE | PENDENTE | planejado |
| RC-10 somente Responsável aprova | QA-SEC-01 | CT-APR-01 | PENDENTE | PENDENTE | planejado |
| RC-11 iniciada não é apagada | perda de histórico | CT-DEL-01 | PENDENTE | PENDENTE | planejado |
| RC-12 transição gera auditoria | QA-AUD-01 | CT-AUD-01 | PENDENTE | PENDENTE | planejado |

## Requisitos não funcionais

| RNF | Risco | Verificação | Issues | Execução |
|---|---|---|---|---|
| RNF-QUAL-01 80/70 | cobertura sem oráculo | JaCoCo + revisão de assertions/sanidade | #53, #89, #102 | PENDENTE |
| RNF-TRACE-01 100% críticas | órfão/fantasma/vencido | auditoria navegável | #55, #107 | PENDENTE |
| RNF-SEC-01 zero crítico | falha/vulnerabilidade aberta | gates segurança e parecer | #92–#98, #110 | PENDENTE |
| RNF-CI-01 merge protegido | CI decorativa | vermelho→verde + branch protection | #52, #103 | PENDENTE |
| RNF-CONC-01 integridade sob disputa | dupla reserva | Testcontainers + JMeter | #33, #54, #106 | PENDENTE |
| RNF-PERF-01 carga mensurável | lentidão/corrupção | JMX/JTL/HTML + invariante | #54, #106 | PENDENTE |
| RNF-ERR-01 erro neutro | vazamento | API/log/artifact negativos | #9, #104, #110 | PENDENTE |
| RNF-REP-01 clone limpo | entrega não executa | instalação por outro integrante | #80, #109 | PENDENTE |

## Como registrar uma execução

Exemplo de formato, sem inventar resultado:

`CI run URL · commit abc123 · ./mvnw -B verify · Java 21/PostgreSQL container · artifact nome · resultado`.

Captura isolada não fecha a linha. O link deve continuar navegável e corresponder ao escopo entregue.

## BASE-05 — Tratamento seguro de erros

| ID | Requisito | Teste | Status |
|---|---|---|---|
| ERR-01 | Validação de campo retorna 400 com fieldErrors | Erro400ValidacaoTest | Implementado |
| ERR-02 | Recurso inexistente retorna 404 | Erro404NaoEncontradoTest | Implementado |
| ERR-03 | Acesso negado retorna 403 sem vazar detalhe | Erro403NegadoTest | Implementado |
| ERR-04 | Conflito de estado retorna 409 | Erro409ConflitoTest | Implementado |
| ERR-05 | Erro interno retorna 500 sem stack trace | Erro500InternoTest | Implementado |
| ERR-06 | Toda resposta de erro carrega correlationId | CorrelationIdFilter + todos os testes acima | Implementado |
