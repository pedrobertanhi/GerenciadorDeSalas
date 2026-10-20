# Backlog completo

> Catálogo versionado das Issues planejadas após auditoria da especificação e dos materiais das aulas. A fonte operacional continua sendo a aba Issues/GitHub Project. Os itens marcados abaixo refletem somente entregas já integradas à `main`.

## Como usar

1. Não atribuir até os quatro integrantes entrarem.
2. Mover para Ready somente com dependências, aceite, testes, risco e evidência definidos.
3. Criar branch por Issue.
4. Fazer commit por alteração concluída de arquivo; separar alterações independentes.
5. Manter código, teste, configuração, documento e evidência em commits diferentes.
6. Abrir PR sem squash e obter revisão de outro integrante.
7. Atualizar RTM/evidências com o commit realmente executado.
8. Só então mover para Done.

Planejamento total: **584 pontos**, sendo **552 até a semana 46** e **32 após a entrega**. Consulte [PLANEJAMENTO.md](PLANEJAMENTO.md).

## Governança (10)

- [x] [#1 — Validar escopo, requisitos e decisões técnicas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/1) — semana 34, 3 pts
- [x] [#2 — Definir modelo de domínio e matriz de permissões](https://github.com/pedrobertanhi/gerenciadordesalas/issues/2) — semana 34, 5 pts
- [x] [#3 — Criar diagramas de contexto, componentes e dados](https://github.com/pedrobertanhi/gerenciadordesalas/issues/3) — semana 34, 5 pts
- [x] [#4 — Configurar fluxo de trabalho no GitHub](https://github.com/pedrobertanhi/gerenciadordesalas/issues/4) — semana 34, 3 pts
- [ ] [#57 — Criar diagramas de sequências críticas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/57) — semana 45, 8 pts
- [ ] [#58 — Configurar quadro, campos e marcos do projeto](https://github.com/pedrobertanhi/gerenciadordesalas/issues/58) — semana 34, 5 pts
- [ ] [#60 — Comprovar autoria dos quatro integrantes](https://github.com/pedrobertanhi/gerenciadordesalas/issues/60) — semana 34, 5 pts
- [ ] [#90 — Realizar diagnóstico inicial do projeto](https://github.com/pedrobertanhi/gerenciadordesalas/issues/90) — semana 34, 3 pts
- [ ] [#99 — Elaborar PRD com personas, escopo e critérios mensuráveis](https://github.com/pedrobertanhi/gerenciadordesalas/issues/99) — semana 35, 3 pts
- [ ] [#109 — Executar revisão independente de SQA e gates de prontidão](https://github.com/pedrobertanhi/gerenciadordesalas/issues/109) — semana 36, 3 pts

## Arquitetura e ATAM (1)

- [ ] [#100 — Executar ATAM enxuto e criar utility tree](https://github.com/pedrobertanhi/gerenciadordesalas/issues/100) — semana 35, 5 pts

## Fundação Java/Spring (6)

- [x] [#5 — Criar estrutura inicial do Spring Boot](https://github.com/pedrobertanhi/gerenciadordesalas/issues/5) — semana 34, 3 pts
- [x] [#6 — Configurar dependências e perfis de ambiente](https://github.com/pedrobertanhi/gerenciadordesalas/issues/6) — semana 34, 3 pts
- [x] [#7 — Configurar PostgreSQL com Docker Compose](https://github.com/pedrobertanhi/gerenciadordesalas/issues/7) — semana 34, 3 pts
- [x] [#8 — Configurar Flyway e esquema inicial](https://github.com/pedrobertanhi/gerenciadordesalas/issues/8) — semana 34, 5 pts
- [x] [#9 — Padronizar validações e tratamento seguro de erros](https://github.com/pedrobertanhi/gerenciadordesalas/issues/9) — semana 34, 5 pts
- [ ] [#10 — Configurar documentação OpenAPI](https://github.com/pedrobertanhi/gerenciadordesalas/issues/10) — semana 34, 3 pts

## Segurança (14)

- [x] [#11 — Implementar usuários, perfis e persistência](https://github.com/pedrobertanhi/gerenciadordesalas/issues/11) — semana 36, 5 pts
- [x] [#12 — Implementar autenticação e armazenamento seguro de senha](https://github.com/pedrobertanhi/gerenciadordesalas/issues/12) — semana 36, 5 pts
- [x] [#13 — Aplicar autorização por perfil](https://github.com/pedrobertanhi/gerenciadordesalas/issues/13) — semana 37, 8 pts
- [ ] [#14 — Criar testes automatizados de segurança](https://github.com/pedrobertanhi/gerenciadordesalas/issues/14) — semana 37, 5 pts
- [ ] [#64 — Modelar solicitante professor ou coordenador e propriedade da reserva](https://github.com/pedrobertanhi/gerenciadordesalas/issues/64) — semana 36, 5 pts
- [ ] [#85 — Implementar gerenciamento administrativo de usuários](https://github.com/pedrobertanhi/gerenciadordesalas/issues/85) — semana 44, 8 pts
- [ ] [#92 — Criar modelo de ameaças e checklist OWASP](https://github.com/pedrobertanhi/gerenciadordesalas/issues/92) — semana 34, 3 pts
- [ ] [#93 — Verificar vulnerabilidades das dependências](https://github.com/pedrobertanhi/gerenciadordesalas/issues/93) — semana 36, 5 pts
- [ ] [#94 — Configurar análise estática de segurança com CodeQL](https://github.com/pedrobertanhi/gerenciadordesalas/issues/94) — semana 37, 5 pts
- [ ] [#95 — Habilitar detecção e proteção contra segredos](https://github.com/pedrobertanhi/gerenciadordesalas/issues/95) — semana 35, 3 pts
- [ ] [#96 — Escanear repositório e imagem com Trivy](https://github.com/pedrobertanhi/gerenciadordesalas/issues/96) — semana 40, 5 pts
- [ ] [#97 — Executar teste dinâmico de segurança com OWASP ZAP](https://github.com/pedrobertanhi/gerenciadordesalas/issues/97) — semana 44, 5 pts
- [ ] [#98 — Aplicar gate final de segurança da release](https://github.com/pedrobertanhi/gerenciadordesalas/issues/98) — semana 45, 5 pts
- [ ] [#110 — Provar ausência de vazamento em respostas, logs e cache](https://github.com/pedrobertanhi/gerenciadordesalas/issues/110) — semana 40, 3 pts

## Recursos (6)

- [ ] [#15 — Implementar cadastro de salas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/15) — semana 35, 5 pts
- [ ] [#16 — Implementar cadastro de professores e competências](https://github.com/pedrobertanhi/gerenciadordesalas/issues/16) — semana 35, 5 pts
- [ ] [#17 — Implementar cadastro de materiais e quantidades](https://github.com/pedrobertanhi/gerenciadordesalas/issues/17) — semana 35, 5 pts
- [ ] [#18 — Criar testes de integração dos cadastros](https://github.com/pedrobertanhi/gerenciadordesalas/issues/18) — semana 35, 5 pts
- [ ] [#19 — Implementar pesquisa combinada de recursos](https://github.com/pedrobertanhi/gerenciadordesalas/issues/19) — semana 36, 5 pts
- [ ] [#20 — Implementar consulta de disponibilidade](https://github.com/pedrobertanhi/gerenciadordesalas/issues/20) — semana 42, 8 pts

## Manutenção (2)

- [ ] [#21 — Implementar cadastro de bloqueios de manutenção](https://github.com/pedrobertanhi/gerenciadordesalas/issues/21) — semana 41, 5 pts
- [ ] [#22 — Impedir reserva de recurso em manutenção](https://github.com/pedrobertanhi/gerenciadordesalas/issues/22) — semana 41, 5 pts

## Reservas (13)

- [ ] [#23 — Criar modelo e persistência de reservas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/23) — semana 37, 8 pts
- [ ] [#24 — Implementar criação de reservas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/24) — semana 37, 8 pts
- [ ] [#25 — Implementar alteração e cancelamento de reservas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/25) — semana 40, 8 pts
- [ ] [#26 — Implementar máquina de estados da reserva](https://github.com/pedrobertanhi/gerenciadordesalas/issues/26) — semana 39, 5 pts
- [ ] [#27 — Validar início, término e regras temporais](https://github.com/pedrobertanhi/gerenciadordesalas/issues/27) — semana 39, 5 pts
- [ ] [#28 — Detectar conflito de sala](https://github.com/pedrobertanhi/gerenciadordesalas/issues/28) — semana 39, 5 pts
- [ ] [#29 — Detectar conflito na agenda do professor](https://github.com/pedrobertanhi/gerenciadordesalas/issues/29) — semana 39, 5 pts
- [ ] [#30 — Detectar conflito e quantidade de materiais](https://github.com/pedrobertanhi/gerenciadordesalas/issues/30) — semana 39, 8 pts
- [ ] [#31 — Criar suíte parametrizada de sobreposição](https://github.com/pedrobertanhi/gerenciadordesalas/issues/31) — semana 39, 5 pts
- [ ] [#32 — Implementar proteção transacional contra dupla reserva](https://github.com/pedrobertanhi/gerenciadordesalas/issues/32) — semana 42, 8 pts
- [ ] [#33 — Criar teste automatizado de reservas simultâneas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/33) — semana 42, 8 pts
- [ ] [#59 — Impedir exclusão de reservas iniciadas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/59) — semana 39, 5 pts
- [ ] [#87 — Implementar início, conclusão e não comparecimento da reserva](https://github.com/pedrobertanhi/gerenciadordesalas/issues/87) — semana 43, 8 pts

## Aprovações (3)

- [ ] [#34 — Implementar fila de recursos restritos](https://github.com/pedrobertanhi/gerenciadordesalas/issues/34) — semana 41, 5 pts
- [ ] [#35 — Implementar aprovação e rejeição de reservas restritas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/35) — semana 41, 8 pts
- [ ] [#61 — Validar alocação de docentes pelo Responsável](https://github.com/pedrobertanhi/gerenciadordesalas/issues/61) — semana 41, 8 pts

## Retirada e devolução (3)

- [ ] [#36 — Implementar registro de retirada de materiais](https://github.com/pedrobertanhi/gerenciadordesalas/issues/36) — semana 43, 5 pts
- [ ] [#37 — Implementar devolução parcial e total de materiais](https://github.com/pedrobertanhi/gerenciadordesalas/issues/37) — semana 43, 5 pts
- [ ] [#38 — Criar testes de retirada, devolução e não comparecimento](https://github.com/pedrobertanhi/gerenciadordesalas/issues/38) — semana 43, 5 pts

## Auditoria (3)

- [ ] [#39 — Implementar auditoria de mudanças de estado](https://github.com/pedrobertanhi/gerenciadordesalas/issues/39) — semana 43, 8 pts
- [ ] [#40 — Implementar consulta e testes do histórico auditável](https://github.com/pedrobertanhi/gerenciadordesalas/issues/40) — semana 43, 5 pts
- [ ] [#88 — Auditar mudanças cadastrais e operacionais](https://github.com/pedrobertanhi/gerenciadordesalas/issues/88) — semana 43, 8 pts

## Notificações (2)

- [ ] [#41 — Implementar gateway e cliente de notificações](https://github.com/pedrobertanhi/gerenciadordesalas/issues/41) — semana 40, 5 pts
- [ ] [#42 — Testar notificações com WireMock](https://github.com/pedrobertanhi/gerenciadordesalas/issues/42) — semana 40, 5 pts

## Relatórios (2)

- [ ] [#43 — Implementar relatórios de utilização e conflitos evitados](https://github.com/pedrobertanhi/gerenciadordesalas/issues/43) — semana 44, 8 pts
- [ ] [#44 — Criar testes automatizados dos relatórios](https://github.com/pedrobertanhi/gerenciadordesalas/issues/44) — semana 44, 5 pts

## Interface (15)

- [ ] [#45 — Criar layout responsivo e navegação por perfil](https://github.com/pedrobertanhi/gerenciadordesalas/issues/45) — semana 37, 5 pts
- [ ] [#46 — Criar telas de recursos, pesquisa e manutenção](https://github.com/pedrobertanhi/gerenciadordesalas/issues/46) — semana 45, 0 pts
- [ ] [#47 — Criar telas de reservas, aprovações, operações e relatórios](https://github.com/pedrobertanhi/gerenciadordesalas/issues/47) — semana 45, 0 pts
- [ ] [#62 — Criar telas de professores e competências](https://github.com/pedrobertanhi/gerenciadordesalas/issues/62) — semana 45, 5 pts
- [ ] [#63 — Criar telas de salas](https://github.com/pedrobertanhi/gerenciadordesalas/issues/63) — semana 41, 5 pts
- [ ] [#65 — Criar tela de pesquisa e disponibilidade](https://github.com/pedrobertanhi/gerenciadordesalas/issues/65) — semana 42, 8 pts
- [ ] [#66 — Criar telas de materiais e equipamentos](https://github.com/pedrobertanhi/gerenciadordesalas/issues/66) — semana 46, 5 pts
- [ ] [#67 — Criar tela de manutenção de recursos](https://github.com/pedrobertanhi/gerenciadordesalas/issues/67) — semana 41, 5 pts
- [ ] [#68 — Criar tela de criação e detalhe da reserva](https://github.com/pedrobertanhi/gerenciadordesalas/issues/68) — semana 40, 8 pts
- [ ] [#69 — Criar tela de aprovações e validação docente](https://github.com/pedrobertanhi/gerenciadordesalas/issues/69) — semana 42, 8 pts
- [ ] [#70 — Criar telas de alteração e cancelamento de reserva](https://github.com/pedrobertanhi/gerenciadordesalas/issues/70) — semana 40, 5 pts
- [ ] [#71 — Criar tela de retirada e devolução de materiais](https://github.com/pedrobertanhi/gerenciadordesalas/issues/71) — semana 44, 8 pts
- [ ] [#72 — Criar tela de histórico auditável](https://github.com/pedrobertanhi/gerenciadordesalas/issues/72) — semana 44, 5 pts
- [ ] [#75 — Criar telas de relatórios](https://github.com/pedrobertanhi/gerenciadordesalas/issues/75) — semana 46, 5 pts
- [ ] [#86 — Criar telas administrativas de usuários](https://github.com/pedrobertanhi/gerenciadordesalas/issues/86) — semana 36, 5 pts

## Qualidade e testes (13)

- [ ] [#48 — Criar estratégia, plano e convenções de testes](https://github.com/pedrobertanhi/gerenciadordesalas/issues/48) — semana 35, 5 pts
- [ ] [#49 — Configurar Testcontainers para PostgreSQL](https://github.com/pedrobertanhi/gerenciadordesalas/issues/49) — semana 40, 5 pts
- [ ] [#50 — Criar testes de API em caixa-preta e ponta a ponta](https://github.com/pedrobertanhi/gerenciadordesalas/issues/50) — semana 44, 8 pts
- [ ] [#51 — Desenvolver uma funcionalidade com TDD ou BDD](https://github.com/pedrobertanhi/gerenciadordesalas/issues/51) — semana 41, 5 pts
- [ ] [#52 — Configurar pipeline de integração contínua](https://github.com/pedrobertanhi/gerenciadordesalas/issues/52) — semana 37, 5 pts
- [ ] [#53 — Configurar JaCoCo e SonarCloud](https://github.com/pedrobertanhi/gerenciadordesalas/issues/53) — semana 39, 5 pts
- [ ] [#54 — Planejar e executar testes de carga com JMeter](https://github.com/pedrobertanhi/gerenciadordesalas/issues/54) — semana 42, 8 pts
- [ ] [#73 — Produzir relatório da suíte automatizada](https://github.com/pedrobertanhi/gerenciadordesalas/issues/73) — semana 45, 8 pts
- [ ] [#89 — Avaliar qualidade dos testes e impedir cobertura trivial](https://github.com/pedrobertanhi/gerenciadordesalas/issues/89) — semana 45, 5 pts
- [ ] [#91 — Executar triagem dos primeiros defeitos](https://github.com/pedrobertanhi/gerenciadordesalas/issues/91) — semana 36, 5 pts
- [ ] [#101 — Criar catálogo de casos de teste reproduzíveis](https://github.com/pedrobertanhi/gerenciadordesalas/issues/101) — semana 35, 5 pts
- [ ] [#102 — Aplicar técnicas de caixa-preta, branca e cinza](https://github.com/pedrobertanhi/gerenciadordesalas/issues/102) — semana 39, 3 pts
- [ ] [#105 — Preservar evidências do ciclo TDD ou BDD](https://github.com/pedrobertanhi/gerenciadordesalas/issues/105) — semana 41, 3 pts

## Integração contínua (1)

- [ ] [#103 — Comprovar pipeline vermelho, recuperação verde e bloqueio de merge](https://github.com/pedrobertanhi/gerenciadordesalas/issues/103) — semana 37, 3 pts

## Integrações seguras (1)

- [ ] [#104 — Testar integração externa segura e degradação controlada](https://github.com/pedrobertanhi/gerenciadordesalas/issues/104) — semana 40, 5 pts

## Desempenho (1)

- [ ] [#106 — Definir RNFs mensuráveis e modelo de carga](https://github.com/pedrobertanhi/gerenciadordesalas/issues/106) — semana 42, 3 pts

## Rastreabilidade (1)

- [ ] [#107 — Auditar RTM órfã, fantasma e evidência vencida](https://github.com/pedrobertanhi/gerenciadordesalas/issues/107) — semana 45, 3 pts

## Documentação e entrega (6)

- [ ] [#55 — Manter RTM, registro de defeitos e riscos](https://github.com/pedrobertanhi/gerenciadordesalas/issues/55) — semana 36, 8 pts
- [ ] [#56 — Consolidar entrega, relatório final e apresentação](https://github.com/pedrobertanhi/gerenciadordesalas/issues/56) — semana 46, 0 pts
- [ ] [#77 — Escrever relatório final de qualidade](https://github.com/pedrobertanhi/gerenciadordesalas/issues/77) — semana 45, 8 pts
- [ ] [#79 — Preparar apresentação e demonstração do sistema](https://github.com/pedrobertanhi/gerenciadordesalas/issues/79) — semana 47, 8 pts
- [ ] [#80 — Validar aplicação executável e instalação do zero](https://github.com/pedrobertanhi/gerenciadordesalas/issues/80) — semana 45, 5 pts
- [ ] [#108 — Ensaiar prova oral, perguntas e contingência da demonstração](https://github.com/pedrobertanhi/gerenciadordesalas/issues/108) — semana 45, 3 pts

## Release (3)

- [ ] [#78 — Montar pacote de evidências da entrega](https://github.com/pedrobertanhi/gerenciadordesalas/issues/78) — semana 46, 8 pts
- [ ] [#82 — Criar release candidate e tag da entrega](https://github.com/pedrobertanhi/gerenciadordesalas/issues/82) — semana 46, 5 pts
- [ ] [#83 — Registrar decisão formal de qualidade](https://github.com/pedrobertanhi/gerenciadordesalas/issues/83) — semana 46, 5 pts

## Pós-entrega (4)

- [ ] [#74 — Tratar feedback com causa raiz e teste de regressão](https://github.com/pedrobertanhi/gerenciadordesalas/issues/74) — semana 48, 8 pts
- [ ] [#76 — Atualizar memória do projeto](https://github.com/pedrobertanhi/gerenciadordesalas/issues/76) — semana 48, 3 pts
- [ ] [#81 — Executar revisão integrada de rastreabilidade](https://github.com/pedrobertanhi/gerenciadordesalas/issues/81) — semana 49, 8 pts
- [ ] [#84 — Registrar retrospectiva por métricas e plano de evolução](https://github.com/pedrobertanhi/gerenciadordesalas/issues/84) — semana 50, 5 pts

## Itens adicionados pela auditoria das aulas

- #99 PRD e personas.
- #100 ATAM, utility tree e cenários mensuráveis.
- #101 catálogo reproduzível de casos.
- #102 técnicas de caixa-preta, branca e cinza.
- #103 prova de CI vermelha→verde e bloqueio da main.
- #104 matriz segura da integração externa.
- #105 evidência Red–Green–Refactor.
- #106 RNFs e modelo de carga mensuráveis.
- #107 auditoria de RTM órfã, fantasma e vencida.
- #108 defesa oral de oito minutos, perguntas e contingência.
- #109 SQA independente e gates.
- #110 testes negativos de vazamento em resposta, log e cache.

## Regras para atribuição futura

- Cada integrante recebe entregas importantes de Java e de testes.
- Ninguém fica responsável apenas por documentação ou interface.
- Concorrência, segurança, CI e RTM não ficam concentradas.
- A distribuição usa pontos, risco e dependências; não apenas quantidade de cards.
- Cada pessoa abre PRs próprios e revisa PRs de outra.
- A divisão aprovada será registrada em `docs/equipe/AUTORIA.md` e no Project.
