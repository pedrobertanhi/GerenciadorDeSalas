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
| RF-01 | acesso indevido horizontal/vertical | CT-AUTH-01 + matriz de perfis; autorização das telas (SalaWebControllerTest, MaintenanceBlockWebControllerTest) | #11–#14, #110 | PENDENTE | planejado |
| RF-02 | cadastro inválido/duplicado | CRUD positivos, negativos e constraints (SalaIntegracaoTest, ProfessorIntegracaoTest, MaterialIntegracaoTest, ConstraintsDoBancoTest, ProvaDeIsolamentoTest) | #15–#18 | PENDENTE | planejado |
| RF-03 | disponibilidade incorreta | filtros combinados e limites (SalaPesquisaIntegracaoTest, PesquisaRecursosWebControllerTest) | #19, #20 | PENDENTE | planejado |
| RF-04 | reserva em estado inválido | CT-TEMPO-01, estados, propriedade, alteração, cancelamento e manutenção (`StatusReservaTest`, `ReservationTest`, `ReservationManagementServiceTest`, `ReservationServiceTest`, `ReservationCriacaoIntegrationTest`) | #23–#27, #87 | Automatizado | PR #25 |
| RF-05 | sobreposição não detectada | CT-TEMPO-02, CT-PROF-01, CT-MAT-01 (`ReservationConflitosIntegrationTest`: 11 intervalos × sala, professor e material, estados, alteração, 409 e quantidades; `ReservationTimePolicyTest`) | #27 (consolida #28–#31) | CI run 42523486135 · commit 8e13af9 · `./mvnw -B verify` · Java 21/PostgreSQL container · 655 testes, 0 falhas | PR #173 |
| RF-06 | duas reservas aceitas | CT-CONC-01 (`ReservationConcorrenciaIntegrationTest`: mesma sala, mesmo professor, mesmo patrimônio e recurso inativado durante a espera; `ReservationServiceTest` e `ReservationManagementServiceTest` verificam a ordem das travas) | #27 (consolida #32, #33); carga em #54, #106 | CI run 42525991845 · commit 81f325e · `./mvnw -B verify` · Java 21/PostgreSQL container · 0 falhas | PR #174 |
| RF-07 | restrito aprovado por perfil errado | CT-APR-01 (`AprovacaoServiceTest`, `AprovacaoFilaIntegrationTest`, `AprovacaoDecisaoIntegrationTest`, `AprovacaoDocenteIntegrationTest`, `AprovacaoWebIntegrationTest`, `AprovacoesNavegadorTest`) | #34 (consolida #35, #61, #69), #105 | CI run 42711607822 · commit 98c86e5 · `./mvnw -B verify` · Java 21/PostgreSQL container · 0 falhas | PR #176, #177, #178, #180 |
| RF-08 | reserva durante manutenção | CT-MNT-01 | #21, #22, #105 | PENDENTE | planejado |
| RF-09 | saldo/movimentação incorretos | retirada, parcial, total e atraso | #36–#38 | PENDENTE | planejado |
| RF-10 | histórico incompleto/adulterável | CT-AUD-01 | #39, #40, #88 | PENDENTE | planejado |
| RF-11 | falha externa derruba/vaza | CT-INT-01 e matriz WireMock | #41, #42, #104 | PENDENTE | planejado |
| RF-12 | relatório inconsistente | consultas, período e agregações | #43, #44 | PENDENTE | planejado |
| RF-13 | fluxo inacessível/erro inseguro | E2E responsivo + assertions de ausência (SalaWebControllerTest, MaintenanceBlockWebControllerTest) | #9, #45–#47, #62–#75, #110 | PENDENTE | planejado |
| RF-14 | contrato desatualizado | geração/validação OpenAPI e clone limpo (OpenApiDocumentoTest) | #10, #80 | PENDENTE | planejado |

## Regras críticas

| Regra | Risco/ATAM | Caso | Automação | Execução | Status |
|---|---|---|---|---|---|
| RC-01 término > início | tempo inválido | CT-TEMPO-01 | `ReservationTimePolicyTest`, `ReservationCriacaoIntegrationTest` (400 para período invertido ou zerado) | CI run 42523486135 · commit 8e13af9 · `./mvnw -B verify` · Java 21/PostgreSQL container · 655 testes, 0 falhas | automatizado |
| RC-02 adjacência permitida | falso conflito | CT-TEMPO-02 | `ReservationConflitosIntegrationTest` (adjacente antes e depois, para sala, professor e material), `ReservationTimePolicyTest` | CI run 42523486135 · commit 8e13af9 · `./mvnw -B verify` · Java 21/PostgreSQL container · 655 testes, 0 falhas | automatizado |
| RC-03 sala sem sobreposição | conflito de sala | CT-CONC-01 | `ReservationConflitosIntegrationTest` (SALA), `ReservationConcorrenciaIntegrationTest` (mesma sala) | CI run 42525991845 · commit 81f325e · `./mvnw -B verify` · Java 21/PostgreSQL container · 0 falhas | automatizado |
| RC-04 professor sem sobreposição | conflito docente | CT-PROF-01 | `ReservationConflitosIntegrationTest` (PROFESSOR, em outra sala), `ReservationConcorrenciaIntegrationTest` (mesmo professor) | CI run 42525991845 · commit 81f325e · `./mvnw -B verify` · Java 21/PostgreSQL container · 0 falhas | automatizado |
| RC-05 material dentro do saldo | quantidade negativa | CT-MAT-01 | `ReservationConflitosIntegrationTest` (MATERIAL e quantidades), `ReservationServiceTest`, `ReservationConcorrenciaIntegrationTest` (mesmo patrimônio) | CI run 42525991845 · commit 81f325e · `./mvnw -B verify` · Java 21/PostgreSQL container · 0 falhas | automatizado |
| RC-06 uma aceitação concorrente | QA-CON-01 | CT-CONC-01 | `ReservationConcorrenciaIntegrationTest` | CI run 42525991845 · commit 81f325e · `./mvnw -B verify` · Java 21/PostgreSQL container · 0 falhas | automatizado |
| RC-07 manutenção bloqueia | recurso indisponível | CT-MNT-01 | PENDENTE | PENDENTE | planejado |
| RC-08 fluxo principal | estado inválido | CT-STATE-01 | PENDENTE | PENDENTE | planejado |
| RC-09 estados alternativos | transição perdida | CT-STATE-01 | PENDENTE | PENDENTE | planejado |
| RC-10 somente Responsável aprova | QA-SEC-01 | CT-APR-01 | `AprovacaoServiceTest`, `AprovacaoFilaIntegrationTest`, `AprovacaoDecisaoIntegrationTest`, `AprovacaoDocenteIntegrationTest`, `AprovacaoWebIntegrationTest`, `AprovacoesNavegadorTest` (403 para SOLICITANTE e ADMINISTRADOR na API e na tela, AccessDenied no serviço) | CI run 42711607822 · commit 98c86e5 · `./mvnw -B verify` · Java 21/PostgreSQL container · 0 falhas | automatizado |
| RC-11 iniciada não é apagada | perda de histórico | CT-DEL-01 | PENDENTE | PENDENTE | planejado |
| RC-12 transição gera auditoria | QA-AUD-01 | CT-AUD-01 | PENDENTE | PENDENTE | planejado |

## Requisitos não funcionais

| RNF | Risco | Verificação | Issues | Execução |
|---|---|---|---|---|
| RNF-QUAL-01 80/70 | cobertura sem oráculo | JaCoCo + revisão de assertions/sanidade | #53, #89, #102 | PENDENTE |
| RNF-TRACE-01 100% críticas | órfão/fantasma/vencido | auditoria navegável | #55, #107 | PENDENTE |
| RNF-SEC-01 zero crítico | falha/vulnerabilidade aberta | gates segurança e parecer | #92–#98, #110 | PENDENTE |
| RNF-CI-01 merge protegido | CI decorativa | vermelho→verde + branch protection | #52, #103 | PENDENTE |
| RNF-CONC-01 integridade sob disputa | dupla reserva | Testcontainers + JMeter | #27 (Testcontainers: `ReservationConcorrenciaIntegrationTest`, PR #174), #54, #106 (JMeter) | Testcontainers: CI run 42525991845 · commit 81f325e · `./mvnw -B verify` · Java 21/PostgreSQL container · 0 falhas; JMeter: PENDENTE |
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

## BASE-04 — Flyway e esquema inicial

| ID | Requisito | Verificação | Status |
|---|---|---|---|
| DB-01 | Banco PostgreSQL vazio recebe o esquema automaticamente | FlywayMigrationIntegrationTest | Automatizado |
| DB-02 | Migration V1 cria a tabela inicial sem ação manual | FlywayMigrationIntegrationTest | Automatizado |
| DB-03 | Histórico registra versão, checksum e sucesso | flyway_schema_history + FlywayMigrationIntegrationTest | Automatizado |
| DB-04 | Novas migrations seguem convenção versionada e imutável | docs/FLYWAY.md | Documentado |

## SEC-01 — Usuários, perfis e persistência

| ID | Requisito | Verificação | Status |
|---|---|---|---|
| USR-01 | Usuário é persistido com perfil válido (SOLICITANTE, RESPONSAVEL, ADMINISTRADOR) | UserRepositoryIntegrationTest | Automatizado |
| USR-02 | E-mail duplicado é recusado, sem diferenciar maiúsculas de minúsculas | UserRepositoryIntegrationTest + UserTest | Automatizado |
| USR-03 | Perfil inválido é recusado pelo banco e pela validação | UserRepositoryIntegrationTest + UserTest | Automatizado |
| USR-04 | Usuário inativo permanece no histórico | UserRepositoryIntegrationTest + UserTest | Automatizado |
| USR-05 | Dados de demonstração são sintéticos e carregados só no perfil dev | R__seed_demo_users.sql + application-dev.yml | Documentado |
| USR-06 | Banco recusa e-mail com espaços e nome ou hash de senha em branco | UserRepositoryIntegrationTest | Automatizado |
| USR-07 | Usuário não pode ser excluído fisicamente pelo repositório | UserRepositoryContractTest | Automatizado |

## SEC-02 — Autenticação e armazenamento seguro de senha

| ID | Requisito | Verificação | Status |
|---|---|---|---|
| AUT-01 | Credencial válida autentica e mantém sessão no servidor | AutenticacaoWebTest | Automatizado |
| AUT-02 | Senha é armazenada somente como hash BCrypt | SenhaCodificadaTest | Automatizado |
| AUT-03 | Senha inválida e e-mail inexistente recebem a mesma resposta 401 | AutenticacaoWebTest | Automatizado |
| AUT-04 | Usuário inativo é recusado com resposta idêntica à de senha inválida | AutenticacaoWebTest + UsuarioDetailsServiceTest | Automatizado |
| AUT-05 | Logout invalida a sessão | AutenticacaoWebTest | Automatizado |
| AUT-06 | 401 e 403 do Spring Security usam o contrato padronizado com correlationId | RespostasDeSegurancaTest + AutenticacaoWebTest | Automatizado |
| AUT-07 | Login sem token CSRF é recusado | AutenticacaoWebTest | Automatizado |
| AUT-08 | Hash das contas de demonstração corresponde à senha documentada; contas só no perfil dev | SenhaCodificadaTest + PLANO_DE_SEGURANCA.md | Automatizado e documentado |

## SEC-03 — Autorização por perfil e propriedade

| ID | Requisito | Verificação | Status |
|---|---|---|---|
| AUTZ-01 | Rotas sem autenticação são recusadas com resposta 401 padronizada | AutorizacaoPorPerfilWebTest | Automatizado |
| AUTZ-02 | Solicitante cria reservas e acessa somente recursos próprios | AutorizacaoPorPerfilWebTest + AutorizacaoDePropriedade | Automatizado |
| AUTZ-03 | Somente Responsável executa aprovação e movimentações operacionais | AutorizacaoPorPerfilWebTest | Automatizado |
| AUTZ-04 | Somente Administrador executa operações administrativas | AutorizacaoPorPerfilWebTest | Automatizado |
| AUTZ-05 | Administrador não herda automaticamente operações do Responsável | AutorizacaoPorPerfilWebTest | Automatizado |
| AUTZ-06 | Acesso negado retorna 403 padronizado e não executa efeito colateral | AutorizacaoPorPerfilWebTest | Automatizado |
| AUTZ-07 | Exclusões físicas de usuários, reservas, movimentações e auditoria são negadas | SecurityConfig + AutorizacaoPorPerfilWebTest | Automatizado |
| AUTZ-08 | Rotas não mapeadas são negadas inclusive para usuário autenticado | SecurityConfig + AutorizacaoPorPerfilWebTest | Automatizado |
| AUTZ-09 | Alteração e cancelamento de reserva ficam negados até a propriedade persistida ser consultável | SecurityConfig + AutorizacaoPorPerfilWebTest | Automatizado |
| AUTZ-10 | Auditoria permite consulta administrativa e proíbe criação, alteração ou exclusão manual | SecurityConfig + AutorizacaoPorPerfilWebTest | Automatizado |
| AUTZ-11 | Solicitante consulta movimentações somente quando identificado como dono | AutorizacaoPorPerfilWebTest + AutorizacaoDePropriedade | Automatizado |

## Correção de segurança no login — Limitação de tentativas

| ID | Requisito | Verificação | Status |
|---|---|---|---|
| RATE-01 | Cinco falhas por conta ou origem ativam bloqueio temporário | LimitadorTentativasLoginTest + LimiteTentativasLoginWebTest | Automatizado |
| RATE-02 | Novas falhas aumentam a espera sem exceder quinze minutos | LimitadorTentativasLoginTest | Automatizado |
| RATE-03 | Estado expira em quinze minutos e não causa bloqueio permanente | LimitadorTentativasLoginTest | Automatizado |
| RATE-04 | Conta existente e inexistente mantêm resposta neutra | LimiteTentativasLoginWebTest | Automatizado |
| RATE-05 | Resposta e registro não expõem e-mail, senha ou origem em texto puro | LimiteTentativasLoginWebTest + FiltroLimiteTentativasLogin | Automatizado |
| RATE-06 | Login válido limpa a contagem e preserva o fluxo legítimo | LimitadorTentativasLoginTest + LimiteTentativasLoginWebTest | Automatizado |

## MNT-01 — Bloqueios de manutenção

| ID | Requisito | Verificação | Status |
|---|---|---|---|
| MNT-01 | Somente administrador gerencia bloqueios, e todos os perfis consultam | MaintenanceBlockControllerSecurityTest + BloqueioManutencaoIntegracaoTest | Automatizado |
| MNT-02 | Período inválido é recusado: fim após o início e ainda no futuro | BloqueioDtoValidationTest + MaintenanceBlockTest + ConstraintsBloqueiosManutencaoTest | Automatizado |
| MNT-03 | Recurso inexistente é recusado | MaintenanceBlockServiceTest + BloqueioManutencaoIntegracaoTest | Automatizado |
| MNT-04 | Histórico preservado: sem exclusão, e concluídos e cancelados permanecem | MaintenanceBlockRepositoryContractTest + BloqueioManutencaoIntegracaoTest | Automatizado |
| MNT-05 | Alterar e cancelar só em bloqueio ativo, e concluir só se já começou | MaintenanceBlockTest + MaintenanceBlockServiceTest + BloqueioManutencaoIntegracaoTest | Automatizado |
| MNT-06 | Consulta por tipo, recurso e estado, do mais recente ao mais antigo | BloqueioManutencaoIntegracaoTest | Automatizado |
| MNT-07 | Constraints do banco barram bloqueio inválido | ConstraintsBloqueiosManutencaoTest | Automatizado |

## ENT-02 — Administração de usuários

O [Maven Verify #42734185387](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42734185387) passou na `main` para o SHA candidato `8385c6aab6e7b14de964aaf90317f917f9b99397`, com 830 testes e zero falhas, erros ou testes ignorados.

| Critério | Implementação e testes | Evidência |
|---|---|---|
| Administração restrita a ADMINISTRADOR | `SecurityConfig`, `UsuarioController`, `UsuarioControllerSecurityTest` | Suite aprovada no Maven Verify #42734185387 |
| Cadastro validado, e-mail único e senha com hash | `UsuarioDto`, `UsuarioAdminServiceTest`, `UsuarioAdminIntegracaoTest` | Suite aprovada no Maven Verify #42734185387 |
| Inativação sem exclusão e proteção do último administrador | `UsuarioAdminService`, `UsuarioAdminIntegracaoTest` | Suite aprovada no Maven Verify #42734185387 |
| Sessão de conta inativada bloqueada | `FiltroUsuarioAtivoTest`, `UsuarioInativoSessaoWebTest`, `UsuarioAdminIntegracaoTest` | Suite aprovada no Maven Verify #42734185387 |
| Registro das alterações administrativas | `UsuarioAdminService` e `AuditService`, `UsuarioAdminIntegracaoTest` | Suite aprovada no Maven Verify #42734185387 |
| Controles e evidências da release | [Parecer de segurança](seguranca/PARECER_DE_SEGURANCA.md) | Relatórios de segurança pendentes para o SHA candidato |

### ENT-02 — Evidências complementares

| Critério | Rastreabilidade | Situação |
|---|---|---|
| API administrativa, revogação de sessão e auditoria | [PR #188](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/188) e [Maven Verify do PR](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42729955054) | CI verde no PR da API |
| Interface administrativa e autorização | [PR #189](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/189), `UsuarioWebControllerTest` | Suite aprovada no Maven Verify #42734185387. [Cinco testes de navegador aprovados](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42735598179) no SHA `e62e86cc770bed2c08832da8f4eaf15fff08ce9f`; evidência do commit final da release pendente |
| Política HTTP e por perfil, propriedade de reservas e respostas seguras | [Matriz de segurança das rotas](seguranca/MATRIZ_DE_ROTAS.md) | Inventário e testes existentes mapeados; lacunas registradas |
| CI e controles de segurança no mesmo commit ou tag | [Parecer de segurança](seguranca/PARECER_DE_SEGURANCA.md) | Maven Verify confirmado para o SHA candidato. Relatórios de segurança e revisão pendentes |

### ENT-02 — Candidata após o PR #193

SHA `59fa404c2981955143032a8bf876665a9138e681`. [Maven Verify](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088444): Em andamento na conferência. [CodeQL](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088702): Aprovado. [Trivy filesystem](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088435): Aprovado. O [parecer de segurança](seguranca/PARECER_DE_SEGURANCA.md) distingue os checks do merge, a revisão de dependências do PR e as evidências ainda pendentes.
