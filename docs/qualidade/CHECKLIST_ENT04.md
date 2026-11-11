# Checklist de entrega ENT-04 (#186)

> Documento de controle: itens sem evidência são pendentes, não aprovados. Atualizar para o SHA final antes da decisão.

| Requisito | Estado em 11/11/2026 | Evidência ou próximo passo |
|---|---|---|
| Testes Maven e segurança | Execuções históricas verdes | [Relatório de testes](../testes/RELATORIO_DE_TESTES.md) e verificações da PR #215; revalidar no último SHA |
| Publicação Surefire/Failsafe/JaCoCo | Implementada na branch da PR #215 | Conferir artefato `relatorios-maven` após CI |
| Geração JaCoCo | Configurada no `pom.xml` da PR #215 | Conferir `target/site/jacoco` no artefato |
| Cobertura >=80% linhas e >=70% branches | PENDENTE | Extrair contadores JaCoCo reais; definir gate e exceções justificadas |
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
- [RTM](../RTM.md)
- [Relatório de testes](../testes/RELATORIO_DE_TESTES.md)
