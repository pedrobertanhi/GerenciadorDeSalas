package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.material.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.util.Objects;

/**
 * Linha de material reservado (RSV-01, #23).
 * Ligacao fraca entre {@link Reservation} e {@link Material}, carregando a quantidade pedida.
 * Mesmo material nao pode aparecer duas vezes na mesma reserva (unique).
 */
@Entity
@Table(name = "tb_reservas_materiais",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reservas_materiais_reserva_material",
                columnNames = {"reserva_id", "material_id"}))
public class ReservationMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reservation reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    /** Construtor exigido pelo JPA. Nao use fora de testes de infraestrutura. */
    protected ReservationMaterial() {
    }

    public ReservationMaterial(Reservation reserva, Material material, int quantidade) {
        this.reserva = Objects.requireNonNull(reserva, "reserva e obrigatoria");
        this.material = Objects.requireNonNull(material, "material e obrigatorio");
        definirQuantidade(quantidade);
    }

    public void definirQuantidade(int quantidade) {
        if (quantidade < 1) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
        this.quantidade = quantidade;
    }

    /** Usado pelo agregado ao remover este item. */
    void desassociar() {
        this.reserva = null;
    }

    public Long getId() { return id; }
    public Long getVersao() { return versao; }
    public Reservation getReserva() { return reserva; }
    public Material getMaterial() { return material; }
    public int getQuantidade() { return quantidade; }
}