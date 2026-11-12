# Evidências de cobertura e próximos gates — ENT-04

## Execução verificável

- Commit: `ac816c9f46dcb3ec363daa4b4a16bee9cf879b7e`
- [CI Maven Verify aprovada](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43044216420)
- [Artefato Maven/JaCoCo](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43044216420/artifacts/13866487596)
- CI, CodeQL, Dependency Review, Trivy e ZAP: sucesso para este commit.

A CI extrai os contadores LINE e BRANCH de `target/site/jacoco/jacoco.xml`, escreve os percentuais no resumo da execução e gera `target/site/jacoco/RESUMO_COBERTURA.md`. **Os valores numéricos precisam ser transcritos do relatório efetivamente publicado**; nenhum valor é estimado neste documento.

## Como registrar e avaliar

1. Abrir a execução e consultar **Summary > Cobertura JaCoCo**; alternativamente, baixar o artefato `relatorios-maven-<tentativa>` correspondente ao número da tentativa exibido na execução (por exemplo, `relatorios-maven-2` após uma reexecução) e abrir `site/jacoco/RESUMO_COBERTURA.md` ou `jacoco.xml`.
2. Registrar os contadores cobertos/perdidos, percentuais de linhas e branches, commit e URL do run.
3. Comparar com as metas do projeto (linhas >= 80%, branches >= 70%).
4. Se ambas forem atingidas, implementar `jacoco:check` com limites explícitos em PR separada e provar uma execução verde e uma falha controlada. Se não, abrir correções de testes sem reduzir metas silenciosamente.

## Integrações ainda bloqueadas

**SonarCloud:** requer organização/projeto, configuração e token autorizados. Registrar link da análise, quality gate e commit após execução. Não colocar token em código ou logs.

**JMeter:** requer ambiente autorizado, dados sintéticos e parâmetros de carga. Guardar plano JMX, JTL, relatório HTML, throughput, latência p95/p99, erros, configuração e SHA. Não testar endpoints públicos sem autorização.

**Entrega final:** atualizar RTM, riscos, defeitos, matriz de responsabilidades, diagramas e decisão de qualidade com evidências da mesma candidata. A issue #186 permanece aberta até esse aceite.
