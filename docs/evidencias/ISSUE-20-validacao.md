# Evidência parcial — issue #20

## Escopo entregue neste PR
- Intervalo semiaberto [início, término) conforme ADR-003.
- Validação de início e término em ISO 8601 com deslocamento explícito de fuso horário e normalização para Instant.
- Testes unitários de intervalos inválidos, sobreposição, adjacência e deslocamento.
- Contrato inicial autenticado `GET /api/disponibilidade/consulta?inicio=...&termino=...`.
- Resposta HTTP 400 (`INTERVALO_INVALIDO`) para intervalos inválidos, extremos ou parâmetros ausentes.
- Horários de resposta convertidos para `America/Sao_Paulo` (ADR-003).
- Resposta HTTP 503 (`CONSULTA_INDISPONIVEL`) para intervalo válido até a integração das dependências; **não retorna lista fictícia de recursos disponíveis**.
- Testes unitários do contrato de resposta parcial.

## Bloqueios para concluir os critérios de aceite
Após o merge da issue #19 (RES-05), a branch da #20 foi atualizada com a main em 24/10/2026. A pesquisa combinada já está integrada. Permanecem pendentes os provedores de conflitos de reservas e manutenção das issues RSV-06 (#28), RSV-07 (#29), RSV-08 (#30) e MNT-02 (#22).

Sem esses provedores não é possível comprovar que recursos estão realmente livres. A resposta 503 é temporária, não corresponde à implementação final da issue.

## Verificação
O primeiro CI falhou por conflito de mapeamento com o controlador auxiliar de testes de segurança, que ocupa `GET /api/disponibilidade`. O contrato parcial foi ajustado para `/api/disponibilidade/consulta` sem alterar os testes legados.

O CI / Maven Verify passou no commit `bbdd8971e4bb29853916dc0b71d9f44a5f7752c8` (run 40141381791). Após a revisão do PR, foram adicionadas correções de `DateTimeException`, parâmetros ausentes, fuso horário e assertivas de valores esperados. **É necessária nova execução do CI para validar essas correções.**

## Próximos passos (fora do escopo deste PR parcial)
1. Integrar contratos persistentes de reservas e manutenções quando as dependências estiverem prontas.
2. Implementar consultas de conflitos para sala, professor e material, incluindo manutenção.
3. Substituir 503 pelo resultado real e criar testes de integração cobrindo disponível, reservado, manutenção e adjacência.
4. Somente encerrar #20 após validação de todos os critérios de aceite.
