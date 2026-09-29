# ADR-003: Fuso horário e sobreposição de intervalos

- **Status:** Aceita
- **Data:** 26/08/2026
- **Issue relacionada:** #1

## Contexto

As reservas envolvem salas, professores e materiais em intervalos de tempo. O sistema precisa impedir sobreposições, aceitar horários adjacentes e manter um tratamento consistente de datas entre interface, backend, banco de dados e testes.

## Decisão

O sistema utilizará:

- `America/Sao_Paulo` como fuso horário de negócio;
- `ZoneId` para representar o fuso, evitando um deslocamento UTC fixo;
- `Instant` para comparação e persistência;
- ISO 8601 com deslocamento UTC nas entradas e respostas da API; valores de API sem deslocamento serão rejeitados;
- intervalos no formato semiaberto `[início, término)`.

O término deverá ser obrigatoriamente posterior ao início.

## Regra de sobreposição

Dois intervalos apresentam sobreposição quando `inicioExistente < novoTermino` e `terminoExistente > novoInicio`.

A mesma regra será aplicada à agenda da sala, do professor, dos materiais e aos períodos de manutenção.

## Horários adjacentes

Horários adjacentes serão permitidos. Uma reserva poderá começar exatamente no instante em que outra terminar.

Exemplo permitido:

- Reserva A: 08:00 até 10:00;
- Reserva B: 10:00 até 12:00.

Exemplo recusado:

- Reserva A: 08:00 até 10:00;
- Reserva B: 09:59 até 12:00.

## Conversão de horários

Os horários informados pela interface serão interpretados no fuso `America/Sao_Paulo` e convertidos para `Instant` antes da persistência e comparação.

As respostas serão convertidas para o fuso de negócio na apresentação. A aplicação não utilizará o fuso padrão do servidor como fonte implícita.

## Consequências

### Positivas

- Comparações consistentes entre ambientes;
- Horários adjacentes não geram falso conflito;
- Menor risco de erros causados pelo fuso do servidor;
- Regra única para reservas e manutenções.

### Negativas

- A conversão entre horário local e `Instant` deverá ser explícita;
- Valores locais recebidos pela interface serão interpretados no fuso de negócio, enquanto valores de API sem deslocamento serão rejeitados;
- Casos de limite exigirão testes específicos.

## Verificação futura

Serão criados testes unitários parametrizados para:

- término igual ao início de outra reserva;
- sobreposição parcial e total;
- intervalos idênticos;
- término anterior ou igual ao início;
- conversão entre `America/Sao_Paulo` e UTC;
- conflito na sala, professor, material e manutenção.
