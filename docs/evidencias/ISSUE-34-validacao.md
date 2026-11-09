# Evidência de validação: Issue #34 (APR-01, consolida APR-02, APR-03 e UI-11)

A issue #34 concentra a fila de recursos restritos (APR-01), a aprovação e rejeição (APR-02, antiga #35),
a validação docente pelo Responsável (APR-03, antiga #61) e a tela de aprovações (UI-11, antiga #69).

## Decisões de projeto

| Tema | Decisão | Motivo |
|---|---|---|
| Nome do estado inicial | `CRIADA` corresponde ao `SOLICITADA` de `docs/arquitetura/ESTADOS_DA_RESERVA.md` | Renomear afetaria código, banco e testes de outros integrantes sem ganho funcional |
| Reserva restrita | Reserva que usa material com `restrito = true` | Único recurso que já tem o campo; sala restrita fica para issue futura |
| Validação docente | Exigida em toda reserva com professor | Permite ao Responsável conferir professor e competências antes do uso |
| Reserva comum | Sem material restrito e sem professor, nasce `APROVADA` (origem: sistema) | Segue o documento de estados; antes, toda reserva ficava `CRIADA` para sempre |
| Duas pendências | A reserva só fica `APROVADA` quando aprovação de recurso e validação docente estão resolvidas; qualquer rejeição leva a `REJEITADA` | Decisões independentes, cada uma com autor, instante e motivo |
| Alteração depois da decisão | Exige nova decisão e limpa o registro anterior | A aprovação vale para o pedido decidido, não para um pedido alterado |
| Auditoria e notificação | Cada decisão publica `ReservaDecididaEvent` | AUD-01 (#39) e NOT-01 (#41) ainda não existem; o evento evita acoplamento |

## Entregas

| PR | Item | Conteúdo |
|---|---|---|
| #176 | APR-01 | V9 com `aprovacao_recurso` e `validacao_docente`; regra de nascimento e reavaliação na alteração; fila `GET /api/aprovacoes` só para RESPONSAVEL, ordenada pela data da solicitação |
| #177 | APR-02 | V10 com autor, instante e motivo da decisão de recurso; `POST /api/reservas/{id}/aprovar` e `/rejeitar`; revalidação com travas; evento de decisão |
| #178 | APR-03 | V11 com autor, instante e motivo da validação docente; `POST /api/reservas/{id}/validar-docente` e `/rejeitar-docente`; competências na fila; bloqueio de manutenção passa a travar o recurso (defeito D08) |
| #180 | UI-11 | Telas `/aprovacoes` (fila) e `/aprovacoes/{id}` (decisão), só para RESPONSAVEL, com erros de conflito e autorização |

## Critérios de aceite

| Critério | Como foi atendido | Teste |
|---|---|---|
| Reserva restrita nasce SOLICITADA | Nasce `CRIADA` com `aprovacao_recurso = PENDENTE` | `ReservationServiceTest`, `AprovacaoFilaIntegrationTest` |
| Reserva comum segue regra definida | Nasce `APROVADA` sem passar pela fila | `ReservationTest`, `AprovacaoFilaIntegrationTest`, `ReservasNavegadorTest` |
| Somente o Responsável acessa pendências e decide | `SecurityConfig` (API e telas) e conferência de perfil no serviço | `AprovacaoFilaIntegrationTest`, `AprovacaoDecisaoIntegrationTest`, `AprovacaoDocenteIntegrationTest`, `AprovacaoWebIntegrationTest`, `AprovacoesNavegadorTest` |
| Fila exibe solicitante, período e recursos, ordenada pela solicitação | `AprovacaoDto.ItemFila` com solicitante, sala, professor, competências, período e materiais | `AprovacaoFilaIntegrationTest`, `AprovacaoDocenteIntegrationTest` |
| Decisão muda para o estado correto | `APROVADA` quando não resta pendência, `CRIADA` se ainda houver outra, `REJEITADA` na rejeição | `ReservationTest`, `AprovacaoServiceTest` |
| Motivo de rejeição é preservado | Colunas com CHECK de motivo não vazio, devolvido na API e mostrado na tela | `AprovacaoDecisaoIntegrationTest`, `AprovacaoDocenteIntegrationTest`, `AprovacaoWebIntegrationTest` |
| Decisão repetida é impedida | 409 na API e mensagem de erro na tela | `AprovacaoServiceTest`, `AprovacaoDecisaoIntegrationTest`, `AprovacaoWebIntegrationTest` |
| Conflito surgido antes da decisão impede a aprovação ou validação | Revalidação de recurso ativo, manutenção, sobreposição e saldo, com travas | `AprovacaoServiceTest`, `AprovacaoDecisaoIntegrationTest`, `AprovacaoDocenteIntegrationTest` (reserva sobreposta e manutenção), `ReservationConcorrenciaIntegrationTest` |
| Mudança gera auditoria | Evento `ReservaDecididaEvent` publicado em cada decisão; a gravação na trilha depende da #39 | `AprovacaoServiceTest` |
| Interface funciona em desktop e mobile | Bootstrap responsivo, sem rolagem horizontal, campos e botões dentro da janela | `AprovacoesNavegadorTest` (celular, tablet e desktop) |

## Concorrência

- Ordem global das travas em todo o sistema: reserva, sala, professor, materiais por id. Criação, alteração e
  decisões seguem essa ordem, o que evita deadlock.
- Decisões simultâneas sobre a mesma reserva acontecem uma de cada vez: a segunda enxerga a primeira e recebe 409.
- Criar ou alterar bloqueio de manutenção trava o recurso (apontamento do Sourcery no PR #178). Um bloqueio criado
  durante a validação docente faz a validação esperar e, depois do commit, recusar.

## Execução na CI

| PR | Commit | Run | Resultado |
|---|---|---|---|
| #176 | 40fd21c | 42530362488 | verde |
| #177 | 905dd8b | 42532693223 | 714 testes, 0 falhas |
| #178 | 6b07a5b | 42535697997 | verde |
| #180 | 98c86e5 | 42711607822 | verde, incluindo os testes E2E no navegador |

## Roteiro de validação manual

1. Entrar como SOLICITANTE e criar uma reserva com material restrito e outra com professor.
2. Entrar como RESPONSAVEL e abrir `/aprovacoes`: as duas aparecem, da mais antiga para a mais recente.
3. Em **Decidir**, rejeitar a primeira sem motivo: a tela pede o motivo. Rejeitar com motivo: mensagem de sucesso.
4. Na segunda, clicar em **Validar docente**: a reserva sai da fila e fica `APROVADA`.
5. Entrar como SOLICITANTE ou ADMINISTRADOR e abrir `/aprovacoes`: acesso negado.

## Pendências fora do escopo

- Gravação da decisão na trilha de auditoria: AUD-01 (#39).
- Notificação ao solicitante: NOT-01 (#41).
- Sala restrita: depende de nova coluna em `salas`, a ser tratada em issue própria.
