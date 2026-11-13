# Roteiro de apresentação — ENT-04

## 1. Objetivo

Consolidar a qualidade, a documentação e as evidências de entrega do projeto Gerenciador de Salas.

## 2. Qualidade automatizada

- Maven Verify executado com sucesso.
- ZAP Baseline, Dependency Review, Trivy e CodeQL aprovados.
- Cobertura JaCoCo:
  - Linhas: 93,90%
  - Branches: 82,88%
- BDD executável: criação de reserva validada com PostgreSQL 16.16 via Testcontainers; 9 testes, sem falhas, erros ou ignorados.
- SonarCloud: Quality Gate aprovado historicamente na candidata `98e59a55` da PR #221. O Quality Gate da `main` deve ser conferido após o merge.

## 3. Segurança do pipeline

O workflow foi ajustado para não expor o `SONAR_TOKEN` durante a execução de pull requests.

- Em pull requests, é executado apenas o Maven Verify.
- A análise autenticada do SonarCloud ocorre somente após push na branch `main` ou disparo manual nessa branch.
- O workflow usa `bash ./mvnw`, evitando falha por permissão de execução do Maven Wrapper.

## 4. Teste de carga

O JMeter foi executado em ambiente local autorizado, utilizando dados sintéticos.

### Baseline

| Métrica | Resultado |
|---|---:|
| Amostras totais | 12.742 |
| Erros | 0 (0,00%) |
| Média | 4,56 ms |
| Mediana | 4 ms |
| p95 | 6 ms |
| p99 | 7 ms |
| Máximo | 70 ms |
| Throughput | 212,54 requisições/s |

### Disputa de reserva

O plano JMeter contém o cenário de duas requisições simultâneas para a mesma sala e horário. Entretanto, o JTL versionado nesta entrega contém somente o baseline de um usuário.

Portanto, a disputa permanece **pendente de evidência** até que seja anexado um JTL/relatório com as duas requisições de reserva, seus códigos de resposta e o resultado esperado de uma reserva aceita e uma recusada por conflito.

## 5. Documentação entregue

- Checklist ENT-04 atualizado.
- Procedimento operacional do SonarCloud.
- Evidências de cobertura JaCoCo.
- Evidência de CI vermelho/verde.
- Plano, JTL e relatório do baseline JMeter.
- Evidência BDD de criação de reserva.
- Validação em clone limpo.
- Decisão de qualidade da ENT-04.
- Roteiro de apresentação.

## 6. Riscos residuais

- TDD histórico não é alegado; há evidência BDD executável.
- A disputa concorrente de reserva no JMeter aguarda JTL específico.
- Casos adicionais de relógio, offset e fuso permanecem como ampliação de cobertura, sem invalidar a execução atual com relógio fixo.
- Após o merge, falta conferir a análise autenticada e o Quality Gate da `main`.

## 7. Decisão

A entrega está tecnicamente apta para merge após os checks e as aprovações exigidas pelo ruleset.

A Issue #186 permanece aberta após o merge até que seja registrada a análise do SonarCloud na `main` e aceita formalmente a pendência da disputa de reserva no JMeter.