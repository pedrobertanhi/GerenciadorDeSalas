# Evidência de validação — Issue #22 (MNT-02)

## Entrega parcial preparada

- consulta central `MaintenanceBlockService.recursoEmManutencao(...)` para que os casos de uso
  consultem a indisponibilidade de uma sala, professor ou material no intervalo solicitado;
- consulta persistente que considera bloqueios ativos e concluídos até o instante de encerramento;
- intervalos semiabertos: um horário que termina exatamente no início do bloqueio, ou começa
  exatamente no fim, continua disponível;
- teste parametrizado para sobreposição total, parcial e adjacência.

## Política para reservas já existentes

O cadastro de manutenção **não cancela nem altera automaticamente** reservas já persistidas.
Elas permanecem como histórico e a decisão operacional deve ser registrada quando os módulos de
reserva e auditoria estiverem disponíveis. A regra desta Issue é impedir novas reservas que
sobreponham uma manutenção ativa, sem apagar ou modificar registros anteriores.

## Integração pendente por dependência

No estado atual do repositório não existe o caso de uso da RSV-02 (criação de reserva), nem a
consulta real de disponibilidade da RES-06. Portanto ainda não há endpoint de reserva em que a
regra possa ser chamada, nem autorização de reserva para testar. Quando a RSV-02 estiver na
`main`, ela deve chamar `recursoEmManutencao(...)` antes de persistir e devolver HTTP 409 com a
mensagem: `Recurso indisponível por manutenção no período informado.`

Esta PR não declara a Issue #22 concluída: ela entrega a base verificável sem antecipar ou
duplicar o escopo da RSV-02/RES-06.
