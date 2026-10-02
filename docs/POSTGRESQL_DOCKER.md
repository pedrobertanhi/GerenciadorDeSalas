# PostgreSQL local com Docker Compose

Este guia cobre a execução do banco de desenvolvimento da Issue #7 (BASE-03).

## Pré-requisitos

- Docker Desktop ou Docker Engine;
- Docker Compose v2;
- porta `5432` disponível.

## Configuração padrão

O ambiente funciona sem criar arquivos locais ou alterar credenciais.

| Configuração | Valor de desenvolvimento |
|---|---|
| Serviço | `postgres` |
| Imagem | `postgres:16-alpine` |
| Banco | `gerenciador_salas_dev` |
| Usuário | `postgres` |
| Senha | `postgres` |
| Porta local | `5432` |
| Volume | `postgres_data` |

Esses valores são exclusivos do ambiente local e não representam credenciais de produção. O arquivo `.env.example` documenta as variáveis que podem ser personalizadas.

## Iniciar o banco

Na raiz do repositório:

```bash
docker compose up -d --wait postgres
```

O parâmetro `--wait` só encerra quando o healthcheck do PostgreSQL estiver saudável.

## Verificar o estado e a conexão

```bash
docker compose ps
docker compose exec -T postgres pg_isready -U postgres -d gerenciador_salas_dev
docker compose exec -T postgres psql -U postgres -d gerenciador_salas_dev -c "SELECT 1;"
```

Resultado esperado:

- o serviço `postgres` aparece como `healthy`;
- o `pg_isready` responde `accepting connections`;
- a consulta retorna o valor `1`.

Para analisar falhas:

```bash
docker compose logs postgres
```

## Executar a aplicação

O perfil `dev` usa, por padrão, os mesmos dados do container:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

A criação das tabelas e migrations pertence à BASE-04 (#8). Até essa etapa, o PostgreSQL fica disponível e a aplicação utiliza os parâmetros corretos, mas a validação do JPA pode indicar ausência do esquema inicial.

## Parar ou recriar o ambiente

Parar preservando os dados:

```bash
docker compose stop
```

Parar e remover os containers, preservando o volume:

```bash
docker compose down
```

Recriar o banco do zero, removendo também o volume:

```bash
docker compose down -v
docker compose up -d --wait postgres
```

> O comando com `-v` apaga todos os dados locais do PostgreSQL.
