# Parecer Final de Segurança

> Status: candidata bloqueada por evidências obrigatórias pendentes. Registro da ENT-02 (#184) para revisão de outro integrante.

## Identificação

| Campo | Valor |
|---|---|
| Release/tag | Candidata identificada por SHA |
| Commit | [8385c6aab6e7b14de964aaf90317f917f9b99397](https://github.com/pedrobertanhi/GerenciadorDeSalas/commit/8385c6aab6e7b14de964aaf90317f917f9b99397) |
| Data da conferência | 09/11/2026 |
| Responsável pela issue | @davisalves38-lab |
| Revisor | Não atribuído |

## Resultados

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

## Lacunas da candidata

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
