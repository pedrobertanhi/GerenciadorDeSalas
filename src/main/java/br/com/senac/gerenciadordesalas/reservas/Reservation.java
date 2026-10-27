package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.usuario.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Agregado central de reserva (RSV-01, #23).
 * Relaciona solicitante, sala, professor opcional e materiais com quantidade.
 * O status inicial e as transicoes sao controlados pelo dominio.
 */
@Entity
@Table(name = "tb_reservas")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private User solicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    @Column(name = "inicio", nullable = false)
    private Instant inicio;

    @Column(name = "termino", nullable = false)
    private Instant termino;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusReserva status;

    @OneToMany(mappedBy = "reserva",
            cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReservationMaterial> materiais = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /** Construtor exigido pelo JPA. Nao use fora de testes de infraestrutura. */
    protected Reservation() {
    }

    public Reservation(User solicitante, Sala sala, Professor professor,
                       Instant inicio, Instant termino) {
        this.solicitante = Objects.requireNonNull(solicitante, "solicitante e obrigatorio");
        this.sala = Objects.requireNonNull(sala, "sala e obrigatoria");
        this.professor = professor;
        definirPeriodo(inicio, termino);
        this.status = StatusReserva.CRIADA;
    }

   public void definirPeriodo(Instant inicio, Instant termino) {
        if (this.status != null && this.status != StatusReserva.CRIADA) {
            throw new IllegalStateException(
                    "Periodo so pode ser alterado enquanto a reserva estiver CRIADA");
        }
        if (inicio == null || termino == null || !termino.isAfter(inicio)) {
            throw new IllegalArgumentException("O termino deve ser posterior ao inicio");
        }
        this.inicio = inicio;
        this.termino = termino;
    }

    /** Transiciona para {@code novo} se a regra permitir; caso contrario lanca. */
    public void transicionarPara(StatusReserva novo) {
        if (!this.status.podeTransicionarPara(novo)) {
            throw new IllegalStateException(
                    "Transicao invalida: " + this.status + " -> " + novo);
        }
        this.status = novo;
    }

        public ReservationMaterial adicionarMaterial(Material material, int quantidade) {
        Objects.requireNonNull(material, "material e obrigatorio");
        boolean jaExiste = this.materiais.stream()
                .anyMatch(m -> material.equals(m.getMaterial()));
        if (jaExiste) {
            throw new IllegalStateException(
                    "Material ja esta vinculado a esta reserva; remova antes de readicionar");
        }
        ReservationMaterial item = new ReservationMaterial(this, material, quantidade);
        this.materiais.add(item);
        marcarAtualizado();
        return item;
    }

    public void removerMaterial(ReservationMaterial item) {
        if (this.materiais.remove(item)) {
            item.desassociar();
            marcarAtualizado();
        }
    }

     /**
     * Marca a reserva como suja para o JPA, forcando o {@link #preUpdate()} a
     * atualizar {@code atualizadoEm} mesmo quando a unica mudanca na transacao
     * for na colecao de materiais.
     */
    private void marcarAtualizado() {
        this.atualizadoEm = Instant.now();
    }

    @PrePersist
    void prePersist() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        this.atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Long getVersao() { return versao; }
    public User getSolicitante() { return solicitante; }
    public Sala getSala() { return sala; }
    public Professor getProfessor() { return professor; }
    public Instant getInicio() { return inicio; }
    public Instant getTermino() { return termino; }
    public StatusReserva getStatus() { return status; }
    public List<ReservationMaterial> getMateriais() { return Collections.unmodifiableList(materiais); }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void setProfessor(Professor professor) { this.professor = professor; }
}