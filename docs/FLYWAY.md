# Flyway e versionamento do banco

Este guia documenta a configuração da BASE-04 (#8) e a criação das próximas migrations do projeto.

## Funcionamento

O Flyway executa automaticamente as migrations de `src/main/resources/db/migration` durante a inicialização da aplicação. Não é necessário executar scripts SQL manualmente.

A primeira migration é:

```text
V1__create_initial_schema.sql
```

Ela cria a tabela inicial `tb_salas`. O Flyway registra a execução na tabela `flyway_schema_history`, preservando versão, checksum, data e resultado.

## Convenção de nomes

Use o formato:

```text
V<numero>__<descricao_em_snake_case>.sql
```

Exemplos:

```text
V2__create_users_and_roles.sql
V3__add_room_active_status.sql
V4__create_professors.sql
```

Regras obrigatórias:

- usar versões crescentes e nunca repetir um número;
- utilizar dois sublinhados entre a versão e a descrição;
- escrever a descrição em inglês e `snake_case`;
- criar uma migration para cada alteração independente de esquema;
- nunca editar uma migration que já tenha sido aplicada;
- corrigir uma estrutura existente com uma nova migration;
- não usar `IF NOT EXISTS` para esconder divergências de esquema;
- não armazenar senha, token ou credencial nos scripts.

## Executar localmente

Na raiz do repositório:

```bash
docker compose up -d --wait postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No Windows:

```powershell
docker compose up -d --wait postgres
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Ao iniciar, a aplicação conecta ao PostgreSQL configurado pela BASE-03 e aplica automaticamente as migrations pendentes.

## Verificar o histórico

```bash
docker compose exec -T postgres psql -U postgres -d gerenciador_salas_dev -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

O resultado esperado inclui a versão `1`, a descrição `create initial schema` e `success = true`.

## Validar em banco limpo

> O comando com `-v` remove todos os dados locais do PostgreSQL.

```bash
docker compose down -v
docker compose up -d --wait postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Depois, confirme as tabelas:

```bash
docker compose exec -T postgres psql -U postgres -d gerenciador_salas_dev -c "\dt"
```

Devem existir `flyway_schema_history` e `tb_salas`.

## Teste automatizado

```bash
./mvnw -B -Dtest=FlywayMigrationIntegrationTest test
```

O teste utiliza um PostgreSQL 16 limpo em container, inicia a aplicação, aplica a migration e verifica:

- registro bem-sucedido da versão `1` em `flyway_schema_history`;
- criação da tabela `tb_salas`.

Se o Docker não estiver disponível, esse teste de integração é ignorado; no ambiente de CI com Docker ele deve ser executado.
