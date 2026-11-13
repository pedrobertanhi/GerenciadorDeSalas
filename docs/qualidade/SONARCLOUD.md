# SonarQube Cloud — configuração e evidências

Projeto: [pedrobertanhi_GerenciadorDeSalas](https://sonarcloud.io/project/overview?id=pedrobertanhi_GerenciadorDeSalas)  
Organização: `pedrobertanhi`

## Comportamento do workflow

O workflow `.github/workflows/sonarcloud.yml` protege o `SONAR_TOKEN` e possui dois comportamentos:

| Evento | O que executa |
|---|---|
| Pull request para `main` | `Maven Verify` sem secret. Gera e valida a cobertura JaCoCo. O job autenticado do Quality Gate é pulado. |
| Push na `main` ou disparo manual na `main` | `Maven Verify`, análise autenticada no SonarQube Cloud e espera pelo Quality Gate. |

A concorrência é cancelável por branch, evitando que uma análise antiga sobrescreva uma análise mais recente da `main`.

## Secret obrigatório

Criar no GitHub em **Settings → Secrets and variables → Actions**:

| Nome | Valor | Regra |
|---|---|---|
| `SONAR_TOKEN` | token de análise gerado no SonarQube Cloud para a organização/projeto | nunca versionar, comentar ou registrar em evidência |

## Evidências

- A candidata `98e59a55a2cde9050b480055a7d0be8b29d4e72d` da PR #221 teve [Quality Gate aprovado](https://sonarcloud.io/dashboard?id=pedrobertanhi_GerenciadorDeSalas&pullRequest=221), com 0 novas issues.
- Na execução [#43426869863](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43426869863), referente a uma PR, o Maven Verify passou e o job autenticado **SonarCloud Quality Gate** foi pulado por desenho do workflow.
- Após o merge, conferir a execução do workflow na `main`, incluindo o Quality Gate e o SHA analisado, antes de encerrar a Issue #186.

Se o gate da `main` falhar, corrigir os achados ou registrar uma decisão de risco justificada; não desabilitar o gate.