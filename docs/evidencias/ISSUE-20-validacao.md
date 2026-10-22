# Evidência parcial — issue #20

## Implementado
- Intervalo semiaberto [início, término) conforme ADR-003.
- Validação de início e término, ISO 8601 com offset e normalização para Instant.
- Testes unitários para intervalos inválidos, sobreposição, adjacência e fuso.

## Bloqueios para concluir os critérios de aceite
Na main consultada em 22/10/2026 existem entidades de sala, professor e material, mas não há modelos/repositórios de reserva nem manutenção. A issue #20 depende de RES-05, RSV-06, RSV-07, RSV-08 e MNT-02. A issue #19 (RES-05) está aberta.

Não é seguro retornar recursos como disponíveis sem verificar reservas e manutenção. Portanto ainda não foi criado endpoint que afirme disponibilidade.

## Verificação
Os testes foram adicionados, mas **não foram executados neste ambiente**. CI deve validar os commits.

## Próximos passos
1. Integrar ou disponibilizar contratos persistentes de reservas e manutenções.
2. Implementar consultas de conflitos para sala, professor e material, incluindo manutenção.
3. Criar endpoint autenticado e testes de integração cobrindo os critérios de aceite.
4. Revisar CI e só então marcar PR como pronto para aprovação.
