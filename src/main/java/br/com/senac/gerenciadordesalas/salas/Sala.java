package br.com.senac.gerenciadordesalas.salas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "salas")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(nullable = false)
    private Integer capacidade;

    @Column(nullable = false, length = 255)
    private String localizacao;

    @Column(nullable = false)
    private Boolean ativa = true;

    // Construtor vazio exigido pelo JPA/Hibernate
    public Sala() {}

    public Sala(String nome, Integer capacidade, String localizacao) {
        this.nome = nome;
        this.capacidade = capacidade;
        this.localizacao = localizacao;
        this.ativa = true;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Integer getCapacidade() { return capacidade; }
    public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public Boolean getAtiva() { return ativa; }
    public void setAtiva(Boolean ativa) { this.ativa = ativa; }
}