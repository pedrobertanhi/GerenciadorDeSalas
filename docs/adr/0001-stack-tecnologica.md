# ADR-001: Stack tecnológica base

- **Status:** Aceita
- **Data:** 26/08/2026
- **Issue relacionada:** #1

## Contexto

O projeto deve ser desenvolvido em Java 21 com Spring Boot 3.x. Também é necessário definir previamente a ferramenta de construção e o banco de dados para manter o ambiente padronizado entre os quatro integrantes, o pipeline de integração contínua e os testes automatizados.

## Decisão

A aplicação utilizará:

- Java 21 como versão da linguagem e da JVM;
- Spring Boot 3.x como framework principal;
- Maven como ferramenta de construção e gerenciamento de dependências;
- Maven Wrapper para garantir a mesma execução nos ambientes locais e no CI;
- PostgreSQL como banco de dados relacional.

A versão específica do Spring Boot 3.x será registrada no `pom.xml` durante a inicialização da aplicação e deverá ser compatível com Java 21.

## Justificativa

A stack atende à especificação do projeto, possui integração direta com JUnit 5, JaCoCo, Spring Security, Testcontainers e ferramentas de integração contínua. O PostgreSQL oferece suporte adequado a transações e restrições necessárias para as regras de concorrência das reservas.

## Consequências

### Positivas

- Ambiente padronizado entre desenvolvimento, testes e CI;
- Compatibilidade com as ferramentas obrigatórias do projeto;
- Suporte a transações e persistência relacional;
- Execução reproduzível por meio do Maven Wrapper.

### Negativas

- Todos os integrantes precisarão utilizar Java 21;
- O PostgreSQL deverá estar disponível no ambiente de desenvolvimento;
- Atualizações de dependências precisarão preservar a compatibilidade com Spring Boot 3.x.

## Alternativas consideradas

- Gradle: não escolhido porque a equipe padronizou o projeto com Maven;
- MySQL: não escolhido porque o PostgreSQL foi definido como banco relacional do projeto;
- Versões anteriores do Java: descartadas por não atenderem à especificação.

## Verificação futura

A aplicação deverá comprovar esta decisão por meio do `pom.xml`, Maven Wrapper, configuração do PostgreSQL, execução automatizada dos testes e pipeline do GitHub Actions.
