# Roteiro de apresentação — ENT-04

## 1. Objetivo

Consolidar a qualidade, a documentação e as evidências de entrega do projeto Gerenciador de Salas.

## 2. Qualidade automatizada

- Maven Verify executado com sucesso.
- ZAP Baseline, Dependency Review, Trivy e CodeQL aprovados.
- Cobertura JaCoCo:
  - Linhas: 93,90%
  - Branches: 82,88%
- SonarCloud: Quality Gate aprovado na PR #221.

## 3. Segurança do pipeline

O workflow foi ajustado para não expor o `SONAR_TOKEN` durante a execução de pull requests.

- Em pull requests, é executado apenas o Maven Verify.
- A análise autenticada do SonarCloud ocorre somente após push na branch `main`.
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

Foram executadas duas requisições simultâneas para a mesma sala e horário.

- Uma reserva foi aprovada.
- A segunda requisição recebeu conflito de estado.
- O resultado confirma que a aplicação impede dupla reserva para o mesmo recurso e período.

## 5. Documentação entregue

- Checklist ENT-04 atualizado.
- Procedimento operacional do SonarCloud.
- Evidências de cobertura JaCoCo.
- Evidência de CI vermelho/verde.
- Resultado real do JMeter.
- Decisão de qualidade da ENT-04.
- Roteiro de apresentação.

## 6. Riscos residuais

- A rastreabilidade de TDD/BDD e API/E2E permanece parcial na RTM.
- Testes envolvendo relógio e fuso horário ainda não foram auditados integralmente.
- A validação de clone limpo deve ser registrada antes do merge final.

## 7. Decisão

A entrega está tecnicamente apta:

- Todos os checks automatizados atuais passaram.
- A cobertura está acima dos limites definidos.
- O Quality Gate do SonarCloud foi aprovado.
- O teste de carga foi executado com sucesso e sem erros.

O merge permanece condicionado apenas às exigências externas do ruleset da branch, como conversas resolvidas e aprovações humanas.
