package br.com.senac.gerenciadordesalas.professor;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_competencias")
public class Competencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100, unique = true)
    private String nome;
    protected Competencia() {}
    public Competencia(String nome) { this.nome = nome; }
    public Long getId() { return id; }
    public String getNome() { return nome; }
}