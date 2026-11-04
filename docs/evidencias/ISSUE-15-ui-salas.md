# Evidência de validação — Issue #15 (RES-01), telas de salas

## Escopo entregue

Esta evidência cobre a parte de telas de salas (#63, consolidada na #15). A API de salas já estava na `main`. A tela de manutenção (#67) segue em PR separado.

- listagem paginada de salas ativas, 10 por página, com filtro por capacidade mínima e localização;
- detalhe da sala;
- formulário de cadastro e de edição, com erros de validação junto de cada campo;
- confirmação de inativação, que preserva o histórico de reservas;
- rotas `/salas` liberadas na `SecurityConfig`: consulta para qualquer perfil autenticado e escrita somente para o administrador.

## Testes localizáveis

- `SalaWebControllerTest`: 43 casos, renderizando as telas de verdade com a segurança real.

Cobertura por critério da Issue:

- fluxo de cadastro, edição, inativação e consulta: cadastro válido, edição, confirmação de inativação e detalhe;
- capacidade e localização são validadas: capacidade vazia, zero, negativa, decimal, texto e acima de seis dígitos; nome e localização vazios ou longos demais; nome duplicado;
- ações de escrita aparecem somente ao administrador: a lista e o detalhe não exibem Cadastrar, Editar nem Inativar para os outros perfis, e as telas e os POST de escrita devolvem 403;
- erros não expõem detalhes internos: uma exceção inesperada devolve 500 sem o texto da causa, e o nome da sala é exibido com escape de HTML;
- responsividade: todas as telas declaram `viewport` e usam grade e tabela responsivas do Bootstrap.

## Como validar

Comando: `./mvnw --batch-mode --no-transfer-progress verify`.

Roteiro de validação manual, com o perfil administrador e depois com o solicitante:

1. entrar pela tela de login e abrir `/salas`;
2. cadastrar uma sala válida e conferir a mensagem de sucesso na lista;
3. tentar cadastrar com capacidade 0, com texto e com localização vazia, e conferir a mensagem junto de cada campo;
4. cadastrar de novo com o mesmo nome e conferir a mensagem de nome duplicado;
5. editar a sala, abrir o detalhe e inativar pela tela de confirmação;
6. conferir que a sala inativada some da lista;
7. repetir a consulta com o solicitante e conferir que não há botões de escrita;
8. no navegador, reduzir a largura para 360 px, 768 px e 1280 px e conferir que a tabela rola na horizontal, os campos ocupam a largura e nada fica cortado.

## Critérios de aceite cobertos

- CRUD funciona somente para administrador: testes de autorização das telas e dos POST;
- capacidade positiva e localização obrigatória: casos de validação do formulário;
- inativação preserva histórico: a sala é inativada e não apagada, e a confirmação informa que as reservas permanecem.

## Decisões para o grupo

- o projeto não tem ferramenta de teste em navegador real (Playwright ou Selenium): os testes renderizam as telas com MockMvc e o roteiro manual cobre a visualização;
- ainda não existe layout compartilhado com navegação por perfil (UI-01): as telas de salas são autônomas, como a de professores;
- inativar uma sala não cancela as reservas futuras dela: as existentes permanecem no histórico e novas reservas são recusadas;
- as telas usam o Bootstrap por CDN, como as demais.

## Resultado

A registrar com o link da execução do **Maven Verify** do commit mesclado.