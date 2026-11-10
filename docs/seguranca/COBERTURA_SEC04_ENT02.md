# Cobertura da SEC-04 na ENT-02

Comparação entre o [rascunho #179](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/179) (`b813391`) e a candidata histórica `a5b38c7`. O [Maven Verify da candidata](https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42741507363) executou 835 testes, sem falhas, erros ou ignorados. Os testes adicionados depois desse SHA exigem nova evidência de CI.

| Critério do rascunho | Evidência em a5b38c7 | Encaminhamento |
|---|---|---|
| Negar consulta de auditoria a SOLICITANTE e RESPONSAVEL | `AutorizacaoPorPerfilWebTest` comprova leitura por ADMINISTRADOR e negação de escrita manual; os dois cenários de leitura negada do #179 ainda não estavam nessa classe | Dois testes explícitos adicionados neste PR; comprovação depende do CI |
| Negar administração de usuários ao RESPONSAVEL | `UsuarioControllerSecurityTest.outrosPerfisNaoAcessamNenhumaRotaDeUsuarios` cobre também SOLICITANTE. A classe executou 20 casos | Reutilizar evidência existente |
| Negar criação de reserva ao ADMINISTRADOR | `ReservationCriacaoIntegrationTest.perfilSemPermissaoDeCriacao_recebe403` cobre também RESPONSAVEL. A classe executou 9 casos | Reutilizar evidência existente |
| Autorização horizontal de reserva | `ReservationManagementServiceTest.usuarioDiferente_semPerfilOperacional_naoAlteraNemCancela`. A classe executou 8 casos | A evidência comprova a regra no serviço. A [matriz de rotas](MATRIZ_DE_ROTAS.md) mantém a lacuna de cobertura HTTP por operação e perfil |
| Sessão, entrada inválida e erros neutros | `AutenticacaoWebTest`, `FalhasSegurancaTest`, `UsuarioControllerSecurityTest` e `Erro500InternoTest` | Evidência funcional disponível; revisão de logs e artefatos permanece separada |
| CodeQL, Trivy filesystem e ZAP público | Execuções vinculadas à candidata no [parecer](PARECER_DE_SEGURANCA.md) | ZAP autenticado e administrativo ainda exige execução ou justificativa formal |
| Trivy Image e ocorrências históricas | O [relatório do #179](https://github.com/pedrobertanhi/GerenciadorDeSalas/blob/b8133911b21ca206b4afb44352224a64adb52dea/docs/seguranca/EVIDENCIAS_SEC04.md) registrou falha de imagem em outros SHAs. A candidata tem evidência de filesystem | Confirmar as versões resolvidas no artefato candidato e avaliar os achados nessa versão. O filesystem verde não substitui a evidência de imagem |
| Secret Scanning e Dependency Review | Capturas do repositório e execuções de Dependency Review vinculadas aos SHAs dos PRs | Completar a rastreabilidade solicitada na [revisão da issue #184](https://github.com/pedrobertanhi/GerenciadorDeSalas/issues/184#issuecomment-6766224281) |

A migração de Spring Boot 4 do rascunho #179 fica fora deste recorte. A candidata histórica usa Spring Boot 3.5.16. O teste preventivo `RegressaoRecursosSpringMvcTest` do rascunho não foi executado nessa candidata, e sua inclusão não corrigiria uma versão afetada. Antes de liberar, conferir os componentes resolvidos contra os avisos oficiais e registrar o resultado da triagem. [CVE-2026-47884](https://spring.io/security/cve-2026-47884/) e [CVE-2026-47890](https://spring.io/security/cve-2026-47890/).

A decisão de liberação continua pendente na [issue #184](https://github.com/pedrobertanhi/GerenciadorDeSalas/issues/184).
