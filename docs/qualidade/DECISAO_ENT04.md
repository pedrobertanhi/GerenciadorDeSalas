# Registro de evidências e decisão de qualidade — ENT-04 (#186)

Este documento não substitui execuções reais nem autoriza fechar a issue antes de todas as evidências.

## Proveniência já comprovada

| Entrega | Evidência | Estado |
|---|---|---|
| CI Maven, relatórios e JaCoCo | [PR #215](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/215) | Integrado |
| Cálculo LINE/BRANCH e resumo rastreável | [PR #216](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/216) | Integrado |
| Execução de CI com 5 workflows verdes | [Run CI #43044216420](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43044216420) e checks do SHA correspondente | Histórico; repetir na candidata final |
| Cobertura percentual | [Resumo JaCoCo](EVIDENCIAS_COBERTURA_ENT04.md) | A medir e transcrever a partir do run final |
| Gate de cobertura | Não há evidência de check percentual ativo | Pendente |
| SonarCloud | Sem análise e Quality Gate verificados | Pendente |
| JMeter | Sem JTL e relatório HTML verificados | Pendente |
| Falha controlada e recuperação CI | Sem par de runs vermelho/verde vinculado | Pendente |
| Clone limpo, RTM e revisão cruzada final | Sem aceite vinculado ao SHA de release | Pendente |

## Protocolo de evidência por requisito

Para cada requisito da [RTM](../RTM.md), associar ID, teste automatizado ou procedimento manual, URL da execução, SHA, data, responsável, resultado e eventual defeito. A ausência de um desses dados deve ser explicitada como lacuna, não como sucesso.

Para carga: documentar autorização do ambiente, configuração de usuários/rampa/duração, massa sintética, JMX, JTL, HTML, p95, p99, throughput e taxa de erros. Para SonarCloud: organização, project key, análise no mesmo SHA, Quality Gate e autorização do token em secret.

## Critérios de decisão

- **Aprovado:** todos os requisitos obrigatórios têm evidência reproduzível no mesmo SHA/tag, gates exigidos passam, revisores aprovam e riscos residuais são aceitos nominalmente.
- **Bloqueado:** faltam percentuais/gates, SonarCloud, JMeter, rastreabilidade, teste controlado vermelho/verde ou aprovação.
- **Reprovado:** teste/gate obrigatório falha ou evidência é inconsistente.

**Decisão atual: BLOQUEADO para fechamento da #186.** A CI verde das PRs incrementais não comprova os demais requisitos. Atualizar esta decisão apenas após conferir os artefatos finais.
