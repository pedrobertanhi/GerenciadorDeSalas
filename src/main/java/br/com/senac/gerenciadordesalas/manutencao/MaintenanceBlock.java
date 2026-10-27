package br.com.senac.gerenciadordesalas.manutencao;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Indisponibilidade temporária de um recurso (sala, professor ou material).
 * O bloqueio nunca é excluído: ao ser concluído ou cancelado ele permanece como histórico.
 */
@Entity
@Table(name = "tb_bloqueios_manutencao")
public class MaintenanceBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurso_tipo", nullable = false, length = 20)
    private TipoRecurso recursoTipo;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusBloqueio status = StatusBloqueio.ATIVO;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected MaintenanceBlock() {
    }

    public MaintenanceBlock(TipoRecurso recursoTipo, Long recursoId, String motivo,
                            Instant inicio, Instant fim, Instant agora) {
        validarPeriodo(inicio, fim);
        this.recursoTipo = recursoTipo;
        this.recursoId = recursoId;
        this.motivo = motivo;
        this.inicio = inicio;
        this.fim = fim;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public void atualizar(String motivo, Instant inicio, Instant fim, Instant agora) {
        exigirAtivo("O bloqueio nao permite alteracao no estado atual");
        validarPeriodo(inicio, fim);
        this.motivo = motivo;
        this.inicio = inicio;
        this.fim = fim;
        this.atualizadoEm = agora;
    }

    public void cancelar(Instant agora) {
        exigirAtivo("Somente bloqueios ativos podem ser cancelados");
        encerrar(StatusBloqueio.CANCELADO, agora);
    }

    public void concluir(Instant agora) {
        exigirAtivo("Somente bloqueios ativos podem ser concluidos");
        if (agora.isBefore(inicio)) {
            throw new ConflitoDeEstadoException("O bloqueio ainda nao comecou");
        }
        encerrar(StatusBloqueio.CONCLUIDO, agora);
    }

    /**
     * O bloqueio impede o uso do recurso no intervalo [de, ate)? Intervalos que apenas se
     * tocam não se sobrepõem. Bloqueio cancelado não impede; concluído libera o recurso a
     * partir do instante da conclusão.
     */
    public boolean bloqueiaPeriodo(Instant de, Instant ate) {
        if (status == StatusBloqueio.CANCELADO) {
            return false;
        }
        return inicio.isBefore(ate) && fimEfetivo().isAfter(de);
    }

    public Instant fimEfetivo() {
        if (status == StatusBloqueio.CONCLUIDO && encerradoEm.isBefore(fim)) {
            return encerradoEm;
        }
        return fim;
    }

    private void encerrar(StatusBloqueio novoStatus, Instant agora) {
        this.status = novoStatus;
        this.encerradoEm = agora;
        this.atualizadoEm = agora;
    }

    private void exigirAtivo(String mensagem) {
        if (status != StatusBloqueio.ATIVO) {
            throw new ConflitoDeEstadoException(mensagem);
        }
    }

    private static void validarPeriodo(Instant inicio, Instant fim) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new IllegalArgumentException("O fim deve ser posterior ao inicio");
        }
    }

    public Long getId() {
        return id;
    }

    public TipoRecurso getRecursoTipo() {
        return recursoTipo;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public StatusBloqueio getStatus() {
        return status;
    }

    public Instant getEncerradoEm() {
        return encerradoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}