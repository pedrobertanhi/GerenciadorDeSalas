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
