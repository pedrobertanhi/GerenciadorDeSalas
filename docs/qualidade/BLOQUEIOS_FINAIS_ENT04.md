# Pendências executáveis para aceite da ENT-04 (#186)

## Comprovado no GitHub

- Relatórios Surefire/Failsafe/JaCoCo publicados em CI: [PR #215](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/215).
- Resumo de cobertura rastreável: [PR #216](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/216).
- Cobertura histórica de **93,90% LINE** e **82,88% BRANCH**: [run #43203143550](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43203143550).
- Gate Maven LINE >= 80% e BRANCH >= 70%: [PR #218](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/218).
- Falha controlada: [run #43221134651](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43221134651); recuperação: [run #43223927880](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/43223927880); documentação: [PR #219](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/219).

## Bloqueios que não podem ser declarados concluídos

| Requisito | Insumo externo necessário | Evidência para desbloquear |
|---|---|---|
| SonarCloud | Organização e projeto autorizados, integração GitHub e token/secret configurado por administrador | URL da análise no SHA final, Quality Gate aprovado e ausência de vulnerabilidade crítica confirmada |
| JMeter | URL de ambiente **autorizado**, credenciais de teste se necessárias, massa sintética, perfil de carga e limites aprovados | JMX, JTL e HTML reais, p95/p99, throughput, erros e verificação de integridade |
| Proteção de branch | Acesso administrativo às regras de branch/regrasets | Captura ou URL verificável de checks e revisões obrigatórios |
| Rastreabilidade/aceite | Responsáveis e revisão dos requisitos da RTM, riscos e defeitos, ambiente de clone limpo | Links por requisito para o mesmo SHA/tag, decisão assinada e aprovação cruzada |

## Próxima execução reproduzível

1. Administrador configura o SonarCloud **sem publicar tokens no repositório** e informa organização/project key. Executar análise na candidata de release e anexar URL e estado do gate.
2. Responsável pelo ambiente autoriza explicitamente o teste de carga, define massa sintética, credenciais, taxa/usuários, duração e limites p95/p99/erro. Seguir [plano JMeter](../desempenho/PLANO_TESTE_CARGA.md); não executar carga contra produção ou URL pública sem autorização.
3. Auditar testes de relógio/fuso, TDD/BDD, API/E2E, RTM, riscos, defeitos, diagramas, responsáveis, branch protection e clone limpo. Registrar lacunas, não presumir conclusão.
4. Validar tudo no **mesmo SHA/tag final** e atualizar [decisão ENT-04](DECISAO_ENT04.md). Até lá, a decisão é **BLOQUEADO** e a issue #186 deve permanecer aberta.

Este registro distingue fatos observados de tarefas não executadas; não é substituto para os artefatos exigidos.
