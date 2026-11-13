# Checklist de entrega ENT-04 (#186)

> Documento de controle: itens sem evidência são pendentes, não aprovados. Atualizado em 13/11/2026 para a PR #221.

| Requisito | Estado | Evidência ou próximo passo |
|---|---|---|
| Testes Maven e segurança | EVIDENCIADO | CI do SHA atual passou: Maven Verify, ZAP, Dependency Review, Trivy e CodeQL. |
| Cobertura >=80% linhas e >=70% branches | EVIDENCIADO | 93,90% LINE e 82,88% BRANCH no Maven Verify da PR #221. |
| SonarCloud | EVIDENCIADO | Quality Gate aprovado na PR #221. O workflow protege o token: PR executa Maven Verify sem secret; análise autenticada ocorre somente na main. |
| TDD/BDD e API/E2E | PARCIAL | Evidências existentes na RTM; manter como risco residual. |
| Relógio/fuso reproduzíveis | NÃO AUDITADO INTEGRALMENTE | Risco residual: inspeção de testes temporais e execução em fusos controlados. |
| CI falha controlada e recuperação | EVIDENCIADO | [Par vermelho/verde](EVIDENCIA_CI_RED_GREEN_ENT04.md). |
| Branch protection/check obrigatório | CONFIGURADO | Ruleset da main exige PR, 2 aprovações, conversas resolvidas, branch atualizada e checks CI/segurança. |
| JMeter autorizado | EVIDENCIADO | [Resultado do teste de carga](../desempenho/RESULTADO_TESTE_CARGA.md), JTL, relatório HTML e plano JMX em `performance/`. Baseline: 12.742 amostras, 0% erros, p95 6 ms e 212,54 req/s. Disputa: uma reserva aprovada e uma recusada por conflito. |
| RTM, riscos, defeitos, diagramas e responsabilidades | PARCIAL | RTM existente; riscos residuais registrados na decisão final. |
| Clone limpo e documentação | PENDENTE | Executar validação em ambiente novo antes do merge e registrar o resultado. |
| Revisão cruzada e decisão final | PENDENTE DE APROVAÇÃO | [Decisão de qualidade](DECISAO_QUALIDADE_ENT04.md) e [roteiro da apresentação](ROTEIRO_APRESENTACAO_ENT04.md) preparados; faltam clone limpo e 2 aprovações humanas. |

## Decisão atual

A entrega está tecnicamente apta nos checks automatizados e tem evidência real de carga. O merge permanece bloqueado até a validação de clone limpo, resolução das conversas e duas aprovações humanas requeridas pelo ruleset.

## Referências

- [Issue #186](https://github.com/pedrobertanhi/GerenciadorDeSalas/issues/186)
- [PR #221](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/221)
- [Resultado JMeter](../desempenho/RESULTADO_TESTE_CARGA.md)
- [Evidências de cobertura](EVIDENCIAS_COBERTURA_ENT04.md)
- [Evidência CI vermelho/verde](EVIDENCIA_CI_RED_GREEN_ENT04.md)
- [RTM](../RTM.md)
