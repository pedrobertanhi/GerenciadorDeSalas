# Matriz de segurança das rotas — ENT-02

Inventário do código em `64ed003` ([PR #189](https://github.com/pedrobertanhi/GerenciadorDeSalas/pull/189)), com base nos controladores, no `SecurityConfig` e nas regras dos serviços. A [matriz funcional](MATRIZ_DE_PERMISSOES.md) descreve os requisitos. A tabela abaixo registra o que o código expõe.

**P** permitido ao perfil, sujeito a validação e estado do recurso. **C** condicionado à propriedade. **N** negado. Várias rotas na mesma célula compartilham o método e a regra. `{id}` é o identificador do recurso.

| Método | Rota | SOLICITANTE | RESPONSAVEL | ADMINISTRADOR |
|---|---|---|---|---|
| GET | /login | P | P | P |
| POST | /login, /logout | P | P | P |
| GET | /usuarios, /usuarios/novo, /usuarios/{id}/editar, /usuarios/{id}/situacao | N | N | P |
| POST | /usuarios, /usuarios/{id}/editar, /usuarios/{id}/situacao | N | N | P |
| GET | /api/usuarios, /api/usuarios/{id} | N | N | P |
| POST | /api/usuarios | N | N | P |
| PUT | /api/usuarios/{id} | N | N | P |
| PATCH | /api/usuarios/{id}/ativar, /api/usuarios/{id}/inativar | N | N | P |
| GET | /salas, /salas/novo, /salas/{id}, /salas/{id}/editar, /salas/{id}/inativar | N | N | P |
| POST | /salas, /salas/{id}, /salas/{id}/inativar | N | N | P |
| GET | /api/salas, /api/salas/{id} | P | P | P |
| POST | /api/salas | N | N | P |
| PUT | /api/salas/{id} | N | N | P |
| PATCH | /api/salas/{id}/inativar | N | N | P |
| GET | /professores, /api/professores, /api/professores/{id} | P | P | P |
| GET | /professores/novo, /professores/{id}/editar | N | N | P |
| POST | /professores, /professores/{id}, /professores/{id}/desativar, /api/professores | N | N | P |
| PUT | /api/professores/{id} | N | N | P |
| PATCH | /api/professores/{id}/desativar | N | N | P |
| GET | /api/materiais, /api/materiais/{id} | P | P | P |
| POST | /api/materiais | N | N | P |
| PUT | /api/materiais/{id} | N | N | P |
| PATCH | /api/materiais/{id}/desativar | N | N | P |
| GET | /manutencoes, /manutencoes/{id}, /api/manutencoes, /api/manutencoes/{id} | P | P | P |
| GET | /manutencoes/novo, /manutencoes/{id}/editar | N | N | P |
| POST | /manutencoes, /manutencoes/{id}, /manutencoes/{id}/concluir, /manutencoes/{id}/cancelar, /api/manutencoes | N | N | P |
| PUT | /api/manutencoes/{id} | N | N | P |
| PATCH | /api/manutencoes/{id}/concluir, /api/manutencoes/{id}/cancelar | N | N | P |
| GET | /recursos/pesquisa, /api/disponibilidade/consulta | P | P | P |
| GET | /reservas, /reservas/{id}, /reservas/{id}/editar, /reservas/{id}/cancelar | C | P | P |
| GET | /reservas/nova | P | N | N |
| POST | /reservas, /api/reservas | P | N | N |
| POST | /reservas/{id}, /reservas/{id}/cancelar, /api/reservas/{id}/cancelar | C | P | P |
| PUT | /api/reservas/{id} | C | P | P |
| GET | /aprovacoes, /aprovacoes/{id}, /api/aprovacoes | N | P | N |
| POST | /aprovacoes/{id}/aprovar, /aprovacoes/{id}/rejeitar, /aprovacoes/{id}/validar-docente, /aprovacoes/{id}/rejeitar-docente | N | P | N |
| POST | /api/reservas/{id}/aprovar, /api/reservas/{id}/rejeitar, /api/reservas/{id}/validar-docente, /api/reservas/{id}/rejeitar-docente | N | P | N |
| GET | /api/auditoria | N | N | P |
| GET | /swagger-ui.html, /swagger-ui/**, /v3/api-docs, /v3/api-docs/** | P¹ | P¹ | P¹ |
| GET | /v3/api-docs.yaml | N | N | N |
| DELETE | /api/usuarios/**, /api/admin/usuarios/**, /api/reservas/**, /api/movimentacoes/**, /api/auditoria/** | N | N | N |
| POST, PUT, PATCH | /api/auditoria/** | N | N | N |

¹ O Springdoc fornece as rotas de documentação. A configuração libera os padrões `/swagger-ui/**`, `/swagger-ui.html` e `/v3/api-docs/**` para usuários autenticados. A rota padrão `/v3/api-docs.yaml` fica fora desses padrões e é negada aos três perfis pela regra final.

- A política termina com `anyRequest().denyAll()`. Alguns padrões reservam acesso para rotas ainda sem controlador correspondente, como `/api/movimentacoes/**`, `/api/perfil` e `/api/reservas/minhas/**`. Um matcher não cria um endpoint.
- A tabela lista métodos declarados. HEAD e OPTIONS implícitos passam primeiro pela política HTTP. Regras restritas a GET não se estendem automaticamente a HEAD ou OPTIONS. Métodos de escrita passam pela proteção CSRF.
- GET e POST de login são públicos. Sem sessão válida, rotas protegidas retornam 401 para API ou redirecionam para `/login` quando o cliente solicita HTML. Falha de CSRF pode resultar em 403 antes da autorização. Sessão válida com perfil indevido recebe 403.
- Em reservas, o solicitante só lista e acessa as próprias reservas. O serviço distingue reserva inexistente (404) de reserva existente pertencente a outro solicitante (403). Responsáveis e administradores têm acesso operacional conforme o estado da reserva. As decisões de aprovação são exclusivas do responsável.

## Cobertura e lacunas

| Cenário | Testes no repositório | Evidência a completar |
|---|---|---|
| Autenticação, logout, sessão ausente e falhas de segurança | `AutenticacaoWebTest`, `FalhasSegurancaTest`, `AutorizacaoPorPerfilWebTest` | Vincular a execução da release |
| Administração por perfil e proteção CSRF | `UsuarioControllerSecurityTest`, `UsuarioWebControllerTest`, `UsuarioAdminIntegracaoTest` | Validação manual em desktop e celular |
| Conta inativa e sessão com perfil revogado | `FiltroUsuarioAtivoTest`, `UsuarioInativoSessaoWebTest`, `UsuarioAdminIntegracaoTest` | Vincular a execução da release |
| Propriedade de reservas e autorização vertical em decisões | `ReservationManagementServiceTest`, `ReservationCriacaoIntegrationTest`, `AprovacaoDecisaoIntegrationTest`, `AprovacaoDocenteIntegrationTest`, `AprovacaoWebIntegrationTest` | Confirmar cobertura HTTP por operação e perfil |
| Entrada inválida e mensagens sem dados internos | `UsuarioControllerSecurityTest`, `UsuarioWebControllerTest`, `Erro400ValidacaoTest`, `Erro500InternoTest` | Revisar logs e artefatos da release quanto a credenciais, tokens, SQL e stack traces |
| Auditoria da alteração de perfil e respostas sem credenciais | `UsuarioAdminIntegracaoTest`, `UsuarioAdminServiceTest`, `UsuarioControllerSecurityTest` | Vincular a execução da release |

A tabela aponta testes existentes. Ela não equivale à execução exaustiva de todas as combinações de rota, método, perfil e estado. Os controles de release são acompanhados no [parecer](PARECER_DE_SEGURANCA.md).

### Pontos para a revisão da release

- `GlobalExceptionHandler` responde com mensagem neutra no 500, mas envia a exceção ao logger. A ausência de SQL, credenciais e stack traces nos logs ou artefatos compartilhados continua sem comprovação.
- O commit inventariado contém os workflows `ci.yml` e `docker-compose.yml`. Não há arquivos versionados para CodeQL, Trivy, ZAP, revisão de dependências ou Dependabot nessa base. A disponibilidade e o estado dos controles gerenciados no GitHub, como secret scanning, precisam ser verificados nas configurações do repositório.
