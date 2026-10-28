# Evidência parcial: Issue #25 (RSV-03)

## Entrega nesta branch

- contrato de alteração com sala, professor, período, finalidade e materiais;
- alteração permitida somente no estado `CRIADA`;
- substituição dos materiais com nova validação de ativo, duplicidade e quantidade;
- cancelamento lógico: a reserva continua persistida e recebe status `CANCELADA`;
- cancelamento recusado para reserva em uso ou terminal;
- autorização no serviço: dono, `RESPONSAVEL` ou `ADMINISTRADOR`; qualquer outro solicitante recebe 403;
- endpoints autenticados `PUT /api/reservas/{id}` e
  `POST /api/reservas/{id}/cancelar`, com proteção CSRF da sessão.

## Limite conhecido e dependências

A Issue #25 depende de RSV-06, RSV-07 e RSV-08. Portanto, esta branch **não
fecha a Issue #25** antes de integrar as verificações de:

- conflito de sala por sobreposição;
- conflito de professor por sobreposição;
- disponibilidade de materiais no período.

Sem essas regras, não é correto afirmar que toda alteração conflitante é
recusada. A PR deve ficar dependente da entrega dessas Issues, sem squash.

## Testes incluídos

`ReservationManagementServiceTest` cobre:

- alteração pelo dono enquanto a reserva está criada;
- bloqueio de dono diferente sem perfil operacional;
- cancelamento lógico com preservação do registro;
- recusa de cancelamento de reserva em andamento.

## Como validar

```bash
./mvnw --batch-mode --no-transfer-progress test \
  -Dtest=ReservationManagementServiceTest
```
