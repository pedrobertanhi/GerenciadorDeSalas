# Checklist de entrega ENT-04 (#186)

> Documento de controle: itens sem evidência são pendentes, não aprovados. Atualizar para o SHA final antes da decisão.

| Requisito | Estado em 12/11/2026 | Evidência ou próximo passo |
|---|---|---|
| Testes Maven e segurança | CI validada nas PRs #215 e #216 | [Relatório de testes](../testes/RELATORIO_DE_TESTES.md) e verificações da PR #215; revalidar no último SHA |
| Publicação Surefire/Failsafe/JaCoCo | Integrada à main via PR #215 | [Artefato da execução #216](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43044216420/artifacts/13866487596); usar sufixo da tentativa em reexecuções |
| Geração JaCoCo | Integrada ao `pom.xml` via PR #215 | Ver [evidências JaCoCo](EVIDENCIAS_COBERTURA_ENT04.md) e PR #216 |
| Cobertura >=80% linhas e >=70% branches | Gate integrado via PR #218; resultado histórico validado | [Evidências de cobertura](EVIDENCIAS_COBERTURA_ENT04.md): 93,90% LINE e 82,88% BRANCH no run #43203143550; revalidar no SHA final |
| SonarCloud | CONFIGURADO; execução pendente | Projeto `pedrobertanhi_GerenciadorDeSalas` importado; workflow e procedimento em [SONARCLOUD.md](SONARCLOUD.md). Salvar `SONAR_TOKEN` como secret e validar o gate no SHA final |
| TDD/BDD e API/E2E | Evidência parcial na RTM | Consolidar cenário, commits red-green-refactor e testes reproduzíveis |
| Relógio/fuso reproduzíveis | NÃO AUDITADO INTEGRALMENTE | Inspecionar testes temporais e executar em fusos controlados |
| CI falha controlada e recuperação | EVIDENCIADO | [Par vermelho/verde](EVIDENCIA_CI_RED_GREEN_ENT04.md): [falha #43221134651](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43221134651) e [recuperação #43223927880](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43223927880); CI do SHA final da PR deve passar |
| Branch protection/check obrigatório | CONFIGURADO | Ruleset ativo na `main`: PR, 2 aprovações, conversas resolvidas, branch atualizada e checks CI/segurança |
| JMeter autorizado | PENDENTE | Definir ambiente e massa sintética; executar e anexar JTL/HTML reais |
| RTM, riscos, defeitos, diagramas e responsabilidades | PARCIAL | Atualizar links e responsáveis com evidências do mesmo SHA |
| Clone limpo e documentação | PENDENTE | Validar README, dependências, compose e links em ambiente novo |
| Revisão cruzada e decisão final | PENDENTE | Registrar revisores, aprovação, riscos e SHA/tag final |

## Critério de bloqueio

Não marcar a #186 como concluída apenas porque o CI da PR está verde. A decisão de liberação exige cobertura mensurada, evidências de carga e qualidade, rastreabilidade e revisão. Sem ambiente autorizado ou credenciais SonarCloud, registrar bloqueio explícito em vez de inventar resultado.

## Referências

- [Issue #186](https://github.com/pedrobertanhi/GerenciadorDeSalas/issues/186)
- [PR #215](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/215)
- [PR #216](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/216)
- [Evidências de cobertura](EVIDENCIAS_COBERTURA_ENT04.md)
- [Evidência CI vermelho/verde](EVIDENCIA_CI_RED_GREEN_ENT04.md)
- [PR #218](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/218)
- [PR #219](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/219)
- [RTM](../RTM.md)
- [Relatório de testes](../testes/RELATORIO_DE_TESTES.md)
