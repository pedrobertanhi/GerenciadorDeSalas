# Testes de integração com PostgreSQL e Testcontainers

## Objetivo

Os testes de persistência usam a mesma família de banco da aplicação: PostgreSQL 16. O Testcontainers cria um banco descartável automaticamente, aplica as migrations do Flyway e elimina a necessidade de um PostgreSQL de teste configurado manualmente.

## Pré-requisitos

- Java 21;
- Docker em execução;
- Maven Wrapper incluído no repositório.

Não é necessário criar banco, usuário, senha ou volume para executar a suíte.

## Execução local

No Linux ou macOS:

```bash
./mvnw --batch-mode --no-transfer-progress verify
```

No Windows:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Os testes anotados com `@Testcontainers` iniciam a imagem `postgres:16-alpine`. As propriedades de conexão são fornecidas por `@DynamicPropertySource`, substituindo a URL do perfil `test` somente durante a execução.

## Cobertura da infraestrutura

| Verificação | Evidência automatizada |
|---|---|
| Container inicia sem PostgreSQL manual | `FlywayMigrationIntegrationTest` e `UserRepositoryIntegrationTest` |
| Flyway aplica a migration em banco vazio | `FlywayMigrationIntegrationTest` |
| Esquema é validado pelo Hibernate | `spring.jpa.hibernate.ddl-auto=validate` |
| Persistência usa PostgreSQL real | `UserRepositoryIntegrationTest` |
| Dados não vazam entre casos | rollback automático de `@DataJpaTest` |
| Execução ocorre no CI | job `Maven Verify` em `.github/workflows/ci.yml` |

## Isolamento

`UserRepositoryIntegrationTest` usa `@DataJpaTest`, portanto cada caso executa em transação revertida ao final. O teste de migration é somente leitura: consulta a tabela de histórico do Flyway e o catálogo do PostgreSQL.

Cada classe possui seu próprio container estático, encerrado automaticamente ao término da suíte.

## Diagnóstico

Se a suíte for ignorada por ausência de Docker, confirme que o daemon está disponível:

```bash
docker version
docker info
```

No CI oficial, o ambiente `ubuntu-24.04` disponibiliza Docker. A execução válida deve apontar para o commit da PR e terminar com o job `Maven Verify` aprovado.

## Limites atuais

Esta configuração atende ao estágio de instância única e aos testes de persistência existentes. Reuso global de container somente deverá ser adotado se o tempo da suíte se tornar um gargalo medido; antecipar essa otimização aumentaria o acoplamento entre testes.
