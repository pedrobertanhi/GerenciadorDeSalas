# Evidência de validação — Issue #15 (RES-01), telas de salas

## Escopo entregue

Esta evidência cobre a parte de telas de salas (#63, consolidada na #15). A API de salas já estava na `main`. A tela de manutenção (#67) segue em PR separado.

- listagem paginada de salas ativas, 10 por página, com filtro por capacidade mínima e localização;
- detalhe da sala;
- formulário de cadastro e de edição, com erros de validação junto de cada campo;
- confirmação de inativação, que preserva o histórico de reservas;
- rotas `/salas` liberadas na `SecurityConfig` somente para o administrador: o CRUD de salas é exclusivo dele, como pede a Issue, e os demais perfis consultam salas pela pesquisa de recursos.

## Testes localizáveis

- `SalaWebControllerTest`: 51 casos, renderizando as telas de verdade com a segurança real;
- `SalasNavegadorTest` e `SalasAutorizacaoResponsividadeNavegadorTest`: 7 casos em navegador Chrome (Selenium), com a aplicação completa em uma porta real e PostgreSQL em container.

Cobertura por critério da Issue:

- fluxo de cadastro, edição, inativação e consulta: cadastro válido, edição, confirmação de inativação e detalhe;
- capacidade e localização são validadas: capacidade vazia, zero, negativa, decimal, texto e acima do limite do Integer (2147483647), com toda capacidade inteira positiva dentro desse limite aceita; nome e localização vazios ou longos demais; nome duplicado;
- o CRUD é exclusivo do administrador: os outros perfis recebem 403 em todas as telas de salas e nos POST de escrita;
- erros não expõem detalhes internos: uma exceção inesperada devolve 500 sem o texto da causa, e o nome da sala é exibido com escape de HTML;
- responsividade: todas as telas declaram `viewport` e usam grade e tabela responsivas do Bootstrap;
- página inválida na listagem (texto, negativa, decimal ou grande demais): a tela de salas continua, com uma mensagem e a primeira página, sem resposta JSON de erro;
- testes em navegador real (E2E): CRUD completo pela interface (cadastro, edição e inativação), validação do navegador e do servidor, autorização (visitante e solicitante) e responsividade em celular (390 px), tablet (768 px) e desktop (1280 px).

## Como validar

Comando: `./mvnw --batch-mode --no-transfer-progress verify`.

Roteiro de validação manual, com o perfil administrador e depois com o solicitante:

1. entrar pela tela de login e abrir `/salas`;
2. cadastrar uma sala válida e conferir a mensagem de sucesso na lista;
3. tentar cadastrar com capacidade 0, com texto e com localização vazia, e conferir a mensagem junto de cada campo;
4. cadastrar de novo com o mesmo nome e conferir a mensagem de nome duplicado;
5. editar a sala, abrir o detalhe e inativar pela tela de confirmação;
6. conferir que a sala inativada some da lista;
7. tentar abrir `/salas` com o solicitante e conferir que o acesso é negado;
8. no navegador, reduzir a largura para 360 px, 768 px e 1280 px e conferir que a tabela rola na horizontal, os campos ocupam a largura e nada fica cortado.

## Critérios de aceite cobertos

- CRUD funciona somente para administrador: testes de autorização das telas e dos POST;
- capacidade positiva e localização obrigatória: casos de validação do formulário;
- inativação preserva histórico: a sala é inativada e não apagada, e a confirmação informa que as reservas permanecem.

## Decisões para o grupo

- a matriz de permissões libera a pesquisa de salas a todos os perfis, e o CRUD de salas é exclusivo do administrador, como pede a Issue: por isso as telas de `/salas` são restritas a ele;
- os testes em navegador usam Selenium com o Chrome sem interface gráfica. No CI o Chrome já vem instalado no `ubuntu-24.04`. Em um computador sem Docker ou sem Chrome eles ficam ignorados, e fora do CI não são obrigatórios; o roteiro manual continua como complemento;
- ainda não existe layout compartilhado com navegação por perfil (UI-01): as telas de salas são autônomas, como a de professores;
- inativar uma sala não cancela as reservas futuras dela: as existentes permanecem no histórico e novas reservas são recusadas;
- as telas usam o Bootstrap por CDN, como as demais.

## Resultado

Execução do **Maven Verify** do commit `8250f72`: <https://github.com/pedrobertanhi/GerenciadorDeSalas/actions/runs/42056257536>.

Resultado: `Tests run: 497, Failures: 0, Errors: 0, Skipped: 0`. O `8250f72` é o último commit de código. Nessa execução rodaram `SalaWebControllerTest` (51 casos) e, em navegador Chrome com PostgreSQL em container, `SalasNavegadorTest` (2 casos) e `SalasAutorizacaoResponsividadeNavegadorTest` (5 casos).