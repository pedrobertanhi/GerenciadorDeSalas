# Evidência de Testes: CRUD de Salas (RES-01)

## Resumo
A funcionalidade de gestão de salas foi implementada e testada com sucesso, garantindo que os requisitos de negócio e segurança são cumpridos.

## Critérios de Aceite Validados
- **CRUD funciona somente para administrador:** Testes com `@WithMockUser` garantem que o perfil `SOLICITANTE` recebe erro 403 (Forbidden) ao tentar acessar os endpoints. O acesso é exclusivo para `ADMINISTRADOR`.
- **Capacidade positiva e localização obrigatória:** O `SalaRequestDTO` utiliza anotações `@Positive`, `@NotBlank` e `@NotNull`. Testes de integração validaram que o envio de capacidade negativa ou vazia devolve erro 400 (Bad Request).
- **Inativação preserva histórico:** O endpoint de inativação altera o campo booleano `ativa` para `false` no banco de dados, em vez de deletar o registro.

## Cobertura de Testes
- Criação de sala com sucesso (Retorno 201 Created).
- Recusa de criação com capacidade inválida (Retorno 400 Bad Request).
- Bloqueio de acesso para perfis não autorizados (Retorno 403 Forbidden).
- Inativação lógica com preservação de dados (Retorno 204 No Content).