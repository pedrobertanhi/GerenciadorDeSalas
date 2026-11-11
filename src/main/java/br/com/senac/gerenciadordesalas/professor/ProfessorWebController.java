package br.com.senac.gerenciadordesalas.professor;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Telas de professores e competencias. ENT-03 (#185): mensagens de validacao em portugues,
 * estados vazio e de falha na lista, tela propria para incluir e remover competencias e
 * avisos de erro na inativacao, sempre preservando o que foi digitado.
 */
@Controller
@RequestMapping("/professores")
@PreAuthorize("isAuthenticated()")
public class ProfessorWebController {

    private static final Logger LOG = LoggerFactory.getLogger(ProfessorWebController.class);
    private static final String TELA_COMPETENCIAS = "professores/competencias";

    private final ProfessorService service;

    public ProfessorWebController(ProfessorService service) {
        this.service = service;
    }

    public record Form(
            @NotBlank(message = "Informe o nome.") @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres.") String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "Informe um e-mail válido.")
            @Size(max = 254, message = "O e-mail pode ter no máximo 254 caracteres.") String email,
            String competencias) {
        public Form() { this("", "", ""); }

        ProfessorDto.Entrada entrada() {
            if (competencias == null || competencias.isBlank()) {
                // Campo vazio quer dizer "sem competencias", nao uma competencia em branco.
                return new ProfessorDto.Entrada(nome, email, Set.of());
            }
            Set<String> itens = Arrays.stream(competencias == null ? new String[0] : competencias.split(","))
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (itens.isEmpty()) itens = Set.of();
            return new ProfessorDto.Entrada(nome, email, itens);
        }
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String competencia, Model model, Authentication authentication) {
        model.addAttribute("administrador", authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")));
        model.addAttribute("competencia", competencia == null ? "" : competencia);
        model.addAttribute("filtrando", competencia != null && !competencia.isBlank());
        List<ProfessorDto.Saida> professores;
        try {
            professores = service.listar(competencia);
        } catch (DataAccessException ex) {
            LOG.error("Falha ao listar professores", ex);
            model.addAttribute("falha", "Não foi possível carregar os professores agora. Tente novamente em instantes.");
            professores = List.of();
        }
        model.addAttribute("professores", professores);
        return "professores/lista";
    }

    @GetMapping("/novo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String novo(Model model) {
        model.addAttribute("form", new Form());
        model.addAttribute("titulo", "Cadastrar professor");
        model.addAttribute("acao", "/professores");
        return "professores/formulario";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String editar(@PathVariable Long id, Model model) {
        var professor = service.consultar(id);
        model.addAttribute("form", new Form(professor.nome(), professor.email(),
                String.join(", ", professor.competencias())));
        model.addAttribute("titulo", "Editar professor");
        model.addAttribute("acao", "/professores/" + id);
        return "professores/formulario";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String criar(@Valid @ModelAttribute("form") Form form, BindingResult erros,
                        Model model, RedirectAttributes redirect) {
        return salvar(form, erros, model, redirect, null);
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") Form form,
                            BindingResult erros, Model model, RedirectAttributes redirect) {
        return salvar(form, erros, model, redirect, id);
    }

    private String salvar(Form form, BindingResult erros, Model model,
                          RedirectAttributes redirect, Long id) {
        model.addAttribute("titulo", id == null ? "Cadastrar professor" : "Editar professor");
        model.addAttribute("acao", id == null ? "/professores" : "/professores/" + id);
        if (erros.hasErrors()) return "professores/formulario";
        var entrada = form.entrada();
        if (entrada.competencias().stream().anyMatch(String::isBlank)
                || entrada.competencias().stream().anyMatch(s -> s.length() > 100)) {
            erros.rejectValue("competencias", "invalidas", "Informe competências válidas de até 100 caracteres.");
            return "professores/formulario";
        }
        try {
            if (id == null) service.criar(entrada);
            else service.atualizar(id, entrada);
        } catch (ConflitoDeEstadoException ex) {
            erros.rejectValue("email", "duplicado", "Este e-mail já está cadastrado.");
            return "professores/formulario";
        }
        redirect.addFlashAttribute("sucesso", "Cadastro salvo com sucesso.");
        return "redirect:/professores";
    }

    @PostMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.desativar(id);
            redirect.addFlashAttribute("sucesso", "Professor desativado.");
        } catch (RecursoNaoEncontradoException ex) {
            redirect.addFlashAttribute("erro", "Professor não encontrado.");
        }
        return "redirect:/professores";
    }

    // ---------- competencias ----------

    @GetMapping("/{id}/competencias")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String competencias(@PathVariable Long id, Model model) {
        return telaDeCompetencias(id, "", model);
    }

    @PostMapping("/{id}/competencias")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String adicionarCompetencia(@PathVariable Long id, @RequestParam(defaultValue = "") String competencia,
                                       Model model, RedirectAttributes redirect) {
        try {
            service.adicionarCompetencia(id, competencia);
        } catch (IllegalArgumentException | ConflitoDeEstadoException ex) {
            model.addAttribute("erro", ex.getMessage());
            return telaDeCompetencias(id, competencia, model);
        }
        redirect.addFlashAttribute("sucesso", "Competência incluída.");
        return "redirect:/professores/" + id + "/competencias";
    }

    @PostMapping("/{id}/competencias/remover")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String removerCompetencia(@PathVariable Long id, @RequestParam(defaultValue = "") String competencia,
                                     RedirectAttributes redirect) {
        try {
            service.removerCompetencia(id, competencia);
            redirect.addFlashAttribute("sucesso", "Competência removida.");
        } catch (IllegalArgumentException | ConflitoDeEstadoException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/professores/" + id + "/competencias";
    }

    private String telaDeCompetencias(Long id, String digitado, Model model) {
        model.addAttribute("professor", service.consultar(id));
        model.addAttribute("competencia", digitado);
        return TELA_COMPETENCIAS;
    }
}
