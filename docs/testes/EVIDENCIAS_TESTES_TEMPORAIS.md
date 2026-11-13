# Evidências de testes temporais

Status: execução completa registrada para o teste de política temporal; evidência histórica e fronteiras adicionais ainda pendentes.

## Cobertura observada no código

Referência: [ReservationTimePolicyTest.java](../../src/test/java/br/com/senac/gerenciadordesalas/reservas/ReservationTimePolicyTest.java), com `Clock.fixed` em `2026-11-10T09:00:00Z`.

| Cenário | Situação no código inspecionado |
|---|---|
| Início ou término nulo | Caso parametrizado existente |
| Término igual ou anterior ao início | Caso parametrizado existente |
| Início um segundo antes de agora | Teste existente de rejeição |
| Intervalo futuro em UTC | Teste existente de aceitação |
| Intervalos adjacentes e sobrepostos | Teste existente; término exclusivo |
| Início exatamente em agora e imediatamente depois | Evidência específica a consolidar |
| Mesmo instante representado por offsets diferentes | Evidência específica a consolidar |
| Conversão de horário local, virada de dia e transições de fuso | Avaliar requisito e consolidar casos aplicáveis |

O nome `aceitaHorarioFuturoIndependenteDoFusoDeOrigem` não basta para comprovar conversão entre fusos: o corpo usa apenas instantes com `Z`. Não registrar equivalência entre offsets como comprovada por esse caso.

A [RelogioFixoTestConfig.java](../../src/test/java/br/com/senac/gerenciadordesalas/cadastros/RelogioFixoTestConfig.java) fornece outro instante fixo para integração: `2026-11-01T00:00:00Z`. Conferir sua importação em cada classe relevante; sua existência não garante uso por toda a suíte.

## Reprodução

Na raiz, com JDK 21, em PowerShell:

```powershell
git rev-parse HEAD
java -version
.\mvnw.cmd --batch-mode --no-transfer-progress '-Dtest=ReservationTimePolicyTest' test
```

Para verificar a aplicação do relógio em integração, com Docker disponível:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress '-Dtest=ReservationCriacaoIntegrationTest' test
```

## Registro e critério de encerramento

| Evidência | Valor |
|---|---|
| SHA completo e estado do checkout | `b17cd259c8753e6bf59ad561b620009a0fae63d5` — branch `chore/sonarcloud-quality-gate` |
| Execução/log, data com fuso e ambiente | `2026-11-13 08:15:05 -03:00`; Windows; Java 21.0.12.1; PostgreSQL via Testcontainers/Docker |
| Comando, código de saída e XML Surefire | `verify`; código de saída 0; `BUILD SUCCESS`; localização do XML Surefire não registrada |
| Casos executados, falhos e ignorados | `ReservationTimePolicyTest`: 7 testes, 0 falhas; a execução completa teve 898 testes, 0 falhas, 0 erros e 0 ignorados |
| Cobertura | JaCoCo gerado; todos os checks de cobertura aprovados |
| Regra de negócio para início igual a agora | Pendente |
| Requisitos de zona/offset e justificativa de casos não aplicáveis | Pendente |
| Revisor e conclusão | Pendente |

Manter o risco residual até verificar as fronteiras exigidas pelo requisito, o uso do relógio controlado e os relatórios vinculados ao SHA da release. Não depender do relógio real nem de espera com `Thread.sleep()` para simular passagem do tempo.
