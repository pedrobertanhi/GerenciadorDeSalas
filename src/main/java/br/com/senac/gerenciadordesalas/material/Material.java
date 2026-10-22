package br.com.senac.gerenciadordesalas.material;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tb_materiais")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialTipo tipo;

    @Column(length = 100, unique = true)
    private String patrimonio;

    @Column(name = "quantidade_total", nullable = false)
    private int quantidadeTotal;

    @Column(nullable = false, length = 150)
    private String localizacao;

    @Column(nullable = false)
    private boolean restrito;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Material() {
    }

    public Material(String nome, String descricao, MaterialTipo tipo, String patrimonio,
                    int quantidadeTotal, String localizacao, boolean restrito) {
        atualizar(nome, descricao, tipo, patrimonio, quantidadeTotal, localizacao, restrito);
    }

    public void atualizar(String nome, String descricao, MaterialTipo tipo, String patrimonio,
                          int quantidadeTotal, String localizacao, boolean restrito) {
        if (quantidadeTotal < 0) {
            throw new IllegalArgumentException("A quantidade total nao pode ser negativa");
        }
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.patrimonio = patrimonio;
        this.quantidadeTotal = quantidadeTotal;
        this.localizacao = localizacao;
        this.restrito = restrito;
    }

    public void desativar() {
        ativo = false;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public MaterialTipo getTipo() {
        return tipo;
    }

    public String getPatrimonio() {
        return patrimonio;
    }

    public int getQuantidadeTotal() {
        return quantidadeTotal;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public boolean isRestrito() {
        return restrito;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
