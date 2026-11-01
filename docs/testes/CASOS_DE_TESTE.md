# Catálogo de Casos de Teste

> Status: planejamento. IDs são estáveis; automação e execução serão vinculadas quando existirem.

## Template obrigatório

| Campo | Conteúdo |
|---|---|
| ID | Identificador único e estável |
| Título | Comportamento verificado em uma linha |
| Origem | RF, RNF, regra crítica, risco ou cenário ATAM |
| Nível/técnica | Unidade, integração, API, E2E, carga; equivalência, limite ou decisão |
| Pré-condições | Estado concreto antes da ação |
| Dados | Valores sintéticos e reproduzíveis |
| Passos | Sequência numerada e independente |
| Resultado esperado | Oráculo objetivo, incluindo efeitos ausentes |
| Automação | Classe/método ou JMX, quando criado |
| Evidência | Commit, PR, execução e artifact do mesmo escopo |
| Status | Planejado, automatizado, aprovado, falhou, bloqueado ou não executado |

## Casos críticos iniciais

### CT-TEMPO-01 — término igual ao início

- **Origem:** RC-01 / RF-04.
- **Nível/técnica:** unidade e API; valor limite.
- **Pré-condições:** solicitante autorizado e recursos ativos.
- **Dados:** início `2026-09-21T10:00:00-03:00`; término igual.
- **Passos:** enviar criação de reserva.
- **Esperado:** validação recusada, nenhuma reserva persistida e código de erro estável.

### CT-TEMPO-02 — intervalos adjacentes

- **Origem:** RC-02 / RF-05.
- **Nível/técnica:** integração; valor limite.
- **Pré-condições:** reserva existente das 10:00 às 11:00.
- **Dados:** nova reserva das 11:00 às 12:00 para a mesma sala.
- **Passos:** criar a segunda reserva.
- **Esperado:** aceita, pois não existe interseção real.

### CT-CONC-01 — disputa simultânea da mesma sala

- **Origem:** RC-03, RC-06 / QA-CON-01.
- **Nível/técnica:** integração concorrente com PostgreSQL/Testcontainers.
- **Pré-condições:** sala ativa e livre; duas transações sincronizadas.
- **Dados:** mesmo recurso e intervalo.
- **Passos:** liberar as duas solicitações ao mesmo tempo; aguardar; consultar banco.
- **Esperado:** uma aceitação, um conflito controlado e exatamente uma reserva válida.

### CT-PROF-01 — choque na agenda docente

- **Origem:** RC-04 / RF-05.
- **Nível/técnica:** integração e tabela de decisão.
- **Pré-condições:** professor já alocado no intervalo em outra sala.
- **Dados:** segunda sala, mesmo professor e período sobreposto.
- **Passos:** solicitar nova reserva.
- **Esperado:** rejeição por conflito do professor e nenhuma gravação.

### CT-MAT-01 — quantidade insuficiente

- **Origem:** RC-05 / RF-05.
- **Nível/técnica:** integração; valores limite.
- **Pré-condições:** dez unidades totais e oito já reservadas.
- **Dados:** solicitar 2 e 3 unidades em casos independentes.
- **Esperado:** 2 é aceito no limite; 3 é recusado; quantidade nunca fica negativa.

### CT-MNT-01 — recurso em manutenção

- **Origem:** RC-07 / RF-08.
- **Nível/técnica:** unidade, integração e API.
- **Pré-condições:** bloqueio ativo que intersecta o intervalo.
- **Dados:** sala e período bloqueados.
- **Passos:** solicitar reserva.
- **Esperado:** conflito de manutenção, zero reserva e histórico preservado.

### CT-AUTH-01 — solicitante altera reserva alheia

- **Origem:** RF-01 / QA-SEC-01.
- **Nível/técnica:** API caixa-preta e autorização horizontal.
- **Pré-condições:** usuário A autenticado; reserva pertencente a B.
- **Dados:** alteração válida enviada por A.
- **Passos:** chamar endpoint; consultar banco e logs sanitizados.
- **Esperado:** negação consistente; estado inalterado; resposta/log sem dados da reserva, token, SQL ou stack trace.

### CT-APR-01 — aprovação restrita por perfil incorreto

- **Origem:** RC-10 / RF-07.
- **Nível/técnica:** unidade, API e tabela de decisão.
- **Pré-condições:** reserva restrita em SOLICITADA.
- **Dados:** decisões por Solicitante, Administrador e Responsável.
- **Passos:** tentar aprovar em execuções independentes.
- **Esperado:** somente Responsável aprova; negativas mantêm estado e geram auditoria quando aplicável.

### CT-STATE-01 — transição inválida

- **Origem:** RC-08, RC-09.
- **Nível/técnica:** unidade parametrizada.
- **Pré-condições:** reserva em cada estado de origem.
- **Dados:** tabela completa origem × evento.
- **Passos:** solicitar transição.
- **Esperado:** transições permitidas resultam no estado correto; demais são negadas com código estável.

### CT-DEL-01 — exclusão após início

- **Origem:** RC-11.
- **Nível/técnica:** integração/API.
- **Pré-condições:** reserva EM_USO.
- **Passos:** solicitar exclusão.
- **Esperado:** operação negada; reserva e auditoria preservadas.

### CT-AUD-01 — auditoria de estado

- **Origem:** RC-12 / RF-10.
- **Nível/técnica:** integração.
- **Pré-condições:** usuário e reserva identificados.
- **Passos:** executar transição válida; consultar histórico.
- **Esperado:** um registro com reserva, origem, destino, autor, instante controlado e correlação.

### CT-INT-01 — provedor retorna erro sensível

- **Origem:** RF-11 / QA-INT-01.
- **Nível/técnica:** WireMock/API.
- **Pré-condições:** provedor simulado devolve 500 e inclui token fictício no corpo.
- **Passos:** disparar notificação; observar resposta e logs.
- **Esperado:** erro interno neutro; token/corpo não atravessa a API nem os logs; reserva não é corrompida.


## Casos complementares reproduzíveis — QA-01 (#48)

Os casos abaixo são especificações de teste, **não evidências de execução**. Cada cenário parte de massa independente e deve ser ligado à automação e ao resultado real na RTM.

### CT-DISP-01 — disponibilidade exclui sala reservada

- **Origem:** RF-03, RF-05, RC-03; issue #20.
- **Nível/técnica:** integração/API; partição de equivalência e caixa-cinza.
- **Pré-condições:** sala A ativa, sala B ativa; somente A tem reserva ativa de `2026-11-10T09:00:00-03:00` a `2026-11-10T10:00:00-03:00`. Banco PostgreSQL isolado.
- **Dados:** consultar `2026-11-10T09:30:00-03:00` até `2026-11-10T10:30:00-03:00`.
- **Passos:** (1) criar massa sintética; (2) autenticar solicitante; (3) consultar disponibilidade; (4) conferir IDs retornados.
- **Esperado:** B aparece; A não aparece; nenhuma reserva é alterada.
- **Automação/evidência:** integração/API a implementar em #20; commit e CI pendentes.
- **Status:** planejado.

### CT-DISP-02 — adjacência não bloqueia disponibilidade

- **Origem:** RF-03, RC-02; issue #20.
- **Nível/técnica:** integração/API; análise de valor limite.
- **Pré-condições:** sala A ativa com reserva de `2026-11-10T09:00:00-03:00` a `2026-11-10T10:00:00-03:00`.
- **Dados:** três consultas independentes: 09:59–10:30, 10:00–11:00 e 08:00–09:00, todas em 10/11/2026 com deslocamento -03:00.
- **Passos:** (1) reiniciar massa entre casos; (2) autenticar; (3) consultar cada intervalo; (4) comparar IDs.
- **Esperado:** A ausente na primeira consulta; A presente nas outras duas; nenhuma mudança no banco.
- **Automação/evidência:** parametrizado em #20; execução pendente.
- **Status:** planejado.

### CT-MNT-02 — manutenção bloqueia somente o intervalo afetado

- **Origem:** RF-08, RC-07; issues #20 e #22 incorporada.
- **Nível/técnica:** integração/API; tabela de decisão e fronteiras.
- **Pré-condições:** sala A ativa; bloqueio ATIVO de 09:00 a 10:00 em 10/11/2026, fuso -03:00; nenhuma reserva.
- **Dados:** tentativas de reserva de 08:00–09:00, 09:00–10:00, 09:30–10:30 e 10:00–11:00.
- **Passos:** (1) criar bloqueio; (2) autenticar solicitante; (3) executar cada tentativa isoladamente; (4) consultar reservas persistidas.
- **Esperado:** primeira e última permitidas; segunda e terceira recusadas com 409 controlado; tentativas recusadas não persistem reservas.
- **Automação/evidência:** integração PostgreSQL em #20; execução pendente.
- **Status:** planejado.

### CT-SEC-02 — perfil sem permissão não revela recurso

- **Origem:** RF-01, RNF-ERR-01; issue #14.
- **Nível/técnica:** API caixa-preta; tabela de decisão e teste negativo de ausência.
- **Pré-condições:** usuário sintético autenticado sem permissão para rota administrativa; recurso de outro usuário cadastrado.
- **Dados:** sessão sem privilégios; identificador existente e inexistente em execuções independentes.
- **Passos:** (1) iniciar sessão; (2) solicitar rota protegida; (3) verificar corpo e cabeçalhos; (4) conferir banco.
- **Esperado:** negação conforme política 403/404 documentada; nenhum campo privado, token, SQL ou stack trace na resposta; banco inalterado.
- **Automação/evidência:** suíte de segurança #14; execução pendente.
- **Status:** planejado.

## Execução cruzada

Antes de fechar #101, outro integrante deve executar uma amostra sem explicação oral. Dúvida ou dependência implícita significa caso incompleto. Nenhum caso usa senha, token, matrícula ou dado pessoal real.
