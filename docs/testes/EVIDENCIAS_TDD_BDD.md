# Evidências de TDD/BDD

Status: BDD executável evidenciado; TDD histórico permanece sem comprovação e não é alegado.

## Execução de referência

| Campo | Valor |
|---|---|
| SHA completo e estado do checkout | `b17cd259c8753e6bf59ad561b620009a0fae63d5` — branch `chore/sonarcloud-quality-gate` |
| Data/hora | `2026-11-13 08:15:05 -03:00` |
| Ambiente | Windows; Java 21.0.12.1; PostgreSQL via Testcontainers/Docker |
| Comando e resultado | `verify`; 898 testes executados, 0 falhas, 0 erros, 1 ignorado; `BUILD SUCCESS`; gate JaCoCo aprovado |

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

## Evidência executável BDD

A especificação BDD está automatizada em:

`src/test/java/br/com/senac/gerenciadordesalas/cadastros/ReservationCriacaoIntegrationTest.java`

Cenário automatizado:

- Dado um solicitante autenticado, sala disponível e materiais suficientes;
- Quando solicita uma reserva válida pela API;
- Então recebe HTTP 201, cabeçalho `Location`, identificador e confirmação de persistência no PostgreSQL.

Comando executado:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress "-Dtest=ReservationCriacaoIntegrationTest" test
```

Resultado:

- Data/hora: `2026-11-13 08:36:40 -03:00`.
- SHA: `afbc7e070ce400deef00faaead1e16252e33e8cf`.
- Ambiente: Windows; Java 21.0.12.1; Docker/Testcontainers; PostgreSQL 16.16.
- Total de testes: 9; falhas: 0; erros: 0; ignorados: 0.
- Conclusão: `BUILD SUCCESS`.

O cenário executável usa `@DisplayName` em Given–When–Then; o `@BeforeEach` prepara solicitante, sala e material, e o método confirma tanto a resposta HTTP quanto a persistência no banco real do Testcontainers.

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
