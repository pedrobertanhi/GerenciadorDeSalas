# Evidência de validação — Issue #25 (RSV-03, RSV-04, UI-09 e UI-10)

## Escopo entregue

- criação de reserva pelo solicitante, com sala, professor opcional, período, finalidade e até três materiais;
- consulta do histórico e do detalhe da reserva conforme o perfil; o solicitante vê as próprias reservas e os perfis operacionais podem atuar conforme a regra de propriedade;
- alteração apenas da reserva no estado `CRIADA`, reaplicando validação temporal, conflitos de sala/professor/material e bloqueios de manutenção;
- cancelamento com confirmação, sem exclusão física nem remoção de materiais ou histórico;
- máquina de estados centralizada: `CRIADA → APROVADA/REJEITADA/CANCELADA`, `APROVADA → EM_ANDAMENTO/CANCELADA/NAO_COMPARECEU` e `EM_ANDAMENTO → CONCLUIDA`.

## Cenários automatizados

- `ReservationManagementServiceTest`: dono, perfil operacional, usuário indevido, alteração em estado inválido, conflito e cancelamento preservando o agregado;
- `ReservationStatusServiceTest` e `StatusReservaTest`: matriz de transições válidas e inválidas, incluindo estados finais;
- `ReservationServiceTest`: criação, disponibilidade de material e recusa de recurso em manutenção;
- `ReservationCriacaoIntegrationTest` e `ReservationConstraintsTest`: persistência, constraints e regressão com PostgreSQL.

## Roteiro manual

1. Entrar como solicitante e abrir `/reservas/nova`.
2. Criar reserva válida e conferir o status `CRIADA` no detalhe.
3. Alterar horário, finalidade ou recurso enquanto o estado for `CRIADA`; testar período conflitante e bloqueio de manutenção para confirmar a mensagem sem perda do formulário.
4. Abrir a confirmação de cancelamento e cancelar; conferir que o detalhe e a lista continuam exibindo o registro como `CANCELADA`.
5. Tentar editar ou cancelar uma reserva `EM_ANDAMENTO` ou final e conferir o bloqueio.
6. Entrar com outro solicitante e confirmar que a reserva de terceiro não é acessível.
7. Conferir em 390 px, 768 px e 1280 px que os formulários e a tabela continuam utilizáveis.

## Resultado esperado da CI

Executar `./mvnw --batch-mode --no-transfer-progress verify`. A execução do PR deve anexar o link verde do **Maven Verify** e não pode ter testes ignorados.

## Decisões

- O período usa `America/Sao_Paulo` na interface e instantes UTC no domínio.
- Cancelamento é uma transição de estado; não existe endpoint nem botão de exclusão física.
- As validações de manutenção são feitas no serviço, portanto valem tanto para API quanto para telas.
