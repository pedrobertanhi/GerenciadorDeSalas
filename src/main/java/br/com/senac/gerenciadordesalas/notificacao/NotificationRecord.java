package br.com.senac.gerenciadordesalas.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registro técnico de entrega; não armazena corpo, token ou resposta externa.
 */
@Entity
@Table(name = "tb_registros_notificacao")
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento", nullable = false, length = 80)
    private String event;

    @Column(name = "entidade", nullable = false, length = 80)
    private String entity;

    @Column(name = "entidade_id", nullable = false)
    private Long entityId;

    @Column(name = "destino", nullable = false, length = 254)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "tentativas", nullable = false)
    private int attempts;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "concluido_em")
    private Instant completedAt;

    @Column(name = "codigo_resposta")
    private Integer responseCode;

    @Column(name = "erro_codigo", length = 80)
    private String errorCode;

    protected NotificationRecord() {
    }

    public NotificationRecord(String event, String entity, Long entityId, String destination,
                              Instant createdAt) {
        this.event = event;
        this.entity = entity;
        this.entityId = entityId;
        this.destination = destination;
        this.createdAt = createdAt;
        this.status = NotificationStatus.PENDENTE;
        this.attempts = 1;
    }

    public void marcarEnviada(int codigoResposta, Instant concluidaEm) {
        this.status = NotificationStatus.ENVIADA;
        this.responseCode = codigoResposta;
        this.completedAt = concluidaEm;
        this.errorCode = null;
    }

    public void marcarFalhaPermanente(String codigoErro, Instant concluidaEm) {
        this.status = NotificationStatus.FALHA_PERMANENTE;
        this.errorCode = codigoErro;
        this.completedAt = concluidaEm;
    }

    public void marcarFalhaTransitoria(String codigoErro, Instant concluidaEm) {
        this.status = NotificationStatus.FALHA_TRANSITORIA;
        this.errorCode = codigoErro;
        this.completedAt = concluidaEm;
    }

    public Long getId() { return id; }
    public String getEvent() { return event; }
    public String getEntity() { return entity; }
    public Long getEntityId() { return entityId; }
    public String getDestination() { return destination; }
    public NotificationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Integer getResponseCode() { return responseCode; }
    public String getErrorCode() { return errorCode; }
}
