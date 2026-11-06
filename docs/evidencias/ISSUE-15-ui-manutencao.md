# Evidência de validação — Issue #15 (UI-08), tela de manutenção

## Escopo entregue

Esta evidência cobre a parte da tela de manutenção (#67, consolidada na #15). A API de bloqueios (MNT-01) já estava na `main`. As telas de salas (#63) foram entregues no PR anterior.

- agenda de bloqueios, do início mais recente ao mais antigo, 10 por página, com filtro por tipo de recurso e por estado;
- histórico: bloqueios concluídos e cancelados continuam na lista e no detalhe;
- registro de bloqueio, com um único seletor que agrupa os recursos ativos por tipo (salas, professores e materiais);
- edição de bloqueio ativo, sem troca do recurso;
- encerramento: concluir (somente depois do início) e cancelar;
- impacto sobre a disponibilidade, no detalhe de cada bloqueio;
- rotas `/manutencoes` liberadas na `SecurityConfig`: consulta para qualquer perfil autenticado e escrita somente para o administrador.

## Impacto sobre a disponibilidade

O detalhe mostra o que o bloqueio faz com o recurso e quantas reservas ativas existem no período:

- bloqueio ativo: o recurso consta como indisponível por manutenção no período;
- bloqueio concluído: vale até o instante da conclusão, e depois o recurso volta a ficar disponível;
- bloqueio cancelado: não afeta a disponibilidade, e nenhuma consulta de reservas é feita;
- reservas já existentes no período são apenas contadas: elas não são canceladas nem alteradas, conforme a política da MNT-02.

## Testes localizáveis

- `MaintenanceBlockWebControllerTest`: 61 casos, renderizando as telas de verdade com a segurança real;
- `ProfessorNomesPorIdTest` e `MaterialNomesPorIdTest`: 4 casos cada, nomes por identificador, inclusive de inativados, sem banco;
- `ManutencaoNavegadorTest` e `ManutencaoValidacaoAutorizacaoResponsividadeNavegadorTest`: 9 casos em navegador Chrome (Selenium), com a aplicação completa e PostgreSQL em container; as telas de salas têm os 7 casos em navegador do PR #167;
- `ReservationImpactoManutencaoTest`: 7 casos da contagem de reservas ativas por recurso, sem banco.

Cobertura por critério da Issue:

- somente o administrador altera a manutenção: as telas e os POST de registro, edição, conclusão e cancelamento devolvem 403 aos outros perfis, e a lista e o detalhe não exibem os botões de escrita;
- período inválido mostra mensagem compreensível: fim igual ou anterior ao início, fim no passado, datas vazias ou em formato inválido, motivo vazio ou com mais de 500 caracteres e recurso não escolhido, cada um com a mensagem junto do campo;
- histórico permanece consultável: concluídos e cancelados aparecem na lista, bloqueios encerrados não têm edição nem novas ações, e professores e materiais inativados continuam com o nome no histórico;
- interface funciona em desktop e mobile: todas as telas declaram `viewport` e usam a grade e a tabela responsivas do Bootstrap;
- edição recusada explica a causa: recurso que não pode ser trocado e bloqueio já concluído ou cancelado têm mensagens diferentes;
- página inválida na agenda: a tela continua, com uma mensagem e a primeira página, sem resposta JSON de erro;
- testes em navegador real (E2E): registro, edição, conclusão e cancelamento pela interface, validação de período e motivo, impacto sobre reservas existentes, autorização (visitante e solicitante) e responsividade em celular (390 px), tablet (768 px) e desktop (1280 px);
- erros não expõem detalhes internos: uma exceção inesperada devolve 500 sem o texto da causa, e o motivo é exibido com escape de HTML.
## Como validar

Comando: `./mvnw --batch-mode --no-transfer-progress verify`.

Roteiro de validação manual, com o perfil administrador e depois com o solicitante:

1. entrar pela tela de login e abrir `/manutencoes`;
2. registrar um bloqueio para uma sala, com início e fim no futuro, e conferir o impacto no detalhe;
3. tentar registrar com o fim antes do início, com o fim no passado e sem motivo, e conferir a mensagem junto de cada campo;
4. editar o bloqueio e conferir que o recurso aparece como texto, sem opção de troca;
5. tentar concluir um bloqueio que ainda não começou e conferir a mensagem;
6. cancelar o bloqueio e conferir que ele continua na lista, com o estado Cancelado, sem botões;
7. criar uma reserva da mesma sala no período, abrir o detalhe de um bloqueio ativo e conferir que a reserva é contada e continua ativa;
8. repetir a consulta com o solicitante e conferir que não há botões de escrita;
9. no navegador, reduzir a largura para 360 px, 768 px e 1280 px e conferir que a tabela rola na horizontal e os campos de data ocupam a largura.

## Critérios de aceite cobertos

- somente administrador altera manutenção: testes de autorização das telas e dos POST;
- período inválido mostra mensagem compreensível: casos de validação do formulário;
- histórico de manutenção permanece consultável: casos da agenda e do detalhe de bloqueios encerrados.

## Decisões para o grupo

- a criação de reservas ainda não consulta `MaintenanceBlockService.recursoEmManutencao` (pendência da Issue #22): a tela informa que o recurso consta como indisponível, mas a recusa de novas reservas depende dessa integração;
- as datas do formulário são interpretadas no horário de Brasília (`America/Sao_Paulo`), como no restante do projeto;
- a consulta de manutenção é liberada a todos os perfis, como na matriz de permissões ("Consultar manutenção": solicitante, responsável e administrador), e a escrita é exclusiva do administrador, como pede a Issue;
- o bloqueio é do recurso inteiro, sem quantidade parcial de material, como na MNT-01;
- os testes em navegador usam Selenium com o Chrome sem interface gráfica. No CI o Chrome já vem instalado no `ubuntu-24.04`. Em um computador sem Docker ou sem Chrome eles ficam ignorados, e fora do CI não são obrigatórios; o roteiro manual continua como complemento;
- ainda não existe layout compartilhado com navegação por perfil (UI-01).

## Resultado

Execução do **Maven Verify** do commit `75d2e12`: <https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42065036726>.

Resultado: `Tests run: 582, Failures: 0, Errors: 0, Skipped: 0`. O último commit de código é o `fc93ff8`; o `75d2e12` e os commits anteriores a ele só alteraram a documentação. Nessa execução rodaram `MaintenanceBlockWebControllerTest` (61 casos) e, em navegador Chrome com PostgreSQL em container, `ManutencaoNavegadorTest` (3 casos), `ManutencaoValidacaoAutorizacaoResponsividadeNavegadorTest` (6 casos) e os 7 casos de salas.	