# SonarQube Cloud — configuração verificável

Projeto: [pedrobertanhi_GerenciadorDeSalas](https://sonarcloud.io/project/overview?id=pedrobertanhi_GerenciadorDeSalas)  
Organização: `pedrobertanhi`

## Integração contínua

O workflow `.github/workflows/sonarcloud.yml` executa, em `main`, pull requests internos
e disparo manual:

1. `./mvnw verify`, que gera `target/site/jacoco/jacoco.xml` e aplica o gate local
   de 80% LINE e 70% BRANCH;
2. análise Maven do SonarQube Cloud, identificada pelo projeto e pela organização;
3. espera pelo resultado do Quality Gate (`sonar.qualitygate.wait=true`).

O checkout possui histórico completo para que a análise de novo código e de pull
request seja correta. PRs vindas de forks não executam esse workflow porque o
GitHub não disponibiliza secrets nelas.

## Secret obrigatório

Criar no GitHub em **Settings → Secrets and variables → Actions**:

| Nome | Valor | Regra |
|---|---|---|
| `SONAR_TOKEN` | token de análise gerado no SonarQube Cloud para a organização/projeto | nunca versionar, comentar ou registrar em evidência |

Após o secret estar salvo, abrir uma PR interna ou usar **Actions → SonarQube
Cloud → Run workflow**. A evidência válida é a URL da execução verde e a URL da
análise, no mesmo SHA. Se o gate falhar, corrigir os achados ou registrar uma
decisão de risco justificada; não desabilitar o gate.
