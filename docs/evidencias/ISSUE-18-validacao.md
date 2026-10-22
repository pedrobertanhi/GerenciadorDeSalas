\# Evidência de validação — Issue #18 (RES-04)



\## Escopo entregue



\- suíte de integração com Spring Boot completo, `MockMvc` com segurança e PostgreSQL 16 real via Testcontainers;

\- testes de API de ponta a ponta para salas, professores e competências, e materiais: criação, consulta, atualização, filtros, inativação e autorização por perfil;

\- testes diretos no banco das constraints das migrations V3, V4 e V5;

\- prova de isolamento entre testes;

\- mapeamento de `EntityNotFoundException` e `EntityExistsException` para 404 e 409 no `GlobalExceptionHandler`.



\## Testes localizáveis



\- `SalaIntegracaoTest`: salas, 11 casos;

\- `ProfessorIntegracaoTest`: professores e competências, 13 casos;

\- `MaterialIntegracaoTest`: materiais e quantidades, 15 casos;

\- `ConstraintsDoBancoTest`: 21 casos de erro (com o nome da constraint) e 4 casos válidos de controle;

\- `ProvaDeIsolamentoTest`: gravação repetida 3 vezes;

\- `ErroDeEntidadePersistenciaTest`: 404 e 409 das exceções de persistência, sem banco.



\## Como validar



```bash

./mvnw --batch-mode --no-transfer-progress verify

```



Os testes de integração exigem Docker. O workflow \*\*Maven Verify\*\* os executa com Java 21 e `postgres:16-alpine` via Testcontainers. Sem Docker, eles aparecem como ignorados e só os testes sem banco rodam.



\## Critérios de aceite cobertos



\- PostgreSQL containerizado: um único container `postgres:16-alpine`, compartilhado pela suíte (`PostgresCompartilhado`);

\- constraints relevantes comprovadas: cada caso de erro confere o nome da constraint que disparou;

\- isolamento: cada teste é transacional e desfeito, e antes de cada teste a base confere que as cinco tabelas estão vazias.



\## Resultado



A registrar com o link da execução do \*\*Maven Verify\*\* do commit mesclado. Até lá, a coluna de execução do RF-02 na RTM permanece PENDENTE.

