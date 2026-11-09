# Diagnóstico inicial — SEC-04 (#14)

> Estado: levantamento inicial, **não** constitui aprovação de segurança nem evidência de execução de scanners.

## Escopo

A issue #14 agrega SEC-04, SEC-07 a SEC-14. A conclusão exige testes de autenticação/autorização e evidências executadas de SCA, CodeQL, detecção de segredos, Trivy, ZAP (quando aplicável) e gate de release.

## Inventário verificado na branch `main`

| Área | Artefato encontrado | Situação do diagnóstico |
|---|---|---|
| CI Java | `.github/workflows/ci.yml` | Executa `./mvnw verify` em PR e push na main; não comprova por si só scanners de segurança |
| Autenticação | `AutenticacaoWebTest.java` | Contém cenários de login, credenciais inválidas, usuário inativo, CSRF e logout |
| Autorização | `AutorizacaoPorPerfilWebTest.java`, `AutorizacaoTest.java` | Testes existentes; cobertura de todas as rotas e perfis ainda precisa de matriz conferida |
| Falhas | `FalhasSegurancaTest.java`, `RespostasDeSegurancaTest.java` | Testes existentes; verificar assertions de ausência de vazamento |
| Modelo de ameaças | `docs/seguranca/MODELO_DE_AMEACAS.md` | Arquivo presente; revisão cruzada e rastreabilidade ainda a validar |
| Matriz de permissões | `docs/seguranca/MATRIZ_DE_PERMISSOES.md` | Arquivo presente; comparar com `SecurityConfig` |
| Relatório de testes | `docs/testes/RELATORIO_TESTES_SEGURANCA.md` | Declara sucesso; vincular a commit, execução real e logs do CI antes de aceitar |
| Parecer final | `docs/seguranca/PARECER_DE_SEGURANCA.md` | Declara status não emitido e controles pendentes |

## Riscos e próximos passos

1. Conferir a matriz endpoint × método HTTP × perfil, incluindo acesso horizontal e vertical; criar testes para lacunas.
2. Conferir se falhas de autenticação/autorização geram status e respostas neutras, sem stack trace, SQL, token ou informações internas.
3. Habilitar e executar scanners exigidos pela issue; registrar versão, commit, resultado e triagem. Não interpretar ausência de alertas como execução bem-sucedida.
4. Validar gates com cenários controlados e sintéticos, sem inserir credenciais reais nem vulnerabilidades no histórico principal.
5. Consolidar evidências no relatório e emitir parecer somente após revisão independente e zero vulnerabilidade crítica confirmada aberta.

## Critério para encerrar

Não marcar #14 como concluída enquanto não houver resultados de CI e segurança rastreáveis à mesma revisão/release, cobertura positiva e negativa dos perfis e aprovação cruzada.
