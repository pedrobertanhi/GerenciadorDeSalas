# ADR-007: Estratégia de notificações

- **Status:** Aceita
- **Data:** 26/08/2026
- **Issue relacionada:** #1

## Contexto

O sistema deve notificar eventos relevantes de reservas por meio de simulação ou integração externa. Falhas na notificação não poderão desfazer reservas válidas nem expor dados sensíveis.

O projeto também exige WireMock para testar integrações externas de forma isolada.

## Decisão

A aplicação utilizará uma interface própria de envio de notificações, separada das regras de negócio.

Serão disponibilizadas duas implementações:

- notificação simulada para desenvolvimento e demonstração;
- integração HTTP configurável para representar um provedor externo.

Os eventos notificáveis incluirão:

- solicitação criada;
- reserva aprovada ou rejeitada;
- reserva alterada ou cancelada;
- retirada e devolução de material;
- falha controlada no envio.

## Comportamento em falhas

A falha da notificação não cancelará uma mudança de estado válida da reserva.

Cada tentativa deverá registrar:

- tipo do evento;
- instante da tentativa;
- destinatário identificado de forma segura;
- resultado do envio;
- quantidade de tentativas;
- mensagem de erro sanitizada.

Falhas externas serão tratadas de forma controlada, sem retornar detalhes internos ou credenciais ao usuário.

## Segurança e robustez

- URL e credenciais serão fornecidas por configuração;
- Segredos não serão incluídos no repositório;
- O cliente HTTP terá limites de conexão e leitura;
- Respostas externas serão validadas;
- Logs não armazenarão tokens ou conteúdo sensível;
- Repetições serão limitadas para evitar chamadas infinitas;
- Eventos terão identificador para reduzir notificações duplicadas.

## Consequências

### Positivas

- Regras de reserva independentes do provedor;
- Integração testável com WireMock;
- Falhas externas não corrompem a reserva;
- Possibilidade de substituir a simulação futuramente.

### Negativas

- Tentativas e falhas precisarão ser armazenadas;
- A integração exigirá configuração e tratamento de indisponibilidade;
- A prevenção de notificações duplicadas exigirá identificadores estáveis.

## Alternativas consideradas

- Envio direto dentro do serviço de reservas: descartado por acoplar a regra de negócio ao provedor;
- Ignorar falhas externas: descartado por impedir rastreabilidade;
- Utilizar um serviço externo real durante os testes: descartado por gerar testes instáveis.

## Verificação futura

WireMock simulará:

- resposta de sucesso;
- erro HTTP;
- resposta inválida;
- atraso e timeout;
- repetição controlada;
- indisponibilidade do provedor.

Os testes deverão comprovar que falhas de notificação não desfazem reservas válidas.
