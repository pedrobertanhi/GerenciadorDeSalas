# Evidência de validação — Issue #17 (RES-03)

## Escopo entregue

- entidade e migration `Material`;
- classificação como `MATERIAL` ou `EQUIPAMENTO`;
- patrimônio opcional, normalizado e único quando informado;
- quantidade total não negativa;
- recurso restrito e estado ativo;
- criação, consulta, atualização, filtros e desativação lógica via API REST;
- leitura para os três perfis autenticados e escrita exclusiva do Administrador;
- contratos OpenAPI dos cinco endpoints.

As telas não fazem parte desta entrega: estão planejadas separadamente na Issue #66.

## Testes localizáveis

- `MaterialServiceTest`: regras de negócio, normalização, duplicidade e desativação;
- `MaterialDtoValidationTest`: quantidade e campos obrigatórios;
- `MaterialControllerSecurityTest`: matriz dos três perfis, autenticação e CSRF;
- `MaterialRepositoryIntegrationTest`: migration PostgreSQL, filtros e constraints;
- `MaterialOpenApiTest`: contrato publicado da API.

## Como validar

```bash
./mvnw --batch-mode --no-transfer-progress verify
```

O workflow **Maven Verify** executa o comando com Java 21. Os testes de persistência usam PostgreSQL 16 via Testcontainers.

## Critérios de aceite cobertos

- quantidade negativa é rejeitada na entrada, no domínio e no banco;
- patrimônio informado é único, inclusive sob concorrência protegida pela constraint;
- desativação mantém o registro e remove o recurso das consultas ativas;
- tentativas de escrita por Solicitante, Responsável ou sem CSRF não chamam o serviço;
- não existe endpoint de exclusão física.
