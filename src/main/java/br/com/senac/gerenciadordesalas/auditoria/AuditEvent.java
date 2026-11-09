package br.com.senac.gerenciadordesalas.auditoria;

import br.com.senac.gerenciadordesalas.usuario.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Evento append-only. Não há setters nem operações de atualização/exclusão
 * expostas pela camada de persistência.
 */
@Entity
@Table(name = "tb_eventos_auditoria")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entidade", nullable = false, length = 80)
    private String entity;

    @Column(name = "entidade_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, length = 80)
    private TipoAcaoAuditoria action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ator_id")
    private User actor;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "estado_anterior", length = 80, updatable = false)
    private String previousState;

    @Column(name = "estado_novo", length = 80, updatable = false)
    private String newState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalhes", columnDefinition = "jsonb")
    private Map<String, Object> details = new LinkedHashMap<>();

    protected AuditEvent() {
    }

    public AuditEvent(String entity, Long entityId, TipoAcaoAuditoria action, User actor,
                      Instant occurredAt, String previousState, String newState,
                      Map<String, Object> details) {
        this.entity = entity;
        this.entityId = entityId;
        this.action = action;
        this.actor = actor;
        this.occurredAt = occurredAt;
        this.previousState = previousState;
        this.newState = newState;
        this.details = details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details);
    }

    public Long getId() { return id; }
    public String getEntity() { return entity; }
    public Long getEntityId() { return entityId; }
    public TipoAcaoAuditoria getAction() { return action; }
    public User getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public Map<String, Object> getDetails() { return Map.copyOf(details); }
}
