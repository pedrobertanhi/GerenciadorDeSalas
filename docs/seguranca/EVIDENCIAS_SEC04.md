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


## Diagnóstico de falhas (logs GitHub Actions)

- **Dependency Review:** job `129286262622` reportou `Dependency review is not supported on this repository` e orientou ativar o Dependency Graph nas configurações de segurança. **Bloqueio de configuração**, não evidência de CVE. Requer intervenção de mantenedor com acesso às configurações: `Settings → Security → Code security and analysis` e habilitar o Dependency Graph / recurso de dependency review, conforme disponibilidade do plano e visibilidade do repositório.
- **Trivy filesystem:** job `129286263050` falhou antes do scan com `Unable to resolve action aquasecurity/trivy-action@0.33.1`. A referência foi alterada para `0.33.0`; confirmar que a nova execução consegue resolver a action e realizar o scan antes de marcar como aprovado.

**Não ignorar, pular nem remover gates com falha para liberar o merge.**
