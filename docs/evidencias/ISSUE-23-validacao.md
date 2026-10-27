# Evidência de validação: Issue #23 (RSV-01)

## Escopo entregue

- entidade `Reservation` (`tb_reservas`) com relação para `User` (solicitante), `Sala`, `Professor` opcional, materiais com quantidade, período semiaberto `[inicio, termino)`, status controlado pelo domínio e coluna `versao` para controle de concorrência otimista;
- entidade `ReservationMaterial` (`tb_reservas_materiais`) com unicidade por `(reserva_id, material_id)` e `quantidade > 0`, ligada à `Reservation` com `cascade = ALL` e `orphanRemoval = true`;
- enum `StatusReserva` com as regras de transição do agregado: `CRIADA` ↠ `APROVADA`, `REJEITADA` ou `CANCELADA`; `APROVADA` ↠ `EM_ANDAMENTO` ou `CANCELADA`; `EM_ANDAMENTO` ↠ `CONCLUIDA`. `REJEITADA`, `CANCELADA` e `CONCLUIDA` são finais;
- `ReservationRepository` com consultas de sobreposição por sala e por professor que já excluem status terminais, pensadas para RSV-06 (#28) e RSV-07 (#29);
- migration V7 com as duas tabelas, FKs para `tb_usuarios`, `salas`, `tb_professores` e `tb_materiais`, `CHECK (termino > inicio)`, `CHECK` do status, índices para consultas por período e por status, e `ON DELETE CASCADE` nos materiais da reserva.

## Decisões de modelo

- **Solicitante e sala obrigatórios**: a reserva sem um dos dois não tem sentido no projeto; o construtor rejeita.
- **Professor opcional**: cobre reuniões administrativas; FK aceita `NULL`.
- **Materiais opcionais, zero ou vários**: a reserva pode não precisar de material; quando precisar, cada linha carrega sua quantidade.
- **Status inicial sempre `CRIADA`**: posto no construtor, nunca aceito como parâmetro; a única forma de mudar é `transicionarPara`, que delega a regra ao enum.
- **Período em `Instant`**: mantém o padrão do `IntervaloDisponibilidade` (RES-06) e permite comparar com os `TIMESTAMP WITH TIME ZONE` do banco.
- **Concorrência otimista com `@Version`**: tanto `Reservation` quanto `ReservationMaterial` carregam `versao`, consistente com a tabela `tb_bloqueios_manutencao` (V6).

## Testes

- `StatusReservaTest`: 15 casos, regras de transição e idempotência dos finais.
- `ReservationTest`: 14 casos, construtor, validação de período, transição de status, materiais (adicionar, remover, quantidade inválida), lista imutável, redefinição de período.
- `ReservationRepositoryIntegrationTest`: 8 casos com PostgreSQL real via Testcontainers. Persistência mínima e completa, incremento de versão, orphan removal, consultas de sobreposição por sala (dentro, antes, depois e cancelada excluída), consulta por professor, ordenação por solicitante.

## Como validar

Comando: `./mvnw --batch-mode --no-transfer-progress verify`.

Localmente sem Docker, os testes de domínio rodam direto:

```bash
./mvnw test -Dtest=StatusReservaTest,ReservationTest
```

O teste de integração exige Docker; sem ele, aparece como ignorado.

## Resultado

- `StatusReservaTest` e `ReservationTest`, execução local nos commits `252ab66` e `610d7e3`: 29 testes executados, 0 falhas e 0 ignorados.
- `ReservationRepositoryIntegrationTest`, execução final no **Maven Verify** do PR: a registrar após abrir o PR com todos os commits. Até lá, a coluna de execução do RF-04 na RTM permanece `PENDENTE`.