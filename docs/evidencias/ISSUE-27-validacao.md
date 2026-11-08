# Evidência de validação: Issue #27 (RSV-05)

## Regras temporais

- início e término são obrigatórios;
- término precisa ser estritamente posterior ao início;
- uma reserva não pode iniciar antes do instante atual fornecido pelo `Clock`;
- a aplicação usa `Clock.systemUTC()`, e os testes usam `Clock.fixed()`;
- intervalos seguem a convenção semiaberta `[inicio, termino)`: o término é
  exclusivo. Assim, dois horários adjacentes não entram em conflito.

## Integração

`ReservationService.criar` executa a política temporal antes de persistir a
reserva. As entradas HTTP continuam sendo recebidas em `OffsetDateTime` e
normalizadas para `Instant`, tornando a comparação independente do fuso do
cliente.

## Testes

`ReservationTimePolicyTest` cobre nulos, igualdade, período invertido,
passado com relógio fixo, instantes em UTC e adjacência.

## Como validar

```bash
./mvnw --batch-mode --no-transfer-progress test \
  -Dtest=ReservationTimePolicyTest,ReservationServiceTest
```

## Escopo consolidado (#28 a #32)

A issue #27 passou a concentrar RSV-06, RSV-07, RSV-08, RSV-09 e RSV-10. A tabela
abaixo liga cada critério à automação e ao PR que o comprova.

| Item | Critério | Automação | PR |
|---|---|---|---|
| RSV-05 | Testes não dependem do relógio real | `RelogioFixoTestConfig` nos testes de integração de reserva (defeito D07) | #172 |
| RSV-06 | Mesmo intervalo e sobreposições parciais recusados, adjacentes aceitos, canceladas não bloqueiam | `ReservationConflitosIntegrationTest` (SALA) | #173 |
| RSV-07 | Professor não é reservado em dois locais, sala diferente não evita o conflito | `ReservationConflitosIntegrationTest` (PROFESSOR, sempre em outra sala) | #173 |
| RSV-08 | Quantidade reservada nunca supera o total, patrimônio único sem dupla reserva, cancelados liberam | `ReservationConflitosIntegrationTest` (MATERIAL e quantidades), `ReservationServiceTest` | #173 |
| RSV-09 | Todos os limites da fórmula com nomes legíveis | `ReservationConflitosIntegrationTest` (JUnit 5 parametrizado) | #173 |
| RSV-10 | Duas solicitações simultâneas não criam duas reservas | `ReservationConcorrenciaIntegrationTest`, `ReservationServiceTest`, `ReservationManagementServiceTest` | #174 |

## Suíte parametrizada de sobreposição (RSV-09)

Reserva base em `[10:00, 12:00)`. Cada caso é aplicado a sala, professor e
material (patrimônio único), totalizando 33 execuções:

| Caso | Intervalo tentado | Resultado esperado |
|---|---|---|
| mesmo intervalo | [10:00, 12:00) | conflito |
| contido | [10:30, 11:30) | conflito |
| contém | [09:00, 13:00) | conflito |
| sobrepõe o início | [09:00, 10:30) | conflito |
| sobrepõe o fim | [11:30, 13:00) | conflito |
| um minuto dentro do início | [09:00, 10:01) | conflito |
| um minuto dentro do fim | [11:59, 13:00) | conflito |
| adjacente antes | [09:00, 10:00) | aceito |
| adjacente depois | [12:00, 13:00) | aceito |
| totalmente antes | [07:00, 08:00) | aceito |
| totalmente depois | [14:00, 15:00) | aceito |

Também na suíte:

- estados: os 7 status × 3 recursos; só `CRIADA`, `APROVADA` e `EM_ANDAMENTO` bloqueiam;
- alteração: a reserva não conflita consigo mesma e não pode passar a sobrepor outra;
- API: conflito devolve 409 com código `CONFLITO_DE_ESTADO`;
- quantidades (total 5): exata, excesso e várias reservas somando.

Os nomes aparecem no relatório do Surefire, por exemplo
`PROFESSOR | contido [10:30, 11:30) -> conflito`, então uma falha indica o cenário.

**Decisão sobre patrimônio único:** não foi criada regra nova. Um equipamento
patrimoniado é cadastrado com quantidade total 1, e a regra de soma da RSV-08 já
impede uma segunda reserva sobreposta. O caso MATERIAL da suíte comprova isso.

## Exclusão mútua (RSV-10)

- Antes de carregar e validar, a criação e a alteração travam no banco
  (`SELECT ... FOR UPDATE`) a sala, o professor e os materiais pedidos.
- Uma reserva concorrente do mesmo recurso espera a primeira terminar e só então
  valida, já enxergando o que ela gravou: não existe janela entre validação e gravação.
- Como a trava vem antes da leitura, um recurso inativado por outra transação
  durante a espera é enxergado e a reserva é recusada (apontamento do Sourcery no PR #174).
- Os materiais são travados sempre em ordem de id, o que evita deadlock.

`ReservationConcorrenciaIntegrationTest` roda com PostgreSQL real e sem transação
de teste, em 5 cenários determinísticos:

1. mesma sala;
2. mesmo professor em salas diferentes;
3. mesmo patrimônio em salas diferentes;
4. sala inativada enquanto a reserva espera;
5. professor inativado enquanto a reserva espera.

Em cada um, a primeira transação grava e segura o commit, a segunda precisa ficar
esperando a trava e, depois do commit, é recusada. O banco termina com no máximo
uma reserva. Se a trava for removida ou vier depois da leitura, a segunda transação
não espera e o teste falha.

## Execução na CI

| PR | Commit | Run | Resultado |
|---|---|---|---|
| #172 | 23e33f4 | 42521237202 | verde |
| #173 | 8e13af9 | 42523486135 | 655 testes, 0 falhas, 0 pulados (`ReservationConflitosIntegrationTest`: 71) |
| #174 | 81f325e | 42525991845 | verde (`ReservationConcorrenciaIntegrationTest`: 5, `ReservationServiceTest`: 19, `ReservationManagementServiceTest`: 6) |

O relatório JaCoCo citado na RSV-09 depende da configuração da #53 (QA-06) e será
anexado quando ela for entregue.
