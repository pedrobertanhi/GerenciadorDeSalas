# Resultado — teste de carga baseline

- Data: 13/11/2026
- Ambiente: local autorizado
- Aplicação: GerenciadorDeSalas
- Banco: PostgreSQL local via Docker
- Cenário: login por sessão/CSRF e consulta de salas ativas (`GET /api/salas`)
- Usuários: 1
- Rampa: 1 segundo
- Duração: 60 segundos

## Resultados

| Métrica | Resultado |
|---|---:|
| Amostras totais | 12.742 |
| Erros | 0 (0,00%) |
| Média | 4,56 ms |
| Mediana | 4 ms |
| p95 | 6 ms |
| p99 | 7 ms |
| Máximo | 70 ms |
| Throughput | 212,54 requisições/s |

## Conclusão

O baseline foi executado com sucesso, sem erros. A autenticação por sessão e CSRF funcionou, e a consulta autenticada de salas ativas respondeu dentro dos tempos registrados acima.

## Disputa de reserva

Foram utilizadas duas contas sintéticas:

- `solicitante.demo@exemplo.test`
- `solicitante.carga2@exemplo.test`

Sala utilizada:

- Nome: Sala JMeter 01
- ID: 1
- Capacidade: 20
- Localização: Ambiente de testes local

Duas threads tentaram reservar a mesma sala no mesmo horário.

| Item | Valor |
|---|---|
| Usuários concorrentes | 2 |
| Sala | Sala JMeter 01 — ID 1 |
| Período | 18/12/2026, 14:00–15:00 |
| Horário enviado à API | `2026-12-18T17:00:00Z` até `2026-12-18T18:00:00Z` |

### Resultado da disputa

Uma thread criou a reserva com sucesso:

- Reserva ID: 2
- Status: APROVADA
- Finalidade: Disputa de reserva JMeter

A outra thread foi bloqueada pela regra de conflito:

- Código HTTP: 409
- Código interno: CONFLITO_DE_ESTADO
- Mensagem: A operacao nao pode ser concluida devido a um conflito de estado atual do recurso.

### Conclusão da disputa

Foi validado que duas reservas não conseguem usar a mesma sala no mesmo período: uma foi aceita e a outra foi recusada por conflito.

## Evidências

- `performance/gerenciador-reservas.jmx`
- `performance/usuarios-teste.csv`
- `performance/resultados-baseline.jtl`
- `performance/relatorio-baseline/index.html`

## Conclusão geral

O teste de baseline foi concluído com sucesso, sem erros, com p95 de 6 ms, p99 de 7 ms e throughput de 212,54 requisições por segundo no ambiente local.

Também foi validada a regra de concorrência de reservas: uma reserva foi aprovada e a outra tentativa para a mesma sala e horário retornou `409 CONFLITO_DE_ESTADO`.