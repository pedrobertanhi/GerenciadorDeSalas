# Decisão de qualidade — ENT-04 (#186)

- Data: 13/11/2026
- PR: #221
- Situação: **APTA TECNICAMENTE, BLOQUEADA PARA MERGE**

## Evidências aceitas

- Maven Verify, ZAP, Dependency Review, Trivy e CodeQL passaram no SHA atual.
- Cobertura: 93,90% de linhas e 82,88% de branches.
- O SonarCloud teve Quality Gate aprovado na candidata `98e59a55` desta PR. No workflow atual, a análise autenticada ocorre somente depois da integração na `main`, para não expor o token ao código de uma PR.
- JMeter executado em ambiente local autorizado, com dados sintéticos: baseline de 12.742 amostras HTTP 200, 0% de erros, p95 de 6 ms e 212,54 requisições/s. O JTL anexado não contém a execução do cenário de disputa; esse resultado não é aceito como evidência até haver JTL próprio.

## Riscos residuais

- TDD/BDD e cobertura API/E2E ainda estão consolidados parcialmente na RTM.
- Reprodutibilidade de testes temporais por relógio/fuso não foi auditada integralmente.
- O baseline JMeter é local e com um usuário; não permite inferir capacidade de produção.

## Decisão

A implementação pode seguir para revisão, mas **não deve ser mesclada** até que:

1. a validação de clone limpo seja registrada;
2. as conversas de revisão aplicáveis sejam resolvidas;
3. o ruleset receba duas aprovações humanas.

Após esses bloqueios externos, a decisão recomendada é **APROVAR O MERGE**.