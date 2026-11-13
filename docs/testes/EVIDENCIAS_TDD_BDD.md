# Evidências de TDD/BDD

Status: pendente de comprovação histórica. Este documento registra o roteiro de coleta; não atesta execução nem adoção anterior de TDD.

## Execução de referência

| Campo | Valor |
|---|---|
| SHA completo e estado do checkout | `b17cd259c8753e6bf59ad561b620009a0fae63d5` — branch `chore/sonarcloud-quality-gate` |
| Data/hora | `2026-11-13 08:15:05 -03:00` |
| Ambiente | Windows; Java 21.0.12.1; PostgreSQL via Testcontainers/Docker |
| Comando e resultado | `verify`; 898 testes, 0 falhas, 0 erros, 0 ignorados; `BUILD SUCCESS`; gate JaCoCo aprovado |

Esta execução comprova o estado atual da suíte no SHA registrado. Ela não fornece as etapas Red, Green e Refactor de uma funcionalidade e, portanto, não encerra o risco residual de TDD/BDD.

## Cenário BDD proposto: criação de reserva

Origem: plano de testes (#51, #105) e criação de reserva (#24).

```gherkin
# language: pt
Funcionalidade: Solicitação de reserva
  Cenário: Solicitante cria uma reserva válida
    Dado um solicitante ativo e autenticado
    E uma sala disponível e materiais com quantidade suficiente
    E um intervalo futuro válido segundo um relógio controlado
    Quando solicitar a reserva pela API
    Então receberá HTTP 201 com Location e identificador
    E a reserva será persistida com os materiais solicitados

  Cenário: Requisição sem autenticação
    Dado uma requisição de reserva válida sem usuário autenticado
    Quando enviar a requisição com CSRF válido
    Então receberá HTTP 401
    E nenhuma reserva será persistida
```

Referência de implementação existente: [ReservationCriacaoIntegrationTest.java](../../src/test/java/br/com/senac/gerenciadordesalas/cadastros/ReservationCriacaoIntegrationTest.java). A descrição acima é uma especificação documental; não existe execução de Gherkin comprovada por este arquivo.

## Registro de um ciclo verificável

Preencher um registro por funcionalidade a partir de commits e execuções reais:

| Etapa | Evidência exigida | SHA completo | Execução/log | Resultado observado |
|---|---|---|---|---|
| Red | Teste novo falha pela regra ainda ausente, sem erro de infraestrutura | Pendente | Pendente | Não verificado |
| Green | Implementação satisfaz o mesmo teste | Pendente | Pendente | Não verificado |
| Refactor | Refatoração preserva o comportamento e a suíte passa | Pendente | Pendente | Não verificado |

Registrar também: requisito/issue, PR, nome do teste, comando exato, ambiente, data com fuso e revisor. Conferir a ordem dos commits e o conteúdo das alterações; a presença atual do teste não demonstra que ele antecedeu a implementação. Se não houve refatoração, registrar explicitamente e justificar, sem inventar um commit.

A [prova de CI vermelho → verde](../qualidade/EVIDENCIA_CI_RED_GREEN_ENT04.md) altera limites do JaCoCo. Ela comprova o gate de cobertura, mas não substitui o registro de TDD de uma funcionalidade.

## Critério de encerramento

Manter como risco residual até que um revisor consiga consultar os commits e os relatórios correspondentes e reproduzir o cenário. Uma nova execução demonstra o estado atual; não recupera, por si só, evidência histórica ausente.
