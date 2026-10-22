package br.com.senac.gerenciadordesalas.professor;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_professores")
public class Professor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String nome;
    @Column(nullable = false, length = 254, unique = true)
    private String email;
    @Column(nullable = false)
    private boolean ativo = true;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tb_professor_competencias",
        joinColumns = @JoinColumn(name = "professor_id"),
        inverseJoinColumns = @JoinColumn(name = "competencia_id"))
    private Set<Competencia> competencias = new HashSet<>();
    protected Professor() {}
    public Professor(String nome, String email, Set<Competencia> competencias) {
        atualizar(nome, email, competencias);
    }
    public void atualizar(String nome, String email, Set<Competencia> competencias) {
        this.nome = nome;
        this.email = email;
        this.competencias = new HashSet<>(competencias);
    }
    public void desativar() { ativo = false; }
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public boolean isAtivo() { return ativo; }
    public Set<Competencia> getCompetencias() { return competencias; }
}