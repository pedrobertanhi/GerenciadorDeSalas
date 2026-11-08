package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
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
 * Agregado central de reserva (RSV-01, #23; finalidade em RSV-02, #24).
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

    @Column(name = "finalidade", nullable = false, length = 500)
    private String finalidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusReserva status;

    @Enumerated(EnumType.STRING)
    @Column(name = "aprovacao_recurso", nullable = false, length = 20)
    private SituacaoPendencia aprovacaoRecurso = SituacaoPendencia.NAO_EXIGIDA;

    @Enumerated(EnumType.STRING)
    @Column(name = "validacao_docente", nullable = false, length = 20)
    private SituacaoPendencia validacaoDocente = SituacaoPendencia.NAO_EXIGIDA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_decidido_por_id")
    private User recursoDecididoPor;

    @Column(name = "recurso_decidido_em")
    private Instant recursoDecididoEm;

    @Column(name = "recurso_motivo_rejeicao", length = 500)
    private String recursoMotivoRejeicao;

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
                       Instant inicio, Instant termino, String finalidade) {
        this.solicitante = Objects.requireNonNull(solicitante, "solicitante e obrigatorio");
        this.sala = Objects.requireNonNull(sala, "sala e obrigatoria");
        this.professor = professor;
        definirPeriodo(inicio, termino);
        definirFinalidade(finalidade);
        this.status = StatusReserva.CRIADA;
    }

    public void definirPeriodo(Instant inicio, Instant termino) {
        if (this.status != null && !permiteAlteracao()) {
            throw new IllegalStateException(
                    "Periodo so pode ser alterado enquanto a reserva estiver CRIADA ou APROVADA");
        }
        if (inicio == null || termino == null || !termino.isAfter(inicio)) {
            throw new IllegalArgumentException("O termino deve ser posterior ao inicio");
        }
        this.inicio = inicio;
        this.termino = termino;
    }

    public void definirFinalidade(String finalidade) {
        if (finalidade == null || finalidade.isBlank()) {
            throw new IllegalArgumentException("A finalidade e obrigatoria");
        }
        this.finalidade = finalidade.trim();
    }

    /**
     * Altera os dados editaveis antes do uso: em CRIADA (aguardando decisao) ou
     * APROVADA, conforme docs/arquitetura/ESTADOS_DA_RESERVA.md. Depois de alterar,
     * a necessidade de aprovacao deve ser reavaliada.
     */
    public void alterar(Sala sala, Professor professor, Instant inicio, Instant termino, String finalidade) {
        if (!permiteAlteracao()) {
            throw new ConflitoDeEstadoException(
                    "Reserva so pode ser alterada enquanto estiver CRIADA ou APROVADA");
        }
        this.sala = Objects.requireNonNull(sala, "sala e obrigatoria");
        this.professor = professor;
        definirPeriodo(inicio, termino);
        definirFinalidade(finalidade);
        marcarAtualizado();
    }

    /**
     * Cancela sem remover o agregado ou seus materiais, preservando historico.
     * Reservas em uso ou ja encerradas nao podem ser canceladas.
     */
    public void cancelar() {
        if (this.status == StatusReserva.EM_ANDAMENTO || this.status.isFinal()) {
            throw new ConflitoDeEstadoException(
                    "Reserva em andamento ou finalizada nao pode ser cancelada");
        }
        this.status = StatusReserva.CANCELADA;
        marcarAtualizado();
    }

    /** {@code true} enquanto a reserva ainda pode ser alterada pelo solicitante. */
    public boolean permiteAlteracao() {
        return this.status == StatusReserva.CRIADA || this.status == StatusReserva.APROVADA;
    }

    /** Transiciona para {@code novo} se a regra permitir; caso contrario lanca. */
    public void transicionarPara(StatusReserva novo) {
        if (!this.status.podeTransicionarPara(novo)) {
            throw new ConflitoDeEstadoException(
                    "Transicao invalida: " + this.status + " -> " + novo);
        }
        this.status = novo;
    }

    /**
     * Regra de aprovacao (APR-01, #34). Chamada pelo sistema depois de criar ou
     * alterar a reserva, quando sala, professor e materiais ja estao definidos:
     * <ul>
     *   <li>material restrito exige aprovacao de recurso pelo Responsavel;</li>
     *   <li>professor exige validacao docente pelo Responsavel (APR-03);</li>
     *   <li>sem nenhuma pendencia, a reserva e aprovada automaticamente;</li>
     *   <li>com pendencia, fica CRIADA (o SOLICITADA do modelo) na fila.</li>
     * </ul>
     * Uma reserva APROVADA que passa a exigir decisao volta para CRIADA, como
     * preve a transicao APROVADA para SOLICITADA feita pelo sistema.
     */
    public void reavaliarPendencias() {
        if (!permiteAlteracao()) {
            throw new ConflitoDeEstadoException(
                    "Pendencias so podem ser reavaliadas em reserva CRIADA ou APROVADA");
        }
        boolean temMaterialRestrito = this.materiais.stream()
                .anyMatch(item -> item.getMaterial().isRestrito());
        this.aprovacaoRecurso = temMaterialRestrito
                ? SituacaoPendencia.PENDENTE : SituacaoPendencia.NAO_EXIGIDA;
        // Toda alteracao exige nova decisao: a aprovacao anterior nao vale para o novo pedido.
        this.recursoDecididoPor = null;
        this.recursoDecididoEm = null;
        this.recursoMotivoRejeicao = null;
        this.validacaoDocente = this.professor != null
                ? SituacaoPendencia.PENDENTE : SituacaoPendencia.NAO_EXIGIDA;
        this.status = possuiPendencia() ? StatusReserva.CRIADA : StatusReserva.APROVADA;
        marcarAtualizado();
    }

    /**
     * Garante que a aprovacao de recurso restrito ainda aguarda decisao (APR-02, #34).
     * Impede decidir duas vezes, decidir reserva que nao exige aprovacao ou que ja
     * saiu de CRIADA.
     */
    public void exigirAprovacaoRecursoPendente() {
        if (this.status != StatusReserva.CRIADA || this.aprovacaoRecurso != SituacaoPendencia.PENDENTE) {
            throw new ConflitoDeEstadoException("A aprovacao de recurso desta reserva nao esta pendente");
        }
    }

    /**
     * Responsavel aprova o recurso restrito. A reserva so passa para APROVADA quando
     * nao resta nenhuma outra pendencia (por exemplo, a validacao docente).
     */
    public void aprovarRecurso(User responsavel, Instant decididaEm) {
        exigirAprovacaoRecursoPendente();
        registrarDecisaoRecurso(SituacaoPendencia.APROVADA, responsavel, decididaEm, null);
        if (!possuiPendencia()) {
            transicionarPara(StatusReserva.APROVADA);
        }
        marcarAtualizado();
    }

    /** Responsavel rejeita o recurso restrito com motivo; a reserva passa para REJEITADA. */
    public void rejeitarRecurso(User responsavel, Instant decididaEm, String motivo) {
        exigirAprovacaoRecursoPendente();
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("O motivo da rejeicao e obrigatorio");
        }
        String motivoLimpo = motivo.trim();
        if (motivoLimpo.length() > 500) {
            throw new IllegalArgumentException("O motivo da rejeicao deve ter no maximo 500 caracteres");
        }
        registrarDecisaoRecurso(SituacaoPendencia.REJEITADA, responsavel, decididaEm, motivoLimpo);
        transicionarPara(StatusReserva.REJEITADA);
        marcarAtualizado();
    }

    private void registrarDecisaoRecurso(SituacaoPendencia decisao, User responsavel,
                                         Instant decididaEm, String motivo) {
        this.recursoDecididoPor = Objects.requireNonNull(responsavel, "responsavel e obrigatorio");
        this.recursoDecididoEm = Objects.requireNonNull(decididaEm, "instante da decisao e obrigatorio");
        this.aprovacaoRecurso = decisao;
        this.recursoMotivoRejeicao = motivo;
    }

    /** {@code true} se alguma decisao do Responsavel ainda impede a aprovacao. */
    public boolean possuiPendencia() {
        return aprovacaoRecurso.bloqueiaAprovacao() || validacaoDocente.bloqueiaAprovacao();
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
    public String getFinalidade() { return finalidade; }
    public StatusReserva getStatus() { return status; }
    public SituacaoPendencia getAprovacaoRecurso() { return aprovacaoRecurso; }
    public SituacaoPendencia getValidacaoDocente() { return validacaoDocente; }
    public User getRecursoDecididoPor() { return recursoDecididoPor; }
    public Instant getRecursoDecididoEm() { return recursoDecididoEm; }
    public String getRecursoMotivoRejeicao() { return recursoMotivoRejeicao; }
    public List<ReservationMaterial> getMateriais() { return Collections.unmodifiableList(materiais); }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void setProfessor(Professor professor) { this.professor = professor; }
}
