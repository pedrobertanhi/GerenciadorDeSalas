# Parecer Final de Segurança

> Status: evidências da candidata atualizadas. A decisão de liberação da ENT-02 (#184) aguarda revisão de outro integrante. Responsável pela entrega: @davisalves38-lab.

## Candidata após as atualizações de dependências

SHA analisado: [`a5b38c7e06278dc5db51188195364808bdbd5f4c`](https://github.com/pedrobertanhi/GerenciadorDeSalas/commit/a5b38c7e06278dc5db51188195364808bdbd5f4c). As quatro execuções abaixo foram disparadas na `main` e apontam para esse SHA.

| Controle | Evidência | Resultado |
|---|---|---|
| Maven Verify | [CI](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42741507363) | 835 testes, incluindo os cinco de navegador, sem falhas, erros ou testes ignorados |
| CodeQL | [Execução](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42741507704) | Job concluído com sucesso |
| Trivy filesystem | [Execução](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42741507503) | Job concluído com sucesso no filtro de severidade crítica |
| OWASP ZAP Baseline | [Execução](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42741507387) e [relatório e SHA](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42741507387/artifacts/13729252842) | 0 falhas, 6 avisos e 62 regras aprovadas |

Os seis avisos do ZAP coincidem com a triagem histórica abaixo. A análise passiva foi limitada à superfície pública de login. O painel de Secret scanning registra o estado do repositório na consulta documentada. Dependency Review roda nos PRs de atualização, em seus respectivos SHAs. Os resultados desta candidata aguardam revisão.

## Candidata anterior após o merge do ZAP

SHA analisado: [`f3b3d44d08c9f79b3db9593bc86f864866f4f248`](https://github.com/pedrobertanhi/GerenciadorDeSalas/commit/f3b3d44d08c9f79b3db9593bc86f864866f4f248), merge do PR #200 em `main`. As quatro execuções abaixo apontam para esse SHA.

| Controle | Evidência | Resultado |
|---|---|---|
| Maven Verify | [CI](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42740527307) | Aprovado — 835 testes, 0 falhas, 0 erros e 0 ignorados. Cinco casos de navegador executados. |
| CodeQL | [Execução](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42740527440) | Job concluído com sucesso |
| Trivy filesystem | [Execução](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42740527276) | Job concluído com sucesso na configuração de severidade crítica |
| OWASP ZAP Baseline | [Execução](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42740527332) e [relatório HTML/JSON e SHA](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42740527332/artifacts/13728453064) | 0 falhas, 6 avisos, 62 regras aprovadas |

### Triagem dos avisos do ZAP

Análise passiva da superfície pública de login em runner isolado com PostgreSQL descartável. O workflow publica os avisos sem falhar o job. A avaliação cobre o que o scanner observou nesse ambiente.

| Aviso | Avaliação | Acompanhamento |
|---|---|---|
| CSP ausente (10038) | O login respondeu sem política de conteúdo. Falta uma camada de contenção para conteúdo injetado. | Definir uma CSP compatível com os recursos da aplicação e validar o login em navegador. |
| Conteúdo não armazenável (10049) | Observado também em respostas 401. Em login e respostas protegidas, restringir cache é desejável. | Preservar os cabeçalhos de não armazenamento. |
| Permissions Policy ausente (10063) | Cabeçalho de defesa adicional ausente no login. | Avaliar restrição de funcionalidades do navegador que a aplicação dispensa. |
| Resposta de sessão identificada (10112) | O scanner reconheceu o mecanismo de sessão. O aviso descreve a presença desse mecanismo. | Usar os testes de autenticação, logout e revogação de sessão como evidência funcional. |
| SRI ausente (90003) | O login referencia o CSS do Bootstrap via CDN sem atributo de integridade. | Fixar a integridade do recurso ou servir o arquivo localmente. |
| COEP ausente (90004) | O login não aplica isolamento de incorporação entre origens. | Avaliar se o isolamento é necessário e testar compatibilidade com o CSS externo antes de alterar. |

A triagem não confirmou falha crítica nesses seis avisos. A superfície autenticada fica coberta pelos testes funcionais de autorização, com a análise passiva do ZAP limitada ao fluxo público. Secret scanning e Push protection foram verificados pelas capturas de configuração e do painel. A revisão de dependências permanece vinculada ao SHA do PR #193. Os itens de endurecimento e o risco residual aguardam revisão por outro integrante. O artefato do ZAP tem retenção de 14 dias.

## Candidata anterior após a integração dos controles

O merge do [PR #193](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/193) em `main` gerou a candidata [`59fa404c2981955143032a8bf876665a9138e681`](https://github.com/pedrobertanhi/GerenciadorDeSalas/commit/59fa404c2981955143032a8bf876665a9138e681). Os links abaixo foram conferidos pelo campo `head_sha` das execuções do GitHub Actions.

| Controle | Evidência | Resultado |
|---|---|---|
| Maven Verify | [Execução #42737088444](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088444) | Aprovado — 835 testes, 0 falhas, 0 erros e 0 ignorados |
| Telas administrativas em Chrome, desktop e celular | [Execução #42737088444](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088444) | 5 casos executados, sem falhas, erros ou ignorados |
| CodeQL | [Execução #42737088702](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088702) | Aprovado |
| Trivy filesystem | [Execução #42737088435](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737088435) | Aprovado. O relatório do POM registrou 0 vulnerabilidades críticas. A configuração filtra pela severidade crítica. |
| Dependabot | [Execução #42737093875](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737093875) (Maven), [Execução #42737095367](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42737095367) (Actions) | Execuções de atualização identificadas. Não representam uma contagem de alertas. |
| Dependency Review | [Execução #42736529766](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42736529766) | Aprovado no PR #193 em `e75003aa09a2a71f2782fbef9eea630a835acaa7`. O workflow é disparado por pull request. |
| Secret scanning e push protection | Capturas de Settings e [Secret scanning](https://github.com/pedrobertanhi/GerenciadorDeSalas/security/secret-scanning) fornecidas pelo mantenedor em 09/11/2026 | Secret Protection e Push protection habilitados. O painel mostrava 0 alertas abertos na consulta. |
| Trivy image | Candidata sem Dockerfile versionado | Sem evidência de imagem publicada |
| OWASP ZAP Baseline | [Workflow ZAP Baseline](../../.github/workflows/zap-baseline.yml) | Primeira execução validada no PR #200. Evidência no SHA final da release pendente. |

### Primeira execução do ZAP Baseline

A [execução #42739754196](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42739754196) analisou a superfície pública de login em um banco descartável. O [artefato HTML/JSON e commit.txt](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42739754196/artifacts/13729255221) corresponde ao merge de teste do PR, `e138693cffca5b1bf0ca5d2d6a9dfdd61ec41916`.

O resumo do ZAP registrou 0 falhas, 6 avisos e 62 regras aprovadas. Os avisos foram ausência de CSP, Permissions Policy, SRI e COEP, conteúdo não armazenável e identificação de resposta de sessão. A execução foi configurada para publicar avisos sem falhar o job. Esses avisos ficam para triagem e a análise deve ser repetida no SHA final da release.

As capturas de Secret scanning registram o estado do repositório na data da consulta. O relatório do ZAP identifica separadamente o SHA analisado.

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
- [ ] REPROVADA

Decisão pendente de revisão dos resultados e riscos residuais.

Se qualquer etapa obrigatória estiver ausente ou houver vulnerabilidade crítica confirmada, o parecer deve ser **REPROVADA**.
