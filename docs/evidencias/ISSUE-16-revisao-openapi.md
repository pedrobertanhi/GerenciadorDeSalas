# Revisão do PR #142 — contratos e alcance de leitura

## Base inspecionada

Commit `46d930dd7e085c2c2c0e1b582f9a38539da52e98`, branch `feature/16-professores-competencias`.
A revisão do Sourcery foi publicada em 22/10/2026 00:20 UTC (21/10/2026 21:20 em Brasília).
O thread em ProfessorController solicita contratos OpenAPI. As observações sobre acesso e persistência exigem avaliação humana separada.

## Correção implementada

- Integração springdoc-openapi-starter-webmvc-ui 2.8.17, conforme ADR-002 e Spring Boot 3.5.
- Contratos dos cinco endpoints REST, filtro competência, ID, ProfessorEntrada, ProfessorSaida, erros ErroResposta e códigos HTTP efetivos.
- Listagem retorna array, criação 201, atualização/consulta 200 e desativação 204 sem corpo.
- Segurança por cookie de sessão JSESSIONID; escrita administrativa com CSRF. Não há autenticação Bearer/JWT.
- Swagger UI e /v3/api-docs continuam protegidos pela configuração existente. Não foi aberto acesso anônimo nem desativado CSRF.
- Teste do documento realmente gerado e ampliação da cobertura de autenticação, perfis e CSRF.

## Parecer sobre leitura — decisão pendente dos revisores

A matriz de permissões permite aos perfis SOLICITANTE, RESPONSAVEL e ADMINISTRADOR consultar professores **ativos**, para obter dados necessários ao agendamento. Logo, restringir toda leitura ao administrador contrariaria o uso previsto. O código de listagem aplica ativo=true e filtro exato normalizado por competência.

Há duas ressalvas concretas: ProfessorSaida inclui e-mail para todos os leitores; consultar(id) usa findById e permite consultar inativos. A matriz não demonstra necessidade de e-mail no agendamento nem autoriza explicitamente consulta geral de inativos. Portanto, não se pode declarar o alcance atual integralmente aprovado apenas porque há autenticação.

Antes do merge, os revisores devem confirmar expressamente a finalidade do e-mail e os perfis que podem consultar inativos. Se não forem necessários, a correção mínima é uma projeção de leitura com dados necessários ao agendamento e bloqueio de inativos para os perfis sem permissão, preservando a edição administrativa. Nesse caso, ajustar o contrato, as respostas e testes de API e web correspondentes. A tabela web também mostra e-mail; corrigir apenas o JSON não resolve essa exposição.

Esta alteração documenta o comportamento existente e não muda unilateralmente a política. Não inclui matrícula, auditoria, validação operacional de professor, novos perfis ou redesenho da autenticação. Matrícula/auditoria já estão registradas como divergências de escopo na evidência original e exigem decisão explícita de encaminhamento, sem considerá-las implementadas.

## Migration e reversão

Na base atual, V3 é `V3__create_table_salas.sql`; professores estão em `V4__create_professores_competencias.sql`. A descrição original do PR e a evidência anterior ainda citam V3 para professores e precisam ser corrigidas para V4.

V4 cria tb_professores, tb_competencias e tb_professor_competencias, com restrições e índice; não contém INSERT de dados. Cadastros posteriores permanecem no banco se o código for revertido. Reverter Git não desfaz DDL, dados ou flyway_schema_history. Também não recolhe dados pessoais já consultados.

Não alterar migration já aplicada, executar clean, apagar histórico ou adicionar DROP automático para atender este thread. Antes da implantação, registrar backup/restauração e compatibilidade com a versão anterior; testar reversão em banco descartável com dados e validar o comportamento do Flyway da versão anterior (uma migration aplicada ausente pode impedir a inicialização). Uma eventual remoção exige planejamento específico e avaliação das referências e dados, fora desta correção documental.

## Validação e pendências de aceite

- `git diff --check`: passou.
- Compilação local tentada com `./mvnw -B --no-transfer-progress -DskipTests compile`: bloqueada antes de compilar por falha de DNS de repo.maven.apache.org ao resolver o parent Spring Boot 3.5.16. O ambiente também só dispõe de Java 17; o projeto exige Java 21. Não há resultado local de compilação ou testes novos aprovado.
- A execução anterior de 97 testes não cobre estas mudanças. O CI do commit-base tem execução 39700659491 concluída com sucesso, mas não valida o próximo commit.
- Executar em Java 21: `./mvnw -B --no-transfer-progress verify` (Windows: `.\mvnw.cmd -B verify`), com Docker/PostgreSQL 16 para não omitir integração. Conferir testes ignorados por disabledWithoutDocker.
- Os testes novos verificam publicação dos cinco endpoints, schemas, sessão, respostas, acesso anônimo negado, leitura dos três perfis e escritas administrativas com CSRF. Devem passar junto com os testes existentes de validação, conflito, não encontrado, persistência, filtros e MVC.
- Conferir Swagger UI após login e respostas JSON; validar que 204 não tem corpo e que os schemas não contêm credenciais reais.
- Após qualquer ajuste de autorização, adicionar testes de e-mail e inativos conforme a decisão humana, tanto na API quanto na interface web.
- Registrar link de CI verde no SHA final e evidência navegável exigida pela RTM; completar o fluxo manual de cadastro, edição, filtro e desativação ou teste ponta a ponta.
- Responder ao thread com os commits e evidência depois da validação; resolver somente o apontamento técnico atendido. As decisões humanas e aprovações exigidas continuam sendo condições de merge. Preservar um commit por arquivo e não usar squash.
