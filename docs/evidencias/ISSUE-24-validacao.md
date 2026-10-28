# Evidência de validação: Issue #24 (RSV-02)

## Escopo entregue

- Migration V8 adiciona a coluna `finalidade` em `tb_reservas` como `VARCHAR(500) NOT NULL`, com `CHECK (finalidade ~ '[^[:space:]]')` para rejeitar valores em branco.
- Entidade `Reservation` passa a exigir `finalidade` no construtor; `definirFinalidade` valida obrigatoriedade e aplica `trim`.
- `ReservationDto` com `Entrada` (sala, professor opcional, período em `OffsetDateTime`, finalidade, lista de materiais pedidos), `MaterialPedido` e `Saida` (representação segura, com IDs e materiais).
- `ReservationService.criar(Entrada, emailSolicitante)` com validações: solicitante existe e está ativo, sala ativa, professor ativo (quando informado), materiais ativos, material duplicado no pedido rejeitado, quantidade acima do saldo total rejeitada.
- `ReservationController` expõe `POST /api/reservas`, exclusivo do SOLICITANTE, com `@AuthenticationPrincipal` para pegar o email autenticado. Retorna 201 Created com `Location` e corpo `Saida`.

## Fluxo

1. SOLICITANTE autenticado chama `POST /api/reservas` com JSON contendo `salaId`, `inicio`, `termino`, `finalidade` (obrigatório) e `materiais` (opcional).
2. Service busca solicitante pelo email autenticado, valida sala ativa, professor ativo (se informado), materiais ativos e dentro do saldo.
3. Reserva nasce em `CRIADA`, aguardando o fluxo de aprovação (RSV-04 e APR-01).
4. Resposta 201 traz o ID novo, dono (solicitanteId), status e materiais reservados.

## Testes

- `ReservationServiceTest`: 12 casos unitários cobrindo happy path, sem professor, 404 em solicitante/sala/professor/material (inexistente e inativo), 409 em material duplicado e quantidade acima do saldo.
- `ReservationCriacaoIntegrationTest`: 6 casos ponta a ponta com PostgreSQL real via Testcontainers: SOLICITANTE cria com 201, sem autenticação 401, RESPONSAVEL e ADMINISTRADOR recebem 403, corpo sem finalidade recebe 400, sala inexistente recebe 404, quantidade acima do saldo recebe 409.
- `ReservationTest`, `ReservationRepositoryIntegrationTest` e `ReservationConstraintsTest` foram ajustados para o novo construtor com finalidade (passam `"Reserva de teste"` por padrão) e cobrem a nova constraint `ck_reservas_finalidade`.

## Como validar

Comando: `./mvnw --batch-mode --no-transfer-progress verify`.

Sem Docker, os testes de domínio e os unitários rodam direto:

```bash
./mvnw test -Dtest=ReservationTest,ReservationServiceTest
```

Os testes de integração exigem Docker e aparecem como ignorados quando não disponível.

## Resultado

- `ReservationTest` e `ReservationServiceTest`, execução local: 26 testes executados, 0 falhas.
- `ReservationCriacaoIntegrationTest`, `ReservationRepositoryIntegrationTest` e `ReservationConstraintsTest`: a registrar na execução do Maven Verify do PR.
- Até o merge, a coluna de execução do RF-04 na RTM permanece `PENDENTE`.