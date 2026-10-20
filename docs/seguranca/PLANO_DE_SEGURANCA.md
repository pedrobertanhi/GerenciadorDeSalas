# Plano de Verificação de Segurança

> Objetivo obrigatório: **zero bugs ou vulnerabilidades críticas confirmados e abertos na release**. Scanner verde isolado não prova segurança.

## Superfícies

Identidade e sessão; perfil e propriedade; reserva/concorrência; banco/migrations; interface/API; integração externa; logs/artifacts; dependências; repositório; container; pipeline.

## Controles em camadas

| Controle | Ferramenta/técnica | Issue |
|---|---|---|
| ameaça e OWASP | modelo, abuso e checklist | #92 |
| dependências | dependency review/SCA | #93 |
| SAST | SonarCloud + CodeQL | #53, #94 |
| segredos | secret scanning e push protection | #95 |
| filesystem/imagem | Trivy | #96 |
| DAST | OWASP ZAP em ambiente isolado | #97 |
| autorização | matriz de perfil, propriedade, recurso e estado | #13, #14 |
| entrada/erro | Validation, handler e assertions de ausência | #9, #110 |
| integração | WireMock, timeout e erro neutro | #104 |
| release | consolidação, triagem e parecer | #98 |

## Autorização e defesa em profundidade

1. Security valida identidade, token/expiração e sessão.
2. Caso de uso aplica política central com negação por padrão.
3. Consulta evita carregar estado indevido quando possível.
4. DTO expõe somente campos autorizados.
5. Log registra decisão/correlação sem segredo nem payload sensível.
6. Cache, se usado, inclui identidade/perfil/contexto na chave.

A equipe documenta uma política consistente de 401, 403 e 404; diferenças acidentais que permitem enumeração são defeito.

## Testes negativos obrigatórios

- solicitante altera reserva alheia;
- perfil tenta operação superior;
- token ausente, inválido, expirado ou revogado;
- entrada malformada, grande ou enum inválido;
- erro 500 não expõe stack trace/SQL;
- provedor devolve token fictício no corpo e ele não atravessa a API/log;
- resposta negada não contém dados do recurso;
- banco não muda após operação negada;
- log e artifacts não contêm senha, token, credencial ou dado pessoal real.

## Integração e pipeline seguros

- URL, credencial e timeouts externos por configuração.
- Secrets nunca literais, em fixtures ou logs.
- Workflow começa com `contents: read`.
- Evitar `pull_request_target` com checkout/execução de código não confiável.
- Actions estáveis e revisadas; política pode fixar SHA.
- Falha crítica não usa `continue-on-error`.
- Artifacts são sanitizados e possuem retenção mínima necessária.

## Triagem

Cada achado recebe origem, versão, severidade, prioridade, alcance, evidência sanitizada, ação, responsável, prazo, regressão e status. Falso positivo ou risco aceito exige justificativa e aprovador; não existe para “deixar verde”.

## Gate final

A #98 reprova a release se houver:

- vulnerabilidade/bug crítico confirmado e aberto;
- segredo confirmado;
- alerta crítico sem triagem;
- falha de autorização;
- dupla reserva possível;
- DAST/SAST/SCA/Trivy não executado sem exceção formal;
- evidência de outro commit/tag;
- resposta, log, JTL, HTML ou artifact com dado sensível.

## Evidências

Registrar ferramenta/versão, configuração, comando, ambiente, commit/tag, resultado bruto sanitizado, triagem e conclusão. O parecer declara limitações e nunca generaliza além do ambiente testado.

## Autenticação e contas de demonstração (SEC-02)

- Login por formulário com sessão no servidor (ADR-005); a senha é armazenada somente como hash BCrypt.
- Falha de login (senha errada, e-mail inexistente ou usuário inativo) recebe a mesma resposta 401, `CREDENCIAIS_INVALIDAS`, para não revelar se a conta existe ou está inativa.
- Os 401 e 403 produzidos pelo Spring Security usam o contrato padronizado (`ErroResposta`) com `correlationId`.
- CSRF permanece habilitado. O cookie de sessão é `HttpOnly` e `SameSite=Lax`, e `Secure` no perfil `prod`. O logout invalida a sessão.
- As contas de demonstração (`db/demo`) existem somente no perfil `dev`, usam e-mails `.test` e uma senha sintética documentada no próprio arquivo de seed. O perfil `dev` é de uso local e não deve ser exposto. O perfil `prod` nunca carrega essas contas.
- A política de 401, 403 e 404 por perfil e propriedade é a SEC-03 (#13).
