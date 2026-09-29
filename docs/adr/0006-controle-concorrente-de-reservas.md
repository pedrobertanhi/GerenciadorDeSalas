# ADR-006: Controle concorrente de reservas

- **Status:** Aceita
- **Data:** 26/08/2026
- **Issue relacionada:** #1

## Contexto

Duas solicitações simultâneas para o mesmo recurso não podem gerar duas reservas aceitas. Uma simples consulta de disponibilidade antes da gravação não é suficiente, pois ambas as requisições podem consultar o recurso antes que qualquer uma conclua a transação.

A possibilidade de dupla reserva é um limitador de nota do projeto.

## Decisão

A criação e alteração de reservas ocorrerão dentro de uma transação do Spring.

Antes de confirmar a reserva, o sistema realizará bloqueio pessimista de escrita nos registros dos recursos envolvidos, utilizando os mecanismos do Spring Data JPA e PostgreSQL.

Dentro da mesma transação, o sistema deverá:

1. bloquear os recursos envolvidos;
2. verificar novamente as reservas existentes;
3. verificar a agenda do professor;
4. verificar materiais e quantidades disponíveis;
5. verificar bloqueios de manutenção;
6. validar a necessidade de aprovação;
7. persistir a nova reserva somente se todas as validações forem aprovadas.

Quando houver vários recursos, os bloqueios serão adquiridos em ordem determinística por tipo e identificador para reduzir o risco de deadlock.

## Resultado de solicitações simultâneas

A primeira transação aceita será persistida. A segunda aguardará a liberação do recurso e repetirá a verificação de disponibilidade.

Caso encontre conflito, a segunda solicitação será recusada com resposta controlada `409 Conflict`. Ao final, deverá existir somente uma reserva aceita para o intervalo disputado.

## Restrições

- A verificação de disponibilidade não poderá ocorrer somente fora da transação;
- Controllers não controlarão diretamente transações ou bloqueios;
- Exceções do banco não serão retornadas diretamente ao usuário;
- O bloqueio deverá abranger sala, professor e materiais envolvidos;
- Reservas e manutenções serão verificadas novamente após a aquisição dos bloqueios.

## Consequências

### Positivas

- Proteção contra condições de corrida;
- Comportamento consistente com múltiplas instâncias da aplicação;
- Uso dos recursos transacionais do PostgreSQL;
- Regra verificável por teste automatizado.

### Negativas

- Solicitações concorrentes poderão aguardar a liberação dos recursos;
- Ordem incorreta de bloqueios poderá causar deadlocks;
- Transações deverão permanecer curtas;
- Falhas de bloqueio precisarão de tratamento seguro.

## Alternativas consideradas

- Verificação somente na aplicação: descartada por não proteger solicitações simultâneas;
- Bloco `synchronized` em Java: descartado porque não protege múltiplas instâncias da aplicação;
- Somente restrição de unicidade: insuficiente para detectar sobreposição de intervalos.

## Verificação futura

Será criado teste de integração com PostgreSQL em Testcontainers executando duas solicitações concorrentes para o mesmo recurso e intervalo.

O teste deverá comprovar:

- exatamente uma solicitação aceita;
- exatamente uma solicitação recusada;
- somente uma reserva correspondente no banco;
- ausência de reserva parcial;
- resposta controlada para a solicitação conflitante.

Mocks não serão utilizados para comprovar esta regra crítica.
