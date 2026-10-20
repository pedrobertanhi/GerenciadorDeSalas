# Modelo de Ameaças

> Status: estrutura inicial da Issue #92.

## Ativos

- contas, perfis e sessões;
- reservas e agenda dos professores;
- salas, materiais e manutenção;
- histórico de auditoria;
- relatórios;
- credenciais e configurações;
- banco de dados e artefatos da release.

## Fronteiras de confiança

| ID | Fronteira | Dados que atravessam | Controle esperado |
|---|---|---|---|
| FT01 | Navegador → aplicação | credenciais e requisições | TLS no ambiente publicado, autenticação, validação |
| FT02 | Aplicação → PostgreSQL | dados do domínio | credencial por ambiente, menor privilégio |
| FT03 | Aplicação → API externa | notificações | timeout, payload mínimo, WireMock em testes |
| FT04 | GitHub Actions → serviços | tokens de CI | GitHub Secrets, permissões mínimas |

## Ameaças iniciais

| ID | Ameaça | Ativo | Impacto | Controle | Teste | Issue | Status |
|---|---|---|---|---|---|---|---|
| A01 | Acesso a ação de outro perfil | usuários/reservas | Crítico | autorização no backend | matriz positiva/negativa | #13, #14 | Planejada |
| A02 | Dupla reserva concorrente | reservas/recursos | Crítico | transação e lock | teste simultâneo real | #32, #33 | Planejada |
| A03 | Segredo no repositório | credenciais | Crítico | secret scanning/push protection | validação controlada | #95 | Planejada |
| A04 | Dependência vulnerável | aplicação | Crítico | SCA/dependency review | gate em PR | #93 | Planejada |
| A05 | Injeção ou entrada malformada | banco/aplicação | Alto | DTO, validação e persistência segura | testes negativos + DAST | #9, #97 | Planejada |
| A06 | Exposição de erro interno | aplicação/dados | Alto | handler seguro | 400/403/404/409/500 | #9, #14 | Planejada |
| A07 | Alteração sem rastreabilidade | auditoria | Alto | evento imutável e atômico | integração | #39, #88 | Planejada |
| A08 | Imagem com pacote vulnerável | release | Crítico | Trivy image | scan por digest | #96 | Planejada |
| A09 | Tentativas ilimitadas de login (adivinhação de senha) | contas, perfis e sessões | Médio | limite temporário e progressivo por conta e origem, resposta neutra, chaves anonimizadas e estado limitado/expirável | LimitadorTentativasLoginTest + LimiteTentativasLoginWebTest | #12, #133 | Implementada |

## A09 — decisões do controle

- limite inicial: cinco falhas dentro de uma janela de quinze minutos;
- bloqueio temporário inicial de trinta segundos, dobrado após nova falha e limitado a quinze minutos;
- contagem independente por conta e por endereço observado pela aplicação;
- conta e origem armazenadas somente como resumo SHA-256, sem credenciais em logs;
- no máximo dez mil chaves mantidas em memória, com descarte das menos recentes;
- sucesso limpa o estado correspondente e a expiração impede bloqueio permanente;
- resposta de limitação não informa se a conta existe, está inativa ou possui senha incorreta;
- em implantação com múltiplas instâncias, o estado deverá migrar para armazenamento compartilhado.

## Revisão

Atualizar quando surgir novo ator, integração, dado sensível, endpoint ou mudança arquitetural. Cada ameaça adicionada ou alterada gera commit próprio.
