package br.com.senac.gerenciadordesalas.professor;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfessorService {
    private final ProfessorRepository professores;
    private final CompetenciaRepository competencias;
    public ProfessorService(ProfessorRepository professores, CompetenciaRepository competencias) {
        this.professores = professores;
        this.competencias = competencias;
    }
    private String normalizarEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String normalizarNome(String nome) { return nome.trim(); }
    private Set<Competencia> resolverCompetencias(Set<String> nomes) {
        Set<String> normalizados = nomes.stream().map(String::trim)
            .filter(s -> !s.isBlank()).map(s -> s.toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(TreeSet::new));
        Set<Competencia> resultado = new HashSet<>();
        for (String nome : normalizados) {
            resultado.add(competencias.findByNome(nome).orElseGet(() -> competencias.saveAndFlush(new Competencia(nome))));
        }
        return resultado;
    }
    private ProfessorDto.Saida salvar(Professor professor) {
        try { return ProfessorDto.Saida.de(professores.saveAndFlush(professor)); }
        catch (DataIntegrityViolationException e) { throw new ConflitoDeEstadoException("Cadastro duplicado"); }
    }
    @Transactional
    public ProfessorDto.Saida criar(ProfessorDto.Entrada entrada) {
        String email = normalizarEmail(entrada.email());
        if (professores.existsByEmail(email)) throw new ConflitoDeEstadoException("Email ja cadastrado");
        return salvar(new Professor(normalizarNome(entrada.nome()), email, resolverCompetencias(entrada.competencias())));
    }
    @Transactional
    public ProfessorDto.Saida atualizar(Long id, ProfessorDto.Entrada entrada) {
        Professor p = obter(id);
        String email = normalizarEmail(entrada.email());
        if (professores.existsByEmailAndIdNot(email, id)) throw new ConflitoDeEstadoException("Email ja cadastrado");
        p.atualizar(normalizarNome(entrada.nome()), email, resolverCompetencias(entrada.competencias()));
        return salvar(p);
    }
    @Transactional
    public void desativar(Long id) { obter(id).desativar(); }
    @Transactional(readOnly = true)
    public ProfessorDto.Saida consultar(Long id) { return ProfessorDto.Saida.de(obter(id)); }
    @Transactional(readOnly = true)
    public List<ProfessorDto.Saida> listar(String competencia) {
        String filtro = competencia == null || competencia.isBlank() ? null : competencia.trim().toLowerCase(Locale.ROOT);
        return professores.buscarAtivos(filtro).stream().map(ProfessorDto.Saida::de).toList();
    }
    private Professor obter(Long id) {
        return professores.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado"));
    }
}