# ADR-002: Interface web e documentação OpenAPI

- **Status:** Aceita
- **Data:** 26/08/2026
- **Issue relacionada:** #1

## Contexto

O sistema deve possuir interface responsiva, mensagens de erro compreensíveis e documentação dos fluxos públicos ou da API. A solução deve permanecer integrada ao Spring Boot e evitar complexidade desnecessária.

## Decisão

A camada de apresentação utilizará:

- Thymeleaf para renderização das páginas no servidor;
- Bootstrap 5.x para responsividade e componentes visuais;
- OpenAPI 3 para especificação dos endpoints;
- Swagger UI para consulta da documentação;
- `springdoc-openapi` compatível com a versão utilizada do Spring Boot 3.x.

As regras de negócio, validações críticas e verificações de autorização permanecerão no backend.

## Justificativa

Thymeleaf possui integração direta com Spring Boot e evita a necessidade de uma aplicação frontend separada. Bootstrap facilita o atendimento ao requisito de responsividade. OpenAPI documenta contratos, entradas, respostas, autenticação e códigos HTTP.

## Consequências

### Positivas

- Menor complexidade de desenvolvimento e implantação;
- Interface responsiva e padronizada;
- Documentação navegável dos endpoints;
- Regras de negócio centralizadas no backend.

### Negativas

- A interface fica vinculada ao Spring MVC;
- A documentação OpenAPI deverá acompanhar toda alteração nos endpoints.

## Alternativas consideradas

- React ou Vue: não escolhidos por aumentarem a complexidade inicial;
- CSS sem framework: não escolhido devido ao esforço adicional para garantir responsividade;
- Documentação somente manual: descartada pelo risco de desatualização.

## Segurança

A documentação não poderá expor senhas, tokens, segredos, dados pessoais ou detalhes internos de erros. Endpoints protegidos deverão informar os requisitos de autenticação sem fornecer credenciais reais.

## Verificação futura

A decisão será comprovada pelas dependências do `pom.xml`, páginas responsivas, testes dos fluxos web e especificação OpenAPI.
