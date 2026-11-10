# Matriz de Permissões

A [matriz de segurança das rotas](MATRIZ_DE_ROTAS.md) detalha a política HTTP implementada e os testes existentes.

## Objetivo

Definir as ações permitidas aos perfis Solicitante, Responsável e Administrador.

A matriz atende principalmente ao RF-01 e complementa as regras de propriedade, recurso e estado das reservas.

## Legenda

- **P:** permitido;
- **C:** permitido somente mediante condição;
- **N:** negado.

## Princípios

- toda ação começa negada e precisa de permissão explícita;
- autenticação não significa autorização;
- perfil, propriedade, recurso e estado devem ser avaliados;
- a validação ocorre no serviço, não somente na interface;
- Administrador não herda automaticamente ações operacionais do Responsável;
- ações permitidas continuam sujeitas à validação dos dados;
- toda ação sensível deve gerar auditoria.

## Usuários e acesso

| Ação | Solicitante | Responsável | Administrador | Condição |
|---|---:|---:|---:|---|
| Autenticar | C | C | C | Usuário ativo e credenciais válidas |
| Consultar o próprio perfil | P | P | P | Somente dados próprios |
| Alterar a própria senha | P | P | P | Confirmar identidade e aplicar política de senha |
| Consultar todos os usuários | N | N | P | Finalidade administrativa |
| Criar usuário | N | N | P | E-mail único e perfil válido |
| Alterar perfil de usuário | N | N | P | Não remover o último Administrador ativo |
| Ativar ou desativar usuário | N | N | P | Ação auditada |
| Excluir usuário com histórico | N | N | N | Exclusão física proibida |

## Professores

| Ação | Solicitante | Responsável | Administrador | Condição |
|---|---:|---:|---:|---|
| Consultar professores ativos | P | P | P | Dados necessários ao agendamento |
| Cadastrar professor | N | N | P | Matrícula e e-mail válidos |
| Alterar cadastro de professor | N | N | P | Preservar histórico |
| Ativar ou desativar professor | N | N | P | Ação auditada |
| Validar professor | N | P | N | Responsável registra a decisão |
| Consultar agenda docente | C | P | P | Solicitante acessa somente dados necessários à disponibilidade |

## Salas e materiais

| Ação | Solicitante | Responsável | Administrador | Condição |
|---|---:|---:|---:|---|
| Pesquisar salas e materiais | P | P | P | Somente informações permitidas |
| Consultar disponibilidade | P | P | P | Intervalo válido |
| Cadastrar sala ou material | N | N | P | Dados válidos e sem duplicidade |
| Alterar sala ou material | N | N | P | Preservar referências históricas |
| Ativar ou desativar recurso | N | N | P | Ação auditada |
| Definir recurso como restrito | N | N | P | Mudança auditada |
| Excluir recurso com histórico | N | N | N | Exclusão física proibida |

## Reservas

| Ação | Solicitante | Responsável | Administrador | Condição |
|---|---:|---:|---:|---|
| Criar reserva | C | N | N | Somente em nome próprio e com período válido |
| Consultar reserva própria | C | N | N | Propriedade confirmada |
| Consultar reservas operacionais | N | C | P | Responsável acessa apenas as necessárias ao trabalho |
| Alterar reserva | C | N | N | Propriedade confirmada e estado `SOLICITADA` ou `APROVADA` |
| Cancelar reserva | C | N | N | Propriedade confirmada e estado permitido |
| Aprovar recurso restrito | N | C | N | Estado `SOLICITADA` e recursos disponíveis |
| Rejeitar solicitação | N | C | N | Recurso restrito, estado `SOLICITADA` e justificativa obrigatória |
| Iniciar utilização | N | C | N | Estado `APROVADA` e horário alcançado |
| Concluir utilização | N | C | N | Estado `EM_USO` e sem devolução pendente |
| Registrar não comparecimento | N | C | N | Estado `APROVADA` e horário já iniciado |
| Excluir reserva | N | N | N | Histórico deve ser preservado |
| Forçar transição inválida | N | N | N | Sempre proibido |

## Manutenção

| Ação | Solicitante | Responsável | Administrador | Condição |
|---|---:|---:|---:|---|
| Consultar manutenção | P | P | P | Informações necessárias à disponibilidade |
| Criar manutenção | N | N | P | Período válido e recurso existente |
| Alterar manutenção | N | N | C | Estado ainda permite alteração |
| Cancelar manutenção | N | N | C | Manutenção não concluída |
| Concluir manutenção | N | N | C | Manutenção em andamento |
| Excluir manutenção concluída | N | N | N | Histórico deve ser preservado |

## Movimentação de materiais

| Ação | Solicitante | Responsável | Administrador | Condição |
|---|---:|---:|---:|---|
| Consultar movimentações da própria reserva | C | N | N | Propriedade confirmada |
| Consultar movimentações operacionais | N | P | P | Acesso necessário à operação ou auditoria |
| Registrar retirada | N | C | N | Material reservado e quantidade válida |
| Registrar devolução | N | C | N | Quantidade previamente retirada |
| Corrigir movimentação | N | N | N | Registrar evento compensatório, sem alterar histórico |
| Excluir movimentação | N | N | N | Histórico deve ser preservado |

## Auditoria

| Ação | Solicitante | Responsável | Administrador | Condição |
|---|---:|---:|---:|---|
| Consultar histórico da própria reserva | C | N | N | Propriedade confirmada |
| Consultar histórico operacional | N | C | P | Responsável acessa somente eventos relacionados ao trabalho |
| Consultar auditoria completa | N | N | P | Finalidade administrativa autorizada |
| Criar evento manual de auditoria | N | N | N | Eventos são produzidos pelas operações do sistema |
| Alterar ou excluir evento | N | N | N | Auditoria é somente de inclusão |

## Notificações e relatórios

| Ação | Solicitante | Responsável | Administrador | Condição |
|---|---:|---:|---:|---|
| Consultar próprias notificações | C | N | N | Destinatário confirmado |
| Consultar estado operacional de notificações | N | C | P | Sem exposição de conteúdo sensível |
| Configurar integração externa | N | N | P | Credenciais fora do repositório |
| Consultar relatório próprio | C | N | N | Somente reservas ou carga relacionadas ao usuário |
| Consultar relatório operacional | N | P | P | Período explícito |
| Consultar documentação da API | P | P | P | Usuário autenticado |

## Ações automáticas do sistema

O Sistema não corresponde a um perfil humano. Ele pode executar somente ações internas previstas:

- aprovar automaticamente reservas sem recursos restritos;
- gerar eventos de auditoria;
- calcular disponibilidade;
- produzir notificações;
- marcar falhas controladas de integração.

Toda ação automática deve registrar origem `SISTEMA`, instante e identificador de correlação.

## Decisão de autorização

A ação `Autenticar` é a exceção à exigência de uma sessão já autenticada. Ela exige usuário ativo e credenciais válidas.

Para as demais ações protegidas, a operação somente é permitida quando todas as condições forem verdadeiras:

1. usuário autenticado;
2. usuário ativo;
3. perfil autorizado;
4. propriedade confirmada, quando exigida;
5. recurso acessível;
6. estado atual compatível;
7. dados válidos.

Se qualquer condição falhar, a ação deve ser negada sem expor detalhes internos.

A escolha definitiva entre responder `403` ou `404` para ocultar recursos restritos deve ser única, documentada e testada antes da implementação.
