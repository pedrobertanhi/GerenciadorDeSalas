# Gerenciador de Salas de Aula

Sistema acadêmico em **Java 21 e Spring Boot 4.1.2** para alocar salas, professores, materiais e equipamentos sem conflitos de horário, com autorização, auditoria, concorrência segura e evidências reproduzíveis de qualidade.

> Projeto semestral da disciplina **Qualidade de Software — 2026.2**. Entrega principal na semana 46.

<p align="center">
  <img src="docs/assets/banner-gerenciador-salas.jpg" alt="Banner do Gerenciador de Salas e Recursos" width="100%">
</p>

## Estado atual

O projeto usa Java 21 e Spring Boot 4.1.2. A implementação funcional, segurança e interface evoluíram além da descrição inicial deste documento. A [matriz de rastreabilidade](docs/RTM.md) registra entregas por requisito e ainda contém itens que exigem atualização e evidência.

- A candidata de segurança `1fde5bd` teve [837 testes aprovados](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42920680085); execuções posteriores registradas na RTM alcançaram 898 testes. **Esses resultados não comprovam automaticamente o estado de novos commits.**
- A [decisão de segurança da #184](https://github.com/pedrobertanhi/GerenciadorDeSalas/issues/184) aprovou uma candidata específica para o ambiente acadêmico controlado, com riscos residuais documentados.
- A [entrega de qualidade #186](https://github.com/pedrobertanhi/GerenciadorDeSalas/issues/186) permanece aberta. Consulte o [relatório de testes](docs/testes/RELATORIO_DE_TESTES.md) para evidências e lacunas atuais.
- As PRs #215, #216 e #218 integraram publicação de relatórios Maven, resumo JaCoCo e gate de cobertura (80% LINE / 70% BRANCH). A [CI da PR #217](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43203143550) registrou 93,90% de linhas e 82,88% de branches; são valores históricos, não uma certificação automática da release final.
- A [PR #219](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/219) comprovou falha controlada e recuperação verde da CI, com [evidência rastreável](docs/qualidade/EVIDENCIA_CI_RED_GREEN_ENT04.md).
- SonarCloud, JMeter em ambiente autorizado, rastreabilidade integral e aceite da release ainda dependem de evidências reais. Consulte o [checklist ENT-04](docs/qualidade/CHECKLIST_ENT04.md).

## Produto

### Perfis

- **Solicitante:** professor ou coordenador; consulta disponibilidade e cria, altera ou cancela as próprias reservas.
- **Responsável:** aprova recursos restritos, valida docentes e acompanha retirada/devolução.
- **Administrador:** gerencia usuários, recursos, bloqueios e manutenção.

### Fluxos obrigatórios

- autenticação e autorização por perfil e propriedade;
- cadastros e pesquisa de salas, professores e materiais;
- reservas e agenda docente sem sobreposição;
- uma única aceitação sob solicitações simultâneas;
- aprovação de recursos restritos;
- bloqueio durante manutenção;
- retirada e devolução;
- histórico auditável;
- notificação simulada/externa segura;
- relatórios de utilização, carga docente e conflitos evitados;
- interface responsiva e erros seguros;
- OpenAPI ou fluxos públicos documentados.

A máquina principal é `SOLICITADA → APROVADA → EM_USO → CONCLUIDA`, com `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`. Reserva iniciada não é apagada e toda mudança de estado gera auditoria.

## Stack definida

Java 21 · Spring Boot 4.1.2 · Maven Wrapper · Spring Web · Spring Security · Validation · Spring Data JPA · PostgreSQL · Flyway · Docker Compose · Thymeleaf/Bootstrap ou alternativa registrada em ADR · OpenAPI.

Testes e qualidade: JUnit 5, testes parametrizados, Testcontainers, WireMock, JaCoCo 0.8.15, SonarCloud, GitHub Actions, JMeter, CodeQL, análise de dependências, secret scanning, Trivy e OWASP ZAP.

## Metas e bloqueios

| Controle | Meta/gate |
|---|---|
| Cobertura de linhas | ≥ 80% |
| Cobertura de branches | ≥ 70% |
| Regras críticas na RTM | 100% |
| Bugs/vulnerabilidades críticas | 0 confirmados e abertos |
| Merge | PR revisado + pipeline obrigatório |
| Concorrência | exatamente uma reserva aceita |
| Instalação | clone limpo funciona pelo README |

A release é reprovada se a aplicação não executar, o pipeline estiver ausente/vermelho, houver falha de autorização, dupla reserva ou vulnerabilidade crítica aberta.

## Como executar

Pré-requisitos: **Java 21**, **Docker** e **Docker Compose v2**.

No Linux/macOS:

```bash
docker compose up -d --wait postgres
./mvnw -B verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No Windows:

```powershell
docker compose up -d --wait postgres
.\mvnw.cmd -B verify
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Consulte o [guia do PostgreSQL com Docker Compose](docs/POSTGRESQL_DOCKER.md) para verificar o healthcheck, testar a conexão, consultar logs e recriar o banco.

O Flyway aplica automaticamente as migrations pendentes ao iniciar a aplicação. Consulte o [guia de versionamento do banco](docs/FLYWAY.md) para criar novas migrations, validar o histórico e testar um banco limpo.

## Antes de desenvolver

Leia o [guia operacional de execução](docs/GUIA_DE_EXECUCAO.md). Ele mostra como transformar uma Issue em trabalho comprovado, incluindo branch, microcommits, testes, RTM, evidências, Pull Request, revisão e critérios de Done.

## Fluxo Git obrigatório

1. Issue preparada no Project.
2. Branch curta por tarefa.
3. Alteração concluída em um arquivo → commit próprio.
4. Alterações independentes no mesmo arquivo → commits separados.
5. Código, teste, configuração, documentação e evidência → commits separados.
6. PR pequeno, pipeline e revisão por outra pessoa.
7. Merge sem squash.
8. RTM/evidência atualizadas com o commit realmente executado.

Exemplos:

```text
feat: implementa conflito de sala (#28)
test: cobre horários adjacentes (#31)
docs: liga CT-CONC-01 à execução (#107)
ci: publica relatórios em falha e sucesso (#103)
```

Consulte a [política completa de commits](docs/POLITICA_DE_COMMITS.md).

## Documentação essencial

- [PRD, personas, requisitos e fora de escopo](docs/PRD.md)
- [Planejamento por semana](docs/PLANEJAMENTO.md)
- [Guia de execução de cada tarefa](docs/GUIA_DE_EXECUCAO.md)
- [Backlog completo](docs/BACKLOG.md)
- [Índice de documentação](docs/README.md)
- [ATAM e utility tree](docs/arquitetura/ATAM.md)
- [Plano de testes](docs/testes/PLANO_DE_TESTES.md)
- [Casos reproduzíveis](docs/testes/CASOS_DE_TESTE.md)
- [Técnicas caixa-preta, branca e cinza](docs/testes/TECNICAS_DE_PROJETO.md)
- [RTM](docs/RTM.md)
- [Riscos](docs/RISCOS.md) e [defeitos](docs/DEFEITOS.md)
- [Plano de segurança](docs/seguranca/PLANO_DE_SEGURANCA.md)
- [Plano SQA e gates](docs/qualidade/PLANO_SQA.md)
- [Teste de carga](docs/desempenho/PLANO_TESTE_CARGA.md)
- [Roteiro da demonstração](docs/apresentacao/ROTEIRO_DEMONSTRACAO.md)

## Equipe

Quatro integrantes, ainda não atribuídos. Cada pessoa deverá:

- produzir commits relevantes de Java e testes;
- abrir PRs próprios;
- revisar trabalho de outra pessoa;
- conhecer arquitetura, riscos, CI, segurança, carga e RTM;
- preservar a autoria sem squash.

---

Planejamento acadêmico. Um card ou documento planejado não é evidência de implementação concluída.
