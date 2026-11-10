# Parecer Final de Segurança

> Status: candidata bloqueada por evidências obrigatórias pendentes. Registro da ENT-02 (#184) para revisão de outro integrante.

## Candidata após a integração dos controles

O merge do [PR #193](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/193) em `main` gerou a candidata [`59fa404c2981955143032a8bf876665a9138e681`](https://github.com/pedrobertanhi/GerenciadorDeSalas/commit/59fa404c2981955143032a8bf876665a9138e681). Os links abaixo foram conferidos pelo campo `head_sha` das execuções do GitHub Actions.

| Controle | Evidência | Resultado |
|---|---|---|
| Maven Verify | [Execução #42737088444](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088444) | Aprovado — 835 testes, 0 falhas, 0 erros e 0 ignorados |
| Telas administrativas em Chrome, desktop e celular | [Execução #42737088444](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088444) | 5 casos executados, sem falhas, erros ou ignorados |
| CodeQL | [Execução #42737088702](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088702) | Aprovado |
| Trivy filesystem | [Execução #42737088435](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088435) | Aprovado. O relatório do POM registrou 0 vulnerabilidades críticas. A configuração filtra pela severidade crítica. |
| Dependabot | [Execução #42737093875](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737093875) (Maven), [Execução #42737095367](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737095367) (Actions) | Execuções de atualização identificadas. Não representam uma contagem de alertas. |
| Dependency Review | [Execução #42736529766](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42736529766) | Aprovado no PR #193 em `e75003aa09a2a71f2782fbef9eea630a835acaa7`. O workflow é disparado por pull request. |
| Secret scanning e push protection | Configurações do repositório | Ativação e resultado ainda sem confirmação |
| Trivy image | Candidata sem Dockerfile versionado | Sem evidência de imagem publicada |
| OWASP ZAP | Ambiente controlado com dados sintéticos | Evidência pendente |

As execuções da candidata anterior permanecem abaixo para rastreabilidade. A decisão de liberação continua pendente dos controles sem evidência, da triagem de alertas e da revisão por outro integrante.

## Identificação da candidata anterior

| Campo | Valor |
|---|---|
| Release/tag | Candidata identificada por SHA |
| Commit | [8385c6aab6e7b14de964aaf90317f917f9b99397](https://github.com/pedrobertanhi/GerenciadorDeSalas/commit/8385c6aab6e7b14de964aaf90317f917f9b99397) |
| Data da conferência | 09/11/2026 |
| Responsável pela issue | @davisalves38-lab |
| Revisor | Não atribuído |

## Resultados da candidata anterior

| Controle | Execução | Críticas abertas | Altas abertas | Evidência | Situação |
|---|---|---:|---:|---|---|
| SonarCloud | Pendente | - | - | A definir | Pendente |
| CodeQL | Pendente | - | - | A definir | Pendente |
| Dependências/SCA | Pendente | - | - | A definir | Pendente |
| Secret scanning | Pendente | - | - | A definir | Pendente |
| Trivy filesystem | Pendente | - | - | A definir | Pendente |
| Trivy image | Pendente | - | - | A definir | Pendente |
| OWASP ZAP | Pendente | - | - | A definir | Pendente |
| Maven Verify da candidata | CI iniciado na main | — | — | [Execução #42734185387](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42734185387) | Aprovado — 830 testes, 0 falhas, 0 erros e 0 ignorados |
| Administração de usuários em desktop e celular | Não executada | — | — | A compilação local parou ao resolver o POM pai no Maven Central | Pendente |

A execução [#42734185387](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42734185387) foi disparada por um push na `main`. O campo `head_sha` da [API do GitHub Actions](https://api.github.com/repos/pedrobertanhi/GerenciadorDeSalas/actions/runs/42734185387) confirma que o SHA testado, `8385c6aab6e7b14de964aaf90317f917f9b99397`, coincide com o da candidata. O resultado de 830 testes se aplica a esse SHA.

## Evidência de navegador após a candidata acima

O [Maven Verify do PR #192](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42735598179) executou `UsuariosNavegadorTest` em desktop (1280 × 800) e celular (390 × 844), incluindo paginação e acesso por perfil. Foram 5 casos aprovados, com zero falhas, erros ou ignorados no SHA `e62e86cc770bed2c08832da8f4eaf15fff08ce9f`. Revalidar no SHA que reunir os controles de segurança para a release.

## Lacunas registradas na candidata anterior

- A árvore versionada contém apenas os workflows `ci.yml` e `docker-compose.yml`. CodeQL, revisão de dependências, Trivy, Dependabot e ZAP não têm configuração versionada nessa candidata. Verificar também alertas, secret scanning e push protection nas configurações do repositório.
- Vincular os relatórios disponíveis ao SHA analisado e registrar a triagem. Os campos vazios de achados representam ausência de evidência.
- Para a validação visual, usar contas sintéticas e repetir em desktop e viewport móvel os fluxos de listagem e paginação, cadastro, edição, ativação e inativação em **Salas → Gerenciar usuários**. Conferir acesso negado para os outros perfis. Registrar navegador, dimensões, resultado e capturas sem credenciais.
- Evidências dos PRs da API e das telas são rastreadas na RTM e se referem aos respectivos commits.

## Gate

- [ ] Zero vulnerabilidade crítica confirmada e aberta.
- [ ] Nenhum segredo confirmado.
- [ ] Todos os alertas foram triados.
- [ ] Achados altos possuem correção ou risco residual formal.
- [ ] Testes de autorização estão verdes.
- [ ] A release analisada é a mesma da decisão de qualidade.
- [ ] Outro integrante revisou as evidências.

## Parecer

- [ ] APROVADA
- [x] REPROVADA por falta de evidências obrigatórias

Se qualquer etapa obrigatória estiver ausente ou houver vulnerabilidade crítica confirmada, o parecer deve ser **REPROVADA**.
