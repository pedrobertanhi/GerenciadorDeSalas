# Registro de evidências e decisão de qualidade — ENT-04 (#186)

Este documento não substitui execuções reais nem autoriza fechar a issue antes de todas as evidências.

## Proveniência já comprovada

| Entrega | Evidência | Estado |
|---|---|---|
| CI Maven, relatórios e JaCoCo | [PR #215](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/215) | Integrado |
| Cálculo LINE/BRANCH e resumo rastreável | [PR #216](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/216) | Integrado |
| Execução de CI com 5 workflows verdes | [Run CI #43044216420](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43044216420) e checks do SHA correspondente | Histórico; repetir na candidata final |
| Cobertura percentual | [Resumo JaCoCo](EVIDENCIAS_COBERTURA_ENT04.md): 93,90% LINE e 82,88% BRANCH no run #43203143550 | Histórico verificado; repetir na candidata final |
| Gate de cobertura | [PR #218](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/218), `jacoco:check` com LINE 80% e BRANCH 70% | Integrado; revalidar na candidata final |
| SonarCloud | Sem análise e Quality Gate verificados | Pendente |
| JMeter | Sem JTL e relatório HTML verificados | Pendente |
| Falha controlada e recuperação CI | [Evidência documentada](EVIDENCIA_CI_RED_GREEN_ENT04.md): [run vermelho #43221134651](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43221134651) e [run verde #43223927880](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43223927880) | Comprovado em branch isolada; CI final ainda deve passar |
| Clone limpo, RTM e revisão cruzada final | Sem aceite vinculado ao SHA de release | Pendente |

## Protocolo de evidência por requisito

Para cada requisito da [RTM](../RTM.md), associar ID, teste automatizado ou procedimento manual, URL da execução, SHA, data, responsável, resultado e eventual defeito. A ausência de um desses dados deve ser explicitada como lacuna, não como sucesso.

Para carga: documentar autorização do ambiente, configuração de usuários/rampa/duração, massa sintética, JMX, JTL, HTML, p95, p99, throughput e taxa de erros. Para SonarCloud: organização, project key, análise no mesmo SHA, Quality Gate e autorização do token em secret.

## Critérios de decisão

- **Aprovado:** todos os requisitos obrigatórios têm evidência reproduzível no mesmo SHA/tag, gates exigidos passam, revisores aprovam e riscos residuais são aceitos nominalmente.
- **Bloqueado:** faltam evidências obrigatórias como SonarCloud, JMeter, rastreabilidade ou aprovação final; percentuais, gates e teste controlado vermelho/verde já têm evidências históricas, sujeitas à validação da release.
- **Reprovado:** teste/gate obrigatório falha ou evidência é inconsistente.

**Decisão atual: BLOQUEADO para fechamento da #186.** A CI verde das PRs incrementais não comprova os demais requisitos. Atualizar esta decisão apenas após conferir os artefatos finais.
