# Relatório de Testes — evidências rastreáveis

> Situação: evidências históricas disponíveis; validação da candidata final da #186 ainda pendente. Não usar os resultados abaixo como aprovação automática de commits posteriores.

## Execuções verificadas e limites

| Candidata | Execução | Resultado | Limitação |
|---|---|---|---|
| `1fde5bd4fd74cfa4ec6c4dea3789a44adc6bc4e3` | [Maven Verify 42920680085](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42920680085) | 837 testes, zero falhas, erros ou ignorados | Evidência da candidata de segurança, não da entrega #186 |
| `1643d86` | [Maven Verify 42945878207](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42945878207) | 898 testes, zero falhas, conforme [RTM](../RTM.md) | Evidência histórica; conferir SHA completo e artefatos |
| PR #215, commit `2724802301d13f0689d3cc567a8d3422f08bcfab` | [GitHub Actions](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/215/checks) | CI, CodeQL, Dependency Review, Trivy e ZAP concluídos com sucesso na consulta de 11/11/2026 | PR recebeu commits posteriores; verificações precisam ser repetidas |

## Cobertura e métricas

O plugin JaCoCo 0.8.15 foi adicionado na branch da #186 para gerar `target/site/jacoco/index.html` e `target/site/jacoco/jacoco.xml` durante `mvn verify`. **Nenhuma porcentagem é afirmada antes da publicação do artefato.** As metas do projeto são 80% de linhas e 70% de branches. A implementação do gate de cobertura depende da medição real e da decisão documentada sobre exclusões justificadas.

Relatórios Surefire, Failsafe (se existirem) e JaCoCo são coletados pela CI no artefato `relatorios-maven`, mesmo em falha. A ausência de relatório é explicitamente avisada pelo workflow.

## Pendências para a decisão final da #186

- Conferir execução verde e artefatos da cabeça final da PR #215 após todos os commits.
- Consolidar quantidades por tipo de teste diretamente dos XMLs Surefire/Failsafe, sem atribuir totais sem contagem.
- Medir cobertura JaCoCo e registrar percentual de linhas/branches com URL do artefato.
- Executar JMeter em ambiente autorizado com massa sintética e guardar percentis, throughput, taxa de erros e configuração da carga.
- Registrar análise SonarCloud quando o projeto, token e permissões estiverem disponíveis; não inventar análise.
- Registrar decisão de qualidade e revisão cruzada associadas ao mesmo SHA/tag.

## Critério de conclusão

Somente declarar a entrega pronta após confirmar evidências do mesmo commit, ausência de falhas bloqueantes, revisão cruzada e aceite explícito dos riscos residuais.
