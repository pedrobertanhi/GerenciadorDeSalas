# ADR-005: Autenticação e autorização

- **Status:** Aceita
- **Data:** 26/08/2026
- **Issue relacionada:** #1

## Contexto

O sistema possui três perfis com permissões diferentes: Solicitante, Responsável e Administrador. A falha de autorização é um limitador de nota e poderá expor reservas, usuários e recursos indevidamente.

## Decisão

A segurança será implementada com Spring Security, autenticação por formulário e sessão mantida no servidor.

As senhas serão armazenadas exclusivamente como hash utilizando `PasswordEncoder` com BCrypt. Senhas, tokens e segredos não serão incluídos no código-fonte ou nos logs.

Os perfis serão representados pelas autoridades:

- `ROLE_SOLICITANTE`;
- `ROLE_RESPONSAVEL`;
- `ROLE_ADMINISTRADOR`.

## Política de autorização

O sistema adotará negação por padrão. Uma operação somente será permitida quando houver autorização explícita.

As verificações ocorrerão nos serviços, não apenas na interface ou nos controllers.

- Solicitante consulta disponibilidade e gerencia somente as próprias reservas;
- Responsável aprova recursos restritos, valida docentes e acompanha materiais;
- Administrador gerencia recursos, usuários, bloqueios e manutenções;
- Somente Responsável poderá aprovar uma solicitação de recurso restrito;
- Reservas iniciadas não poderão ser apagadas por nenhum perfil.

## Respostas de acesso

- Requisições sem autenticação receberão `401 Unauthorized` nos fluxos de API;
- Operações sem permissão de perfil receberão `403 Forbidden`;
- Consultas por identificador que possam revelar um recurso restrito inacessível responderão `404 Not Found`.

Essa política deverá ser aplicada de forma consistente e testada para impedir enumeração de recursos.

## Proteções de sessão

- Proteção CSRF permanecerá habilitada nos formulários;
- Cookies de sessão utilizarão `HttpOnly`;
- Cookies utilizarão `Secure` no ambiente de produção;
- A sessão será invalidada durante o logout;
- Identificadores de sessão não serão registrados em logs;
- Mensagens de erro não revelarão credenciais ou detalhes internos.

## Consequências

### Positivas

- Integração direta com Spring Boot e Thymeleaf;
- Controle centralizado por perfil e propriedade;
- Proteção contra acesso direto a endpoints;
- Menor complexidade que autenticação por tokens para a interface escolhida.

### Negativas

- A aplicação precisará controlar sessões;
- Testes deverão cobrir cada perfil e propriedade;
- Integrações futuras sem navegador poderão exigir outra estratégia.

## Alternativas consideradas

- JWT: não escolhido inicialmente porque aumentaria a complexidade sem necessidade para a interface web renderizada no servidor;
- Autorização somente nos controllers: descartada por permitir contorno das regras em outros pontos da aplicação.

## Verificação futura

Serão criados testes automatizados para autenticação, logout, CSRF, acesso por perfil, propriedade da reserva, aprovação de recurso restrito e tentativa de acesso direto a recursos protegidos.
