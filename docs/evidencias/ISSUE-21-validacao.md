# Evidência de validação — Issue #21 (MNT-01)

## Escopo entregue

- migration `V6__create_bloqueios_manutencao.sql` com restrições de integridade no banco;
- entidade `MaintenanceBlock` com regras de período e de estado, e repositório sem exclusão;
- serviço que confere a existência do recurso (sala, professor ou material) antes de gravar;
- API `/api/manutencoes`: registrar, alterar, concluir, cancelar e consultar com filtros por tipo, recurso e estado;
- autorização: somente administrador gerencia bloqueios, todos os perfis consultam, e `DELETE` é negado;
- histórico preservado: concluídos e cancelados permanecem consultáveis.

## Testes localizáveis

Sem banco, 54 casos:

- `MaintenanceBlockTest`: período, estados e sobreposição, 15 casos;
- `BloqueioDtoValidationTest`: período, motivo e recurso, 11 casos;
- `MaintenanceBlockServiceTest`: serviço com relógio fixo, 12 casos;
- `MaintenanceBlockRepositoryContractTest`: repositório sem exclusão, 2 casos;
- `MaintenanceBlockControllerSecurityTest`: autorização por perfil, validação e filtros, 14 casos.

Com PostgreSQL em container, 34 casos:

- `BloqueioManutencaoIntegracaoTest`: ponta a ponta, 17 casos;
- `ConstraintsBloqueiosManutencaoTest`: 11 casos de erro com o nome da constraint, 5 casos válidos e os valores padrão.

## Como validar

Comando: `./mvnw --batch-mode --no-transfer-progress verify`.

Os testes de integração exigem Docker. O workflow **Maven Verify** os executa com Java 21 e `postgres:16-alpine` via Testcontainers. Sem Docker, eles aparecem como ignorados.

## Critérios de aceite cobertos

- somente administrador gerencia bloqueios: `MaintenanceBlockControllerSecurityTest` e `BloqueioManutencaoIntegracaoTest`;
- período inválido é recusado: validação do DTO, entidade e constraint `ck_bloqueios_periodo`;
- histórico de manutenção é preservado: sem exclusão no repositório, `DELETE` negado e bloqueios encerrados que continuam consultáveis.

## Decisões para o grupo

- bloqueios sobrepostos no mesmo recurso são aceitos;
- o bloqueio é do recurso inteiro, sem quantidade parcial de material;
- a integração com a reserva e com a consulta de disponibilidade fica para a MNT-02;
- a consulta é ordenada pelo início do período, do mais recente ao mais antigo, e o filtro por recurso exige o tipo;
- a criação não é idempotente, como nos demais cadastros: uma repetição de requisição gera outro bloqueio, que o administrador cancela e que permanece como histórico;
- a edição não exige a versão lida pelo cliente, como nos demais cadastros: duas telas abertas podem sobrescrever uma à outra, e a solução é uma Issue transversal (ETag ou If-Match);
- um bloqueio cujo fim passa entre a validação e a gravação é aceito e não impede nenhum período futuro.

## Resultado

A registrar com o link da execução do **Maven Verify** do commit mesclado.