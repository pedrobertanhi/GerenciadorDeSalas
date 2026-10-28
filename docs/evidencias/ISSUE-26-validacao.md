# Evidência de validação: Issue #26 (RSV-04)

## Decisão de compatibilidade

O projeto já utiliza `StatusReserva` persistido na coluna `tb_reservas.status`.
Por isso, a Issue não cria uma enumeração paralela chamada `ReservationStatus`.
A máquina de estados foi centralizada na enumeração existente para preservar o
contrato de banco e das APIs já implementadas.

## Matriz de transições

| Estado atual | Próximos estados permitidos |
|---|---|
| `CRIADA` | `APROVADA`, `REJEITADA`, `CANCELADA` |
| `APROVADA` | `EM_ANDAMENTO`, `CANCELADA`, `NAO_COMPARECEU` |
| `EM_ANDAMENTO` | `CONCLUIDA` |
| `REJEITADA` | nenhum (terminal) |
| `CANCELADA` | nenhum (terminal) |
| `CONCLUIDA` | nenhum (terminal) |
| `NAO_COMPARECEU` | nenhum (terminal) |

## Implementação

- `StatusReserva` mantém a matriz imutável de regras e expõe apenas consulta;
- `Reservation#transicionarPara` é o único método do agregado que altera
  status; não existe setter público;
- `ReservationStatusService` carrega a reserva, delega a regra ao agregado e
  persiste a transição dentro de transação;
- transição inválida gera `ConflitoDeEstadoException` com a origem e o
  destino na mensagem; reserva inexistente gera `RecursoNaoEncontradoException`.

A autorização de quem pode disparar cada transição será aplicada pelos casos de
uso de aprovação e operação posteriores; esta entrega não expõe endpoint novo
nem contorna a política de perfis existente.

## Testes

- `StatusReservaTest`: testa todos os pares de estados (matriz completa),
  incluindo as recusas e a impossibilidade de avanço dos estados finais;
- `ReservationStatusServiceTest`: testa transição válida, recusa de
  transição inválida e reserva inexistente.

## Como validar

```bash
./mvnw --batch-mode --no-transfer-progress test \
  -Dtest=StatusReservaTest,ReservationStatusServiceTest
```

A suíte completa permanece disponível com:

```bash
./mvnw --batch-mode --no-transfer-progress verify
```
