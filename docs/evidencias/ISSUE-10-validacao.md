# Evidência de validação: Issue #10 (BASE-06)

## Escopo entregue

- `OpenApiConfig` já estava configurado no projeto (commit da #16) com titulo "Gerenciador de Salas", versao "v1" e `@SecurityScheme` "sessao" apontando para o cookie `JSESSIONID`.
- `MaintenanceBlockController`, `MaterialController` e `ProfessorController` já estavam anotados com `@Tag`, `@Operation` e `@ApiResponse` por entregas anteriores.
- Nesta Issue, `SalaController` e `ConsultaDisponibilidadeController` foram anotados no mesmo padrão: `@Tag` da classe, `@SecurityRequirement("sessao")`, respostas padronizadas 401/403/500 e, por operação, descrição, código de sucesso e erros específicos (400, 404, 409, 503) com `schema` apontando para `ErroResposta` ou para o DTO do próprio contrato.
- `/v3/api-docs` e `/swagger-ui/**` continuam liberados somente para usuário autenticado, conforme a regra já presente em `SecurityConfig`.

## Como funciona

- Após o login, o usuário acessa `/swagger-ui/index.html` e navega pelos endpoints documentados por sessão.
- O documento JSON está em `/v3/api-docs`, com `openapi 3.x`, metadados em `info`, esquema de segurança em `components.securitySchemes.sessao` e um bloco `paths` por operação REST documentada.

## Testes

- `OpenApiDocumentoTest`: 3 casos com Spring Boot completo e PostgreSQL real via Testcontainers (`IntegracaoPostgresBase`), cobrindo:
  - geração do documento com título, versão e esquema de segurança esperado;
  - presença das rotas REST principais (`/api/salas`, `/api/materiais`, `/api/professores`, `/api/disponibilidade/consulta`);
  - resposta HTML válida do Swagger UI em `/swagger-ui/index.html`.

## Como validar

Comando: `./mvnw --batch-mode --no-transfer-progress verify`.

Sem Docker, o teste aparece como ignorado. Com Docker, roda via Testcontainers igual aos demais testes de integração.

Para validação manual: subir a aplicação (`./mvnw spring-boot:run`), autenticar em `/login` e acessar `/swagger-ui/index.html`.

## Resultado

- `OpenApiDocumentoTest` registra o funcionamento do documento no **Maven Verify** do PR; o link da execução fica a cargo do CI após o push.
- Até o merge, a coluna de execução do RF-14 na RTM permanece `PENDENTE`.


## Round 2: correções apontadas pela revisão

- `SalaService.buscarPorId` passou a filtrar inativas: `GET /api/salas/{id}` de sala inativa agora responde 404 (consistente com a documentação). `inativar` foi ajustado para buscar via `findById` direto, para ainda encontrar a sala a ser inativada.
- `SalaIntegracaoTest` ajustado para o novo contrato: o teste `inativarSala_preservaRegistroEHistorico` passa a esperar 404 no GET após a inativação, com `codigo = RECURSO_NAO_ENCONTRADO`.
- `ConsultaDisponibilidadeController`: `inicio` e `termino` marcados como `required = true` nos `@RequestParam`, deixando o contrato OpenAPI coerente com a validação do controller.
- `SalaController`: adicionadas respostas 400 em `pesquisar` (filtros/ordenação inválidos), `buscarPorId` (ID inválido) e `inativar` (ID inválido).
- `OpenApiConfig`: adicionado `@SecurityScheme` "csrf" para o header `X-CSRF-TOKEN`.
- `SalaController`, `MaterialController`, `ProfessorController` e `MaintenanceBlockController`: operações POST/PUT/PATCH ganharam `@SecurityRequirement(name = "csrf")`, permitindo que o Swagger UI envie o token em operações de escrita.