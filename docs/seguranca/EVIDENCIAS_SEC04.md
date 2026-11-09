# Evidências e bloqueios — SEC-04 (#14)

Este registro distingue **configuração**, **execução** e **aprovação**. Um workflow adicionado não prova ausência de vulnerabilidades.

## Execuções verificadas

| Commit | Controle | Resultado observado | Observação |
|---|---|---|---|
| `671581cd07457f7c280d7a5342e916db72129ec8` | Maven Verify | Success | GitHub Actions |
| `671581cd07457f7c280d7a5342e916db72129ec8` | CodeQL Java | Success | Execução concluída; ainda conferir alertas e triagem |
| `72956d5d0035c8bc229cd50aa1acb9bd7064a6ca` | Dependency Review | Failure | Falha no passo de revisão; **causa ainda não diagnosticada** |
| `72956d5d0035c8bc229cd50aa1acb9bd7064a6ca` | Maven Verify / CodeQL | Em execução na consulta | Não declarar sucesso antecipadamente |

## Gate

- A falha de Dependency Review **bloqueia** a aprovação, independentemente de ser vulnerabilidade detectada ou limitação de configuração.
- O scan Trivy filesystem foi configurado em commit posterior, mas seu resultado ainda não foi confirmado.
- Dependabot depende da configuração efetivamente reconhecida pelo GitHub.
- Secret scanning/push protection exigem checagem das configurações do repositório e evidência de funcionamento.
- Trivy image e OWASP ZAP dependem de imagem/aplicação candidata, alvo controlado e execução rastreável.
- Não emitir parecer favorável até que os controles obrigatórios estejam comprovados e revisados por outro integrante.

## Ações necessárias

1. Inspecionar logs do job Dependency Review e corrigir a causa, sem desabilitar o gate para obter resultado verde.
2. Reexecutar CI para o último SHA e registrar os resultados.
3. Completar SCA, secret scanning, Trivy image, ZAP e testes de autorização/entrada malformada.
4. Atualizar `docs/testes/RELATORIO_TESTES_SEGURANCA.md` com números e links reais antes de solicitar revisão final.
