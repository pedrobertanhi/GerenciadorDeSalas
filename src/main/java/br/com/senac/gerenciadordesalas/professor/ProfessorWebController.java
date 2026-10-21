package br.com.senac.gerenciadordesalas.professor;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/professores")
@PreAuthorize("isAuthenticated()")
public class ProfessorWebController {
    private final ProfessorService service;

    public ProfessorWebController(ProfessorService service) {
        this.service = service;
    }

    public record Form(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            String competencias) {
        public Form() { this("", "", ""); }

        ProfessorDto.Entrada entrada() {
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
        model.addAttribute("professores", service.listar(competencia));
        model.addAttribute("competencia", competencia == null ? "" : competencia);
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
        service.desativar(id);
        redirect.addFlashAttribute("sucesso", "Professor desativado.");
        return "redirect:/professores";
    }
}
