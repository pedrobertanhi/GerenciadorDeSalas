# Checklist de entrega ENT-04 (#186)

> Documento de controle: itens sem evidência são pendentes, não aprovados. Atualizar para o SHA final antes da decisão.

| Requisito | Estado em 12/11/2026 | Evidência ou próximo passo |
|---|---|---|
| Testes Maven e segurança | CI validada nas PRs #215 e #216 | [Relatório de testes](../testes/RELATORIO_DE_TESTES.md) e verificações da PR #215; revalidar no último SHA |
| Publicação Surefire/Failsafe/JaCoCo | Integrada à main via PR #215 | [Artefato da execução #216](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43044216420/artifacts/13866487596); usar sufixo da tentativa em reexecuções |
| Geração JaCoCo | Integrada ao `pom.xml` via PR #215 | Ver [evidências JaCoCo](EVIDENCIAS_COBERTURA_ENT04.md) e PR #216 |
| Cobertura >=80% linhas e >=70% branches | PENDENTE | Resumo LINE/BRANCH automatizado na PR #216; registrar percentuais do run final e só então configurar gate |
| SonarCloud | PENDENTE | Necessário projeto configurado, token e execução verificável |
| TDD/BDD e API/E2E | Evidência parcial na RTM | Consolidar cenário, commits red-green-refactor e testes reproduzíveis |
| Relógio/fuso reproduzíveis | NÃO AUDITADO INTEGRALMENTE | Inspecionar testes temporais e executar em fusos controlados |
| CI falha controlada e recuperação | PENDENTE | Evidenciar dois runs, vermelho e verde, sem desabilitar teste |
| Branch protection/check obrigatório | NÃO VERIFICADO | Conferir configuração GitHub com permissão administrativa |
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
- [RTM](../RTM.md)
- [Relatório de testes](../testes/RELATORIO_DE_TESTES.md)
