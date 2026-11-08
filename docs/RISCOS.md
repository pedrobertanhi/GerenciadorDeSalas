# Registro de Riscos

> Status: registro inicial. Revisar em cada marco do projeto.

| ID | Risco | Probabilidade | Impacto | Prioridade | Tratamento planejado | Issues | Responsável | Status |
|---|---|---|---|---|---|---|---|---|
| R01 | Dupla reserva em concorrência | Alta | Crítico | Crítica | Trava `SELECT ... FOR UPDATE` de sala, professor e materiais antes de carregar e validar, com teste de concorrência determinístico em PostgreSQL real (`ReservationConcorrenciaIntegrationTest`, PR #174); teste de carga com JMeter segue na #54 | #27, #54 | Matheus | Mitigado |
| R02 | Falha de autorização por perfil | Média | Crítico | Crítica | Matriz de permissões e testes positivos/negativos | #2, #13, #14 | Não atribuído | Aberto |
| R03 | Aplicação não executar na entrega | Média | Crítico | Crítica | Build reproduzível e instalação em ambiente limpo | #5, #52, #80 | Não atribuído | Aberto |
| R04 | Pipeline ausente ou instável | Média | Crítico | Crítica | GitHub Actions obrigatório em PR e proteção da main | #4, #52 | Não atribuído | Aberto |
| R05 | Cobertura abaixo das metas | Média | Alto | Alta | JaCoCo com 80% linhas e 70% branches | #31, #53 | Não atribuído | Aberto |
| R06 | Vulnerabilidade crítica | Média | Crítico | Crítica | SonarCloud, revisão de hotspots e testes de segurança | #14, #53 | Não atribuído | Aberto |
| R07 | Recurso em manutenção ser reservado | Média | Crítico | Crítica | Validação central na criação e alteração de reserva, com teste de regressão | #21, #22, #25 | Pedro | Em tratamento |
| R08 | Falha externa afetar reserva | Média | Alto | Alta | Gateway isolado, timeout seguro e WireMock | #41, #42 | Não atribuído | Aberto |
| R09 | Desempenho insuficiente sob carga | Média | Alto | Alta | Baseline e cenários JMeter | #54 | Não atribuído | Aberto || R10 | Falta de autoria de integrante | Média | Alto | Alta | Distribuição de código/teste, PRs próprios e revisão cruzada | #60 | Não atribuído | Aberto |
| R11 | Evidência incompleta ou sem versão | Média | Alto | Alta | Índice versionado e auditoria dos links | #55, #78, #81 | Não atribuído | Aberto |
| R12 | Crescimento indevido do escopo | Média | Médio | Média | Validar mudanças contra RF01–RF14 | #1 | Não atribuído | Aberto |
| R13 | Cobertura alta com testes triviais | Média | Alto | Alta | Revisar assertions, cenários de limite e sensibilidade dos testes | #53, #89 | Não atribuído | Aberto |
| R14 | Segredo exposto no Git ou CI | Média | Crítico | Crítica | Secret scanning, push protection e rotação | #95 | Não atribuído | Aberto |
| R15 | Dependência ou imagem com vulnerabilidade crítica | Média | Crítico | Crítica | SCA, dependency review e Trivy | #93, #96 | Não atribuído | Aberto |
| R16 | Vulnerabilidade detectável somente em execução | Média | Crítico | Crítica | DAST com OWASP ZAP | #97 | Não atribuído | Aberto |
| R17 | Release aprovada sem todas as verificações | Média | Crítico | Crítica | Gate e parecer final de segurança | #98 | Não atribuído | Aberto |
| R18 | Capacidade planejada acima da velocidade real | Alta | Alto | Alta | Medir velocidade, limitar WIP e usar buffer da semana 46 | #58 | Não atribuído | Aberto |
| R19 | Regressão visual ou de interação não detectada nas telas | Baixa | Médio | Baixa | Testes E2E em navegador (Selenium e Chrome) no pipeline, mais o roteiro manual versionado nas evidências | #15 | Não atribuído | Mitigado |
| R20 | Telas sem layout compartilhado e sem navegação por perfil (UI-01) gerarem fluxo inconsistente ou inacessível | Média | Médio | Média | Entregar o layout compartilhado e revisar os links entre as telas | #15 | Não atribuído | Aberto |

## Escalas

- **Probabilidade:** baixa, média ou alta.
- **Impacto:** baixo, médio, alto ou crítico.
- **Status:** aberto, em tratamento, aceito, mitigado ou ocorrido.

## Atualização obrigatória

Uma mudança de probabilidade, impacto, tratamento ou status deve gerar commit próprio e referenciar a Issue relacionada.
