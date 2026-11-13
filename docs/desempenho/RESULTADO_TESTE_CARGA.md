# Resultado — teste de carga baseline

- Data: 13/11/2026
- Ambiente: local autorizado
- Aplicação: GerenciadorDeSalas
- Banco: PostgreSQL local via Docker
- Cenário: login por sessão/CSRF e consulta de salas ativas (`GET /api/salas`)
- Usuários: 1
- Rampa: 1 segundo
- Duração: 60 segundos

## Resultados comprovados

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

O arquivo `performance/resultados-baseline.jtl` contém as 12.742 amostras, todas com HTTP 200, do cenário de login e consulta autenticada. O relatório HTML foi gerado a partir do mesmo JTL.

## Cenário de disputa de reserva

O plano `performance/gerenciador-reservas.jmx` também contém um cenário de duas contas sintéticas tentando reservar a mesma sala e o mesmo horário, com sincronização entre as threads.

Contudo, o JTL versionado nesta entrega contém somente o baseline. Portanto, **não se declara resultado da disputa** até que seja anexado um JTL/relatório que inclua as duas requisições de reserva e seus códigos de resposta. Isso evita atribuir uma evidência inexistente ao JMeter.

## Evidências

- `performance/gerenciador-reservas.jmx`
- `performance/usuarios-teste.csv`
- `performance/resultados-baseline.jtl`
- `performance/relatorio-baseline/index.html`

## Limites e interpretação

Este é um baseline local, com um único usuário e PostgreSQL local. Ele comprova funcionamento e desempenho naquele ambiente, mas não representa capacidade de produção nem substitui um ensaio com concorrência maior.