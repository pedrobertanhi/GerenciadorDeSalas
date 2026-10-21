# Relatório de Testes de Segurança (SEC-04)

## Resumo da Execução
Os testes automatizados de segurança foram implementados e executados com sucesso, garantindo a proteção das rotas e o tratamento de falhas.

## Cenários Cobertos
1. **Autenticação (Login/Logout)**
   - Login com sucesso (Geração de sessão/token)
   - Falha de login (Credenciais inválidas)
   - Tentativa de acesso por utilizador inativo (Bloqueado)
   - Invalidação de sessão no Logout

2. **Autorização (RBAC - Perfis)**
   - `SOLICITANTE`: Acesso permitido a reservas próprias; negado a rotas de administração e reservas de terceiros.
   - `RESPONSAVEL`: Acesso permitido à aprovação de reservas; negado à criação de administradores.
   - `ADMINISTRADOR`: Acesso permitido a rotas administrativas; validação de bloqueio em endpoints operacionais exclusivos.
   - Utilizadores não autenticados: Recebem erro `401 Unauthorized` em todas as rotas protegidas.

3. **Tratamento Seguro de Falhas**
   - Entradas malformadas retornam `400 Bad Request`.
   - Nenhuma stack trace, nome de classe Java ou detalhes de infraestrutura são expostos no corpo da resposta.

## Critérios de Aceite Validados
- [x] Falha de autorização bloqueia pipeline (garantido pela integração com Maven/JUnit no CI).
- [x] Todos os perfis têm cenários positivos e negativos testados.
- [x] Nenhum teste depende de credencial real (utilizado `@WithMockUser` e Mock do `UserDetailsService`).