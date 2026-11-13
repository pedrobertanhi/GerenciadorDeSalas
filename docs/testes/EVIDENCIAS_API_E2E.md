# Evidências de API e E2E

Status: execução completa da suíte registrada; a rastreabilidade histórica por cenário e a comprovação individual de jornadas E2E permanecem pendentes.

## Escopo identificado

| Nível | Referência | O que deve ser verificado |
|---|---|---|
| API com MockMvc e PostgreSQL | [ReservationCriacaoIntegrationTest.java](../../src/test/java/br/com/senac/gerenciadordesalas/cadastros/ReservationCriacaoIntegrationTest.java) | Criação 201, Location, resposta e persistência; 401/403 sem reserva persistida |
| Navegador | [ReservasNavegadorTest.java](../../src/test/java/br/com/senac/gerenciadordesalas/cadastros/ReservasNavegadorTest.java) | Inspecionar cenários e confrontar o relatório com as jornadas de reserva |
| Infraestrutura E2E | [NavegadorBase.java](../../src/test/java/br/com/senac/gerenciadordesalas/cadastros/NavegadorBase.java) e [NavegadorE2E.java](../../src/test/java/br/com/senac/gerenciadordesalas/cadastros/NavegadorE2E.java) | Configuração de navegador, aplicação e dependências |

MockMvc não comprova contrato por conexão HTTP externa. Registrar separadamente essa lacuna caso o aceite exija API caixa-preta por rede. Uma jornada isolada de reserva também não comprova todas as jornadas das três personas.

## Reprodução

Pré-requisitos: JDK 21, Docker disponível para Testcontainers e Chrome compatível com a configuração dos testes de navegador. Consultar as classes de infraestrutura antes de executar para identificar requisitos adicionais.

Na raiz, em PowerShell:

```powershell
git rev-parse HEAD
java -version
docker version
.\mvnw.cmd --batch-mode --no-transfer-progress '-Dtest=ReservationCriacaoIntegrationTest,ReservasNavegadorTest' test
```

Esse comando seleciona parte da suíte e não substitui o gate completo:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

## Registro da execução

| Campo | Valor |
|---|---|
| SHA completo e estado do checkout | `b17cd259c8753e6bf59ad561b620009a0fae63d5` — branch `chore/sonarcloud-quality-gate` |
| Requisito, cenário e persona | Pendente |
| Data/hora com fuso, SO, JDK, Docker e Chrome | `2026-11-13 08:15:05 -03:00`; Windows; Java 21.0.12.1; PostgreSQL via Testcontainers/Docker; versão do Chrome não registrada |
| Comando e código de saída | `verify`; código de saída 0; `BUILD SUCCESS` |
| Total, sucessos, falhas, erros e ignorados | 898 testes executados; 0 falhas; 0 erros; 1 ignorado |
| URL da execução CI e tentativa | Pendente |
| Relatórios Surefire e capturas correspondentes | JaCoCo gerado e gate aprovado; localização/identificadores dos relatórios e das capturas não registrados |
| Revisor e conclusão por cenário | Pendente |

A [CI](../../.github/workflows/ci.yml) configura artifacts de relatórios Maven e capturas em `target/capturas/`. A configuração de upload não demonstra que arquivos foram produzidos: conferir conteúdo, SHA e testes ignorados. Preservar evidências antes do prazo de retenção dos artifacts.

## Risco residual e encerramento

Esta execução não afirma individualmente que `ReservasNavegadorTest` passou, pois o resultado consolidado não traz sua linha específica. Permanecem pendentes a associação requisito → cenário → execução da release e a revisão das jornadas por perfil. Só encerrar após conferir relatórios reais, efeitos no banco e resultados negativos, registrando separadamente cenários ausentes, falhos ou ignorados.
