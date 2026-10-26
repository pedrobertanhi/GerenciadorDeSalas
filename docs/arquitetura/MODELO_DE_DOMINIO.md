# Modelo de Domínio

## Objetivo

Documentar as entidades principais do Gerenciador de Salas de Aula, seus atributos, relacionamentos e regras básicas.

Este modelo atende aos requisitos RF-01 a RF-08 do PRD e serve como referência para a implementação futura.

## User

Representa uma pessoa autorizada a acessar o sistema.

### Atributos

- `id`: identificador único;
- `name`: nome completo;
- `email`: endereço único utilizado na autenticação;
- `passwordHash`: hash da senha; a senha em texto puro nunca deve ser armazenada;
- `profile`: perfil de acesso;
- `active`: indica se o acesso está habilitado;
- `createdAt`: instante do cadastro;
- `updatedAt`: instante da última alteração.

### Perfis

- `SOLICITANTE`;
- `RESPONSAVEL`;
- `ADMINISTRADOR`.

### Regras

- o e-mail deve ser único;
- usuários inativos não podem acessar o sistema;
- toda ação deve respeitar o perfil e a propriedade do recurso;
- a autorização utiliza negação por padrão.

## Professor

Representa os dados acadêmicos de um professor.

### Atributos

- `id`: identificador único;
- `name`: nome completo;
- `registration`: matrícula institucional única;
- `email`: endereço institucional;
- `competencies`: competências ou áreas de atuação;
- `active`: indica se o professor está disponível para novas reservas.

### Regras

- a matrícula deve ser única;
- um professor inativo não pode ser incluído em novas reservas;
- o professor não pode participar de reservas com horários sobrepostos;
- um professor pode estar associado a um usuário quando também acessar o sistema.

## Room

Representa uma sala que pode ser pesquisada e reservada.

### Atributos

- `id`: identificador único;
- `name`: nome ou código único;
- `capacity`: quantidade máxima de pessoas;
- `location`: localização da sala;
- `restricted`: indica se exige aprovação;
- `active`: indica se pode receber novas reservas.

### Regras

- o nome ou código deve ser único;
- a capacidade deve ser maior que zero;
- uma sala inativa não pode receber novas reservas;
- uma sala não pode possuir reservas sobrepostas;
- uma sala em manutenção não pode ser reservada no período bloqueado.

## Material

Representa um material ou equipamento acadêmico disponível para reserva.

### Atributos

- `id`: identificador único;
- `name`: nome do material;
- `description`: descrição;
- `type`: classificação `MATERIAL` ou `EQUIPAMENTO`;
- `assetTag`: patrimônio opcional do equipamento;
- `totalQuantity`: quantidade total cadastrada;
- `location`: local de armazenamento;
- `restricted`: indica se exige aprovação;
- `active`: indica se está disponível para novas reservas.

### Regras

- a quantidade total não pode ser negativa;
- o patrimônio deve ser único quando informado;
- a quantidade disponível é calculada considerando reservas, retiradas, devoluções e manutenções;
- uma reserva não pode solicitar quantidade maior que a disponível no período;
- materiais inativos não podem ser incluídos em novas reservas.

## MaintenanceBlock

Representa a indisponibilidade temporária de um recurso (sala, professor ou material), registrada por um administrador.

### Atributos

- `id`: identificador único;
- `resourceType`: tipo do recurso bloqueado, `SALA`, `PROFESSOR` ou `MATERIAL`;
- `resourceId`: identificador do recurso, sem chave estrangeira, porque o mesmo campo aponta para três tabelas;
- `reason`: motivo da manutenção, obrigatório, com até 500 caracteres;
- `startAt`: início do bloqueio;
- `endAt`: término previsto do bloqueio;
- `status`: estado `ATIVO`, `CONCLUIDO` ou `CANCELADO`;
- `closedAt`: instante em que o bloqueio foi concluído ou cancelado;
- `createdAt`: instante do cadastro;
- `updatedAt`: instante da última alteração.

### Regras

- o término deve ser posterior ao início, e na criação ou alteração ainda deve estar no futuro;
- um bloqueio pode começar no passado, quando a manutenção já está em andamento;
- o recurso deve existir no momento da criação e não pode ser trocado depois;
- somente administradores criam, alteram, concluem ou cancelam bloqueios, e todos os perfis podem consultá-los;
- alterar e cancelar exigem o estado `ATIVO`; concluir exige o estado `ATIVO` e que o bloqueio já tenha começado;
- um bloqueio concluído libera o recurso a partir de `closedAt`, e um cancelado deixa de impedir o uso;
- bloqueios nunca são excluídos: concluídos e cancelados permanecem como histórico;
- bloqueios sobrepostos no mesmo recurso são aceitos, e a política para esse caso aguarda decisão do grupo;
- períodos que apenas se tocam não se sobrepõem;
- datas são persistidas como instantes e interpretadas no fuso `America/Sao_Paulo`.

## Reservation

Representa a solicitação de utilização de sala, professor e materiais em determinado intervalo.

### Atributos

- `id`: identificador único;
- `requester`: usuário que criou a reserva;
- `room`: sala reservada;
- `professor`: professor relacionado;
- `startAt`: início do período;
- `endAt`: término do período;
- `status`: estado atual;
- `justification`: justificativa da solicitação;
- `createdAt`: instante da criação;
- `updatedAt`: instante da última alteração.

### Estados previstos

- `SOLICITADA`;
- `APROVADA`;
- `REJEITADA`;
- `EM_USO`;
- `CONCLUIDA`;
- `CANCELADA`;
- `NAO_COMPARECEU`.

As transições permitidas e proibidas serão detalhadas no documento específico da máquina de estados.

### Regras

- o término deve ser posterior ao início;
- intervalos adjacentes são permitidos;
- sobreposição real de sala ou professor é proibida;
- a quantidade reservada de cada material não pode superar a disponibilidade;
- recursos restritos exigem aprovação do perfil Responsável;
- o Solicitante somente altera ou cancela as próprias reservas;
- uma reserva iniciada não pode ser apagada;
- toda mudança de estado deve gerar auditoria;
- solicitações concorrentes para o mesmo recurso podem produzir somente uma aceitação.

## Relacionamentos

| Origem | Relação | Destino | Cardinalidade |
|---|---|---|---|
| User | cria | Reservation | um para muitos |
| User | pode representar | Professor | zero ou um para zero ou um |
| Professor | participa | Reservation | um para muitos |
| Room | é utilizada em | Reservation | um para muitos |
| Reservation | solicita | Material | muitos para muitos |
| Reservation | possui quantidades por material | ReservationMaterial | um para muitos |
| Material | aparece em | ReservationMaterial | um para muitos |

A relação entre `Reservation` e `Material` utiliza a entidade associativa `ReservationMaterial`, pois cada material possui uma quantidade específica dentro da reserva.

## Restrições gerais

- registros com histórico não devem ser excluídos fisicamente;
- datas são persistidas como instantes e interpretadas no fuso `America/Sao_Paulo`;
- dados inválidos ou duplicados devem ser recusados;
- decisões de autorização não devem ficar somente na interface ou nos controllers;
- entidades de manutenção, movimentação, auditoria e notificação serão documentadas separadamente.
