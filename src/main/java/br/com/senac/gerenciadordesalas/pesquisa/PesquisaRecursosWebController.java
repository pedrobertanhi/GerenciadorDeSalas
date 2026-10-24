package br.com.senac.gerenciadordesalas.pesquisa;

import br.com.senac.gerenciadordesalas.material.MaterialDto;
import br.com.senac.gerenciadordesalas.material.MaterialService;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.ProfessorDto;
import br.com.senac.gerenciadordesalas.professor.ProfessorService;
import br.com.senac.gerenciadordesalas.salas.SalaFiltroDTO;
import br.com.senac.gerenciadordesalas.salas.SalaResponseDTO;
import br.com.senac.gerenciadordesalas.salas.SalaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tela de pesquisa combinada de recursos (RES-05, #19).
 * Salas: capacidade mínima e localização. Materiais: tipo e localização.
 * Professores: competência. Somente recursos ativos aparecem.
 * Filtros inválidos não executam a pesquisa e retornam 400 com mensagens na própria tela.
 */
@Controller
@RequestMapping("/recursos/pesquisa")
@PreAuthorize("isAuthenticated()")
public class PesquisaRecursosWebController {

    static final int TAMANHO_PAGINA = 10;
    private static final String VIEW = "recursos/pesquisa";

    public enum Recurso { SALA, MATERIAL, PROFESSOR }

    /** Linha exibida na tabela de resultados, comum aos três tipos de recurso. */
    public static final class Linha {
        private final String nome;
        private final String detalhes;
        private final String localizacao;

        public Linha(String nome, String detalhes, String localizacao) {
            this.nome = nome;
            this.detalhes = detalhes;
            this.localizacao = localizacao;
        }

        public String getNome() { return nome; }
        public String getDetalhes() { return detalhes; }
        public String getLocalizacao() { return localizacao; }
    }

    private final SalaService salas;
    private final MaterialService materiais;
    private final ProfessorService professores;

    public PesquisaRecursosWebController(SalaService salas, MaterialService materiais,
                                         ProfessorService professores) {
        this.salas = salas;
        this.materiais = materiais;
        this.professores = professores;
    }

    @GetMapping
    public String pesquisar(@RequestParam(required = false) String recurso,
                            @RequestParam(required = false) String capacidadeMinima,
                            @RequestParam(required = false) String localizacao,
                            @RequestParam(required = false) String tipo,
                            @RequestParam(required = false) String competencia,
                            @RequestParam(required = false) String pagina,
                            Model model, HttpServletResponse response) {
        List<String> erros = new ArrayList<>();
        Recurso recursoEscolhido = lerRecurso(recurso, erros);
        Integer capacidade = lerCapacidade(capacidadeMinima, erros);
        MaterialTipo tipoMaterial = lerTipo(tipo, erros);
        int numeroPagina = lerPagina(pagina, erros);
        String local = textoOuNulo(localizacao);
        String competenciaFiltro = textoOuNulo(competencia);

        model.addAttribute("recurso", recursoEscolhido.name());
        model.addAttribute("capacidadeMinima", valorOuVazio(capacidadeMinima));
        model.addAttribute("localizacao", valorOuVazio(localizacao));
        model.addAttribute("tipo", valorOuVazio(tipo));
        model.addAttribute("competencia", valorOuVazio(competencia));
        model.addAttribute("erros", erros);

        if (!erros.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("resultado", Page.<Linha>empty(PageRequest.of(0, TAMANHO_PAGINA)));
            return VIEW;
        }

        Page<Linha> resultado = switch (recursoEscolhido) {
            case SALA -> salas.pesquisar(new SalaFiltroDTO(capacidade, local),
                            PageRequest.of(numeroPagina, TAMANHO_PAGINA, Sort.by("nome")))
                    .map(PesquisaRecursosWebController::linhaDeSala);
            case MATERIAL -> paginar(materiais.listar(tipoMaterial, null).stream()
                    .filter(m -> local == null || contemIgnorandoCaixa(m.localizacao(), local))
                    .map(PesquisaRecursosWebController::linhaDeMaterial)
                    .toList(), numeroPagina);
            case PROFESSOR -> paginar(professores.listar(competenciaFiltro).stream()
                    .sorted(Comparator.comparing(ProfessorDto.Saida::nome, String.CASE_INSENSITIVE_ORDER))
                    .map(PesquisaRecursosWebController::linhaDeProfessor)
                    .toList(), numeroPagina);
        };
        model.addAttribute("resultado", resultado);
        return VIEW;
    }

    private static Recurso lerRecurso(String valor, List<String> erros) {
        String texto = textoOuNulo(valor);
        if (texto == null) {
            return Recurso.SALA;
        }
        try {
            return Recurso.valueOf(texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            erros.add("Tipo de recurso inválido. Use SALA, MATERIAL ou PROFESSOR.");
            return Recurso.SALA;
        }
    }

    private static Integer lerCapacidade(String valor, List<String> erros) {
        String texto = textoOuNulo(valor);
        if (texto == null) {
            return null;
        }
        try {
            int capacidade = Integer.parseInt(texto);
            if (capacidade >= 1) {
                return capacidade;
            }
        } catch (NumberFormatException ex) {
            // tratado abaixo com a mesma mensagem
        }
        erros.add("A capacidade mínima deve ser um número inteiro maior que zero.");
        return null;
    }

    private static MaterialTipo lerTipo(String valor, List<String> erros) {
        String texto = textoOuNulo(valor);
        if (texto == null) {
            return null;
        }
        try {
            return MaterialTipo.valueOf(texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            erros.add("Tipo de material inválido. Use MATERIAL ou EQUIPAMENTO.");
            return null;
        }
    }

    private static int lerPagina(String valor, List<String> erros) {
        String texto = textoOuNulo(valor);
        if (texto == null) {
            return 0;
        }
        try {
            int numero = Integer.parseInt(texto);
            if (numero >= 0) {
                return numero;
            }
        } catch (NumberFormatException ex) {
            // tratado abaixo com a mesma mensagem
        }
        erros.add("Página inválida.");
        return 0;
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String valorOuVazio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static boolean contemIgnorandoCaixa(String texto, String termo) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(termo.toLowerCase(Locale.ROOT));
    }

    private static Page<Linha> paginar(List<Linha> linhas, int numeroPagina) {
        PageRequest pagina = PageRequest.of(numeroPagina, TAMANHO_PAGINA);
        int inicio = (int) Math.min(pagina.getOffset(), linhas.size());
        int fim = Math.min(inicio + TAMANHO_PAGINA, linhas.size());
        return new PageImpl<>(linhas.subList(inicio, fim), pagina, linhas.size());
    }

    private static Linha linhaDeSala(SalaResponseDTO sala) {
        return new Linha(sala.nome(), "Capacidade: " + sala.capacidade() + " pessoas", sala.localizacao());
    }

    private static Linha linhaDeMaterial(MaterialDto.Saida material) {
        String tipo = material.tipo() == MaterialTipo.EQUIPAMENTO ? "Equipamento" : "Material";
        String detalhes = tipo + ", " + material.quantidadeTotal() + " unidade(s)"
                + (material.restrito() ? ", exige aprovação" : "");
        return new Linha(material.nome(), detalhes, material.localizacao());
    }

    private static Linha linhaDeProfessor(ProfessorDto.Saida professor) {
        String detalhes = professor.competencias().isEmpty()
                ? "Sem competências cadastradas"
                : "Competências: " + String.join(", ", professor.competencias());
        return new Linha(professor.nome(), detalhes, "Não se aplica");
    }
}