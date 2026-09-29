# ADR-004: Arquitetura em camadas

- **Status:** Aceita
- **Data:** 26/08/2026
- **Issue relacionada:** #1

## Contexto

O sistema possui regras críticas de autorização, reservas, concorrência, manutenção, auditoria e movimentação de materiais. Essas regras precisam permanecer separadas da interface, do banco de dados e das integrações externas para facilitar testes e manutenção.

## Decisão

A aplicação utilizará arquitetura em camadas, organizada nas seguintes responsabilidades:

- apresentação: controllers web e REST, validação inicial das entradas e respostas;
- aplicação: serviços e coordenação dos casos de uso;
- domínio: entidades, estados e regras de negócio;
- persistência: repositórios e mapeamento com o PostgreSQL;
- infraestrutura: segurança, configurações, notificações e integrações externas.

O fluxo principal de dependência será da apresentação para a aplicação e da aplicação para o domínio e persistência.

## Regras arquiteturais

- Controllers não implementarão regras de negócio;
- Repositórios não decidirão regras de autorização;
- Serviços controlarão os casos de uso e as transações;
- Regras críticas deverão ser testáveis sem depender da interface;
- Integrações externas serão acessadas por interfaces próprias;
- Erros serão tratados de forma centralizada;
- Auditoria será gerada pelos casos de uso que alteram estado.

## Justificativa

A arquitetura em camadas é compatível com Spring Boot, possui menor complexidade inicial que uma arquitetura hexagonal completa e permite separar responsabilidades de forma suficiente para o escopo acadêmico.

## Consequências

### Positivas

- Separação clara de responsabilidades;
- Facilidade para testes unitários e de integração;
- Menor duplicação de regras;
- Manutenção e revisão de código mais simples;
- Organização compreensível para os quatro integrantes.

### Negativas

- Serviços podem crescer excessivamente se não forem divididos por caso de uso;
- Dependências entre camadas precisarão ser revisadas;
- A disciplina arquitetural deverá ser mantida durante todo o desenvolvimento.

## Alternativas consideradas

- Arquitetura hexagonal: não escolhida inicialmente por aumentar a quantidade de abstrações e arquivos;
- Código organizado apenas por controllers e repositories: descartado por favorecer regras de negócio espalhadas.

## Verificação futura

A decisão será verificada por revisão de código, testes automatizados e análise da organização dos pacotes. Violações arquiteturais identificadas deverão ser registradas e corrigidas.
