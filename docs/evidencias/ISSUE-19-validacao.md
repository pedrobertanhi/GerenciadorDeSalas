# Evidência de validação: Issue #19 (RES-05)

## Escopo entregue

- pesquisa de salas combinável por capacidade mínima e localização em `GET /api/salas`, com paginação e ordenação (`page`, `size`, `sort`) e ordenação padrão por nome;
- somente salas ativas aparecem na pesquisa;
- pesquisa e consulta de salas liberadas aos três perfis, conforme a linha "Pesquisar salas e materiais" de `docs/seguranca/MATRIZ_DE_PERMISSOES.md`; cadastro, alteração e inativação continuam exclusivos do Administrador;
- filtro vazio equivale a ausência de filtro; capacidade mínima menor que 1 retorna 400 (`VALIDACAO_INVALIDA`); ordenação por campo inexistente retorna 400 (`REQUISICAO_INVALIDA`) em vez de 500;
- o filtro de localização trata `%`, `_` e `\` como texto literal: os curingas são escapados e o caractere de escape é informado ao `LIKE`, de modo que `Sala_1` não encontra `SalaA1`;
- tela `/recursos/pesquisa` (Thymeleaf) para qualquer usuário autenticado: salas por capacidade mínima e localização, materiais por tipo e localização, professores por competência, com 10 itens por página;
- na tela, filtro inválido não executa a pesquisa e retorna 400 com a mensagem exibida na própria página; resultado vazio retorna 200 com a mensagem "Nenhum recurso encontrado.".

## Massa controlada

`SalaPesquisaIntegracaoTest` cria, antes de cada teste, cinco salas conhecidas:

| Sala          | Capacidade | Localização        | Situação |
| ------------- | ---------- | ------------------ | -------- |
| Auditorio     | 120        | Bloco A - Terreo   | ativa    |
| Laboratorio 1 | 30         | Bloco B - 2o andar | ativa    |
| Sala 101      | 40         | Bloco A - 1o andar | ativa    |
| Sala 102      | 25         | Bloco A - 1o andar | ativa    |
| Sala Antiga   | 60         | Bloco A - Terreo   | inativa  |

Os três casos de busca literal criam, além dessa massa, pares de salas próprios (sala alvo e sala-isca) para provar que o curinga não vaza: `Bloco_1` contra `BlocoA1`, `Bloco 100% novo` contra `Bloco 100 novo` e `Bloco A\B` contra `Bloco AB`.

Cada teste confere a lista exata de nomes devolvida, na ordem esperada.

## Testes localizáveis

- `SalaPesquisaIntegracaoTest`: 15 casos com PostgreSQL real via Testcontainers (sem filtros, filtro vazio, capacidade mínima no limite, localização parcial sem diferenciar maiúsculas, filtros combinados, sala inativa excluída, resultado vazio, paginação, ordenação, ordenação inválida, capacidade inválida e busca literal de `%`, `_` e `\` na localização);
- `PesquisaRecursosWebControllerTest`: 15 casos da tela (acesso sem autenticação, acesso pelos três perfis, filtros de salas combinados e normalizados, filtros vazios, materiais por tipo e localização, professores por competência, resultado vazio, paginação e cinco filtros inválidos);
- `SalaControllerTest` e `SalaIntegracaoTest`: ajustados para a pesquisa liberada aos perfis, mantendo a escrita bloqueada.

## Como validar

Comando: `./mvnw --batch-mode --no-transfer-progress verify`.

`SalaPesquisaIntegracaoTest` exige Docker; sem ele, aparece como ignorado. `PesquisaRecursosWebControllerTest` não usa banco e roda em qualquer máquina com Java 21:

```bash
./mvnw test -Dtest=PesquisaRecursosWebControllerTest
```

## Resultado

- **Maven Verify** no PR #147, commit `d8c5d19`: `SalaPesquisaIntegracaoTest` com 12 testes executados, 0 falhas, 0 erros e 0 ignorados;
- após a correção do escape de curingas apontada na revisão: `SalaPesquisaIntegracaoTest` com 15 testes executados, 0 falhas, 0 erros e 0 ignorados (registrar aqui o hash do commit e a execução do Maven Verify correspondente);
- execução local de `PesquisaRecursosWebControllerTest`, commit `07ab4ec`: 15 testes executados, 0 falhas, 0 erros e 0 ignorados;
- execução final do **Maven Verify** do commit mesclado: a registrar após o merge. Até lá, a coluna de execução do RF-03 na RTM permanece PENDENTE.
