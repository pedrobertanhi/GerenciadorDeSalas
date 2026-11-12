# Evidência de CI vermelho → verde — ENT-04 (#186)

## Falha controlada confirmada

- PR experimental (draft): https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/219
- Commit vermelho: `ed0996f558bbee1ad87d72312ac13f964eef2b75`
- Execução CI vermelha: https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43221134651
- O gate JaCoCo foi elevado **temporariamente** para 100% de linhas e 100% de branches, apenas na branch experimental.
- Log do job `Maven Verify`: `Rule violated ... lines covered ratio is 0.93, but expected minimum is 1.00`; `Rule violated ... branches covered ratio is 0.82, but expected minimum is 1.00`; `BUILD FAILURE`; `Coverage checks have not been met`.
- Os quatro demais workflows (ZAP Baseline, Dependency Review, Trivy filesystem e CodeQL) concluíram com sucesso.

## Recuperação

- Commit de restauração: `b9a0453b6e146ac19011cfbd7a54e18631b99802`.
- Limites oficiais restaurados: **80% LINE** e **70% BRANCH**.
- **Execução verde pendente de confirmação**: registrar aqui a URL do run do commit restaurado quando concluído.

## Segurança e escopo

Não houve desativação ou mascaramento de testes. A branch foi isolada e a PR permaneceu em draft durante a prova; os limites temporários de 100% nunca foram integrados à `main`. A decisão de aceite final da #186 continua dependente das demais evidências.
