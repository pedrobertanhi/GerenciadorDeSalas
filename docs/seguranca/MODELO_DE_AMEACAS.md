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
| A09 | Tentativas ilimitadas de login (adivinhação de senha) | contas, perfis e sessões | Médio | limite e atraso progressivo de tentativas, com resposta neutra e uniforme | teste de tentativas repetidas | #12 (origem); Issue a definir | Planejada |

## Revisão

Atualizar quando surgir novo ator, integração, dado sensível, endpoint ou mudança arquitetural. Cada ameaça adicionada ou alterada gera commit próprio.
