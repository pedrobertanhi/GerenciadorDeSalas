# Documentação Arquitetural e de Dados (`[GOV-03]`)

## 1. Diagrama de Contexto (C4 - Nível 1)

- **Versão:** 1.1
- **Data:** 22/09/2026

```mermaid
C4Context
    title Diagrama de Contexto - Sistema de Organização de Recursos

    Person(solicitante, "Solicitante", "Consulta disponibilidade e cria, altera ou cancela reservas.")
    Person(responsavel, "Responsável", "Aprova recursos restritos e acompanha retiradas e devoluções.")
    Person(administrador, "Administrador", "Gerencia usuários, salas, professores, materiais e manutenções.")

    System(sistema, "Gerenciador de Salas", "Aplicação web e API para organizar recursos sem conflitos.")
    SystemDb(banco, "PostgreSQL", "Persiste usuários, recursos, reservas, movimentações e auditoria.")
    System_Ext(notificacao, "Provedor de notificações", "Envia avisos de reservas e aprovações quando habilitado.")

    Rel(solicitante, sistema, "Consulta e solicita reservas", "HTTPS")
    Rel(responsavel, sistema, "Aprova e registra movimentações", "HTTPS")
    Rel(administrador, sistema, "Administra cadastros e manutenções", "HTTPS")
    Rel(sistema, banco, "Persiste e consulta dados", "JDBC")
    Rel(sistema, notificacao, "Envia notificações", "HTTPS")
```

## 2. Diagrama de Componentes (C4 - Nível 3)

- **Versão:** 1.1
- **Data:** 22/09/2026

```mermaid
C4Component
    title Diagrama de Componentes - Backend Spring Boot

    Container_Boundary(backend, "Aplicação Backend") {
        Component(security, "Segurança", "Spring Security", "Autentica usuários e fornece identidade e perfis.")
        Component(web, "Apresentação", "Spring MVC e Thymeleaf", "Recebe requisições web e da API.")
        Component(application, "Aplicação", "Serviços de aplicação", "Autoriza ações e orquestra casos de uso e transações.")
        Component(domain, "Domínio", "Java", "Aplica estados, disponibilidade, sobreposição e regras de negócio.")
        Component(persistence, "Persistência", "Spring Data JPA", "Consulta e persiste entidades.")
        Component(audit, "Auditoria", "Serviço de auditoria", "Registra ações sensíveis e transições.")
        Component(notification, "Notificações", "Porta de integração", "Isola o provedor externo e a implementação simulada.")
        Component(flyway, "Migrações", "Flyway", "Versiona o esquema do banco.")
    }

    ContainerDb(postgres, "PostgreSQL", "Banco de dados", "Armazena os dados do sistema.")
    System_Ext(provider, "Provedor externo", "Serviço de notificações configurável.")

    Rel(web, security, "Autentica requisições")
    Rel(web, application, "Encaminha requisições autenticadas")
    Rel(application, security, "Consulta identidade e perfis")
    Rel(application, domain, "Aplica regras")
    Rel(application, persistence, "Consulta e salva")
    Rel(application, audit, "Registra eventos")
    Rel(application, notification, "Solicita avisos")
    Rel(persistence, postgres, "JDBC")
    Rel(audit, postgres, "JDBC")
    Rel(flyway, postgres, "Aplica migrações")
    Rel(notification, provider, "HTTPS")
```

## 3. Diagrama Entidade-Relacionamento

- **Versão:** 1.1
- **Data:** 22/09/2026

```mermaid
erDiagram
    USER ||--o{ RESERVATION : cria
    USER o|--o| PROFESSOR : representa
    PROFESSOR o|--o{ RESERVATION : participa
    ROOM ||--o{ RESERVATION : recebe

    RESERVATION ||--o{ RESERVATION_MATERIAL : possui
    MATERIAL ||--o{ RESERVATION_MATERIAL : compoe

    ROOM o|--o{ MAINTENANCE : recebe
    MATERIAL o|--o{ MAINTENANCE : recebe
    %% Constraint: MAINTENANCE references exactly one resource: room_id XOR material_id.
    USER ||--o{ MAINTENANCE : cria

    RESERVATION ||--o{ MATERIAL_MOVEMENT : gera
    MATERIAL ||--o{ MATERIAL_MOVEMENT : movimenta
    USER ||--o{ MATERIAL_MOVEMENT : registra

    USER o|--o{ AUDIT_EVENT : atua
    RESERVATION o|--o{ NOTIFICATION : origina
    USER ||--o{ NOTIFICATION : recebe

    USER {
        BIGINT id PK
        VARCHAR name
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR profile "SOLICITANTE, RESPONSAVEL, ADMINISTRADOR"
        BOOLEAN active
    }

    PROFESSOR {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR registration UK
        VARCHAR competencies
    }

    ROOM {
        BIGINT id PK
        VARCHAR name UK
        INTEGER capacity
        VARCHAR location
        BOOLEAN restricted
        BOOLEAN active
    }

    MATERIAL {
        BIGINT id PK
        VARCHAR name UK
        INTEGER total_quantity
        BOOLEAN restricted
        BOOLEAN active
    }

    RESERVATION {
        BIGINT id PK
        BIGINT requester_id FK
        BIGINT professor_id FK "nullable"
        BIGINT room_id FK
        TIMESTAMP starts_at
        TIMESTAMP ends_at
        VARCHAR status
    }

    RESERVATION_MATERIAL {
        BIGINT id PK
        BIGINT reservation_id FK
        BIGINT material_id FK
        INTEGER quantity
    }

    MAINTENANCE {
        BIGINT id PK
        BIGINT room_id FK "nullable; XOR material_id"
        BIGINT material_id FK "nullable; XOR room_id"
        BIGINT created_by FK
        TIMESTAMP starts_at
        TIMESTAMP ends_at
        VARCHAR reason
    }

    MATERIAL_MOVEMENT {
        BIGINT id PK
        BIGINT reservation_id FK
        BIGINT material_id FK
        BIGINT registered_by FK
        INTEGER quantity
        VARCHAR type "RETIRADA, DEVOLUCAO"
        TIMESTAMP occurred_at
    }

    AUDIT_EVENT {
        BIGINT id PK
        BIGINT actor_id FK
        VARCHAR entity_type
        BIGINT entity_id
        VARCHAR action
        TIMESTAMP occurred_at
    }

    NOTIFICATION {
        BIGINT id PK
        BIGINT reservation_id FK
        BIGINT recipient_id FK
        VARCHAR type
        VARCHAR status
        TIMESTAMP created_at
    }
```

## Regras complementares

- `ReservationMaterial` registra a quantidade de cada material solicitado;
- cada manutenção afeta exatamente uma sala ou um material, nunca ambos;
- intervalos são semiabertos: `[início, término)`;
- sala, professor e material não podem possuir sobreposição incompatível;
- movimentações, auditorias, notificações e reservas históricas não são apagadas fisicamente;
- permissões e transições seguem a matriz e a máquina de estados documentadas.
