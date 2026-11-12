# Evidências de cobertura e próximos gates — ENT-04

## Execução verificável

- Commit: `ac816c9f46dcb3ec363daa4b4a16bee9cf879b7e`
- [CI Maven Verify aprovada](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43044216420)
- [Artefato Maven/JaCoCo](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43044216420/artifacts/13866487596)
- CI, CodeQL, Dependency Review, Trivy e ZAP: sucesso para este commit.

A CI extrai os contadores LINE e BRANCH de `target/site/jacoco/jacoco.xml`, escreve os percentuais no resumo da execução e gera `target/site/jacoco/RESUMO_COBERTURA.md`. Na [CI da PR #217, execução 43203143550](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43203143550), o log de `Maven Verify` registrou **93,90% LINE (2431/2589)** e **82,88% BRANCH (755/911)**. São resultados históricos do SHA `62d01d69a0b0c13485be0a0890139156da4c9291`; a candidata final precisa de nova execução.

## Como registrar e avaliar

1. Abrir a execução e consultar **Summary > Cobertura JaCoCo**; alternativamente, baixar o artefato `relatorios-maven-<tentativa>` correspondente ao número da tentativa exibido na execução (por exemplo, `relatorios-maven-2` após uma reexecução) e abrir `site/jacoco/RESUMO_COBERTURA.md` ou `jacoco.xml`.
2. Registrar os contadores cobertos/perdidos, percentuais de linhas e branches, commit e URL do run.
3. Comparar com as metas do projeto (linhas >= 80%, branches >= 70%).
4. O `jacoco:check` está integrado à `main` pela [PR #218](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/218), com limites de 80% LINE e 70% BRANCH. A [falha controlada e recuperação verde](EVIDENCIA_CI_RED_GREEN_ENT04.md) foram comprovadas na PR #219: [run vermelho #43221134651](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43221134651) e [run verde #43223927880](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43223927880). Revalidar a candidata final no mesmo SHA/tag da release.

## Integrações ainda bloqueadas

**SonarCloud:** requer organização/projeto, configuração e token autorizados. Registrar link da análise, quality gate e commit após execução. Não colocar token em código ou logs.

**JMeter:** requer ambiente autorizado, dados sintéticos e parâmetros de carga. Guardar plano JMX, JTL, relatório HTML, throughput, latência p95/p99, erros, configuração e SHA. Não testar endpoints públicos sem autorização.

**Entrega final:** atualizar RTM, riscos, defeitos, matriz de responsabilidades, diagramas e decisão de qualidade com evidências da mesma candidata. A issue #186 permanece aberta até esse aceite.
