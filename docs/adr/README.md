# Registros de Decisão Arquitetural

Este diretório reúne as decisões técnicas que orientam o desenvolvimento do sistema de organização de recursos.

## Estados possíveis

- **Proposta:** decisão documentada e aguardando aprovação da equipe;
- **Aceita:** decisão aprovada pelos quatro integrantes;
- **Substituída:** decisão trocada por outro ADR, sem apagar o histórico.

## Estrutura obrigatória de um ADR

Todo novo ADR deverá conter:

- identificador e título;
- status da decisão;
- data;
- autores ou proponentes;
- Issue relacionada;
- contexto e problema analisado;
- alternativas consideradas;
- decisão adotada;
- consequências positivas e negativas;
- impactos de segurança, quando aplicável;
- forma de verificação futura.

Um ADR não poderá ser considerado completo se alguma seção obrigatória estiver ausente. Decisões substituídas permanecerão no histórico.

## Índice de decisões

| ADR | Decisão | Status |
|---|---|---|
| [ADR-001](0001-stack-tecnologica.md) | Stack tecnológica base | Aceita |
| [ADR-002](0002-interface-web-e-openapi.md) | Interface web e documentação OpenAPI | Aceita |
| [ADR-003](0003-tempo-e-sobreposicao.md) | Fuso horário e sobreposição de intervalos | Aceita |
| [ADR-004](0004-arquitetura-em-camadas.md) | Arquitetura em camadas | Aceita |
| [ADR-005](0005-autenticacao-e-autorizacao.md) | Autenticação e autorização | Aceita |
| [ADR-006](0006-controle-concorrente-de-reservas.md) | Controle concorrente de reservas | Aceita |
| [ADR-007](0007-estrategia-de-notificacoes.md) | Estratégia de notificações | Aceita |

## Validação de escopo do GOV-01

Os requisitos funcionais RF01 a RF14 foram comparados com a especificação fornecida pelo professor.

A revisão confirmou que:

- todos os 14 requisitos funcionais obrigatórios estão presentes no `docs/PRD.md`;
- os perfis Solicitante, Responsável e Administrador estão contemplados;
- as regras críticas de tempo, concorrência, manutenção, estados e auditoria estão documentadas;
- nenhuma funcionalidade fora do escopo inicial foi adicionada;
- as decisões técnicas necessárias antes da implementação foram registradas em ADRs.

A aprovação definitiva será evidenciada pelas revisões dos quatro integrantes no Pull Request relacionado à Issue #1.

## Decisões pendentes

As seguintes definições dependem das próximas etapas:

- versão corretiva específica do Spring Boot 3.x;
- versão do `springdoc-openapi` compatível com o Spring Boot escolhido;
- endereço e credenciais do provedor externo de notificações, caso a implementação externa seja ativada;
- aprovação dos ADRs pelos quatro integrantes.

Versões e credenciais somente serão definidas durante a configuração correspondente. Segredos não serão registrados no repositório.

## Regra de manutenção

Cada ADR deverá ser criado e alterado em commit próprio. Uma decisão substituída permanecerá no histórico e indicará o ADR que passou a substituí-la.
