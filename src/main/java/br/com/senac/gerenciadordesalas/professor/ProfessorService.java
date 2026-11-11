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

    /**
     * Inclui uma competencia no professor ativo (ENT-03, #185). O nome e normalizado como no cadastro
     * completo; competencia repetida e recusada com conflito, para a tela avisar em vez de ignorar.
     */
    @Transactional
    public ProfessorDto.Saida adicionarCompetencia(Long id, String nome) {
        Professor professor = obterAtivo(id);
        String competencia = normalizarCompetencia(nome);
        if (professor.getCompetencias().stream().anyMatch(c -> c.getNome().equals(competencia))) {
            throw new ConflitoDeEstadoException("O professor já possui a competência \"" + competencia + "\".");
        }
        professor.getCompetencias().add(competencias.findByNome(competencia)
                .orElseGet(() -> competencias.saveAndFlush(new Competencia(competencia))));
        return salvar(professor);
    }

    /** Remove uma competencia do professor ativo (ENT-03, #185). A competencia continua cadastrada para os outros. */
    @Transactional
    public ProfessorDto.Saida removerCompetencia(Long id, String nome) {
        Professor professor = obterAtivo(id);
        String competencia = normalizarCompetencia(nome);
        if (!professor.getCompetencias().removeIf(c -> c.getNome().equals(competencia))) {
            throw new ConflitoDeEstadoException("O professor não possui a competência \"" + competencia + "\".");
        }
        return salvar(professor);
    }

    private static String normalizarCompetencia(String nome) {
        String competencia = nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
        if (competencia.isEmpty()) {
            throw new IllegalArgumentException("Informe a competência.");
        }
        if (competencia.length() > 100) {
            throw new IllegalArgumentException("A competência pode ter no máximo 100 caracteres.");
        }
        return competencia;
    }

    private Professor obterAtivo(Long id) {
        Professor professor = obter(id);
        if (!professor.isAtivo()) {
            throw new ConflitoDeEstadoException("Professor inativo não pode ter as competências alteradas.");
        }
        return professor;
    }
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

    /**
     * Nomes de professores por identificador, inclusive os inativados. O histórico precisa continuar
     * mostrando quem era o professor depois que ele deixa de ser listado.
     */
    @Transactional(readOnly = true)
    public Map<Long, String> nomesPorId(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return professores.findAllById(ids).stream()
                .collect(Collectors.toMap(Professor::getId, Professor::getNome));
    }
}