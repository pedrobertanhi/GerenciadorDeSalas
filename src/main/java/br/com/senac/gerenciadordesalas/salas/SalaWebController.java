package br.com.senac.gerenciadordesalas.salas;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas de salas (RES-01, #15): listagem paginada, detalhe, cadastro, edição e inativação.
 * Qualquer perfil autenticado consulta; somente o administrador escreve. A inativação não
 * apaga a sala, e o histórico de reservas é preservado.
 */
@Controller
@RequestMapping("/salas")
@PreAuthorize("isAuthenticated()")
public class SalaWebController {

    static final int TAMANHO_PAGINA = 10;
    static final String AVISO_NAO_ENCONTRADA = "nao-encontrada";

    private final SalaService service;

    public SalaWebController(SalaService service) {
        this.service = service;
    }

    public record Form(
            @NotBlank(message = "O nome da sala é obrigatório.")
            @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
            String nome,
            @NotBlank(message = "A capacidade é obrigatória.")
            @Pattern(regexp = "^$|^\\s*[0-9]+\\s*$",
                    message = "A capacidade deve ser um número inteiro positivo.")
            String capacidade,
            @NotBlank(message = "A localização é obrigatória.")
            @Size(max = 255, message = "A localização deve ter no máximo 255 caracteres.")
            String localizacao) {

        public Form() {
            this("", "", "");
        }

        Sala paraSala() {
            return new Sala(nome.trim(), Integer.valueOf(capacidade.trim()), localizacao.trim());
        }
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String page,
                         @RequestParam(required = false) String capacidadeMinima,
                         @RequestParam(required = false) String localizacao,
                         @RequestParam(required = false) String aviso,
                         Model model, Authentication authentication) {
        Integer minima = null;
        String digitada = capacidadeMinima == null ? "" : capacidadeMinima.trim();
        if (!digitada.isEmpty()) {
            minima = converterCapacidadeMinima(digitada);
            if (minima == null) {
                model.addAttribute("erroFiltro",
                        "A capacidade mínima deve ser um número inteiro maior que zero.");
                digitada = "";
            }
               }
        String local = localizacao == null ? "" : localizacao.trim();
        int numeroDaPagina = converterPagina(page);
        boolean paginaInvalida = preenchido(page) && numeroDaPagina < 0;
        var pagina = service.pesquisar(new SalaFiltroDTO(minima, local),
                PageRequest.of(Math.max(numeroDaPagina, 0), TAMANHO_PAGINA, Sort.by("nome")));
        model.addAttribute("administrador", administrador(authentication));
        model.addAttribute("resultado", pagina);
        model.addAttribute("capacidadeMinima", digitada);
        model.addAttribute("localizacao", local);
        if (paginaInvalida) {
            model.addAttribute("aviso", "A página informada não é válida. Exibindo a primeira página.");
        }
        if (AVISO_NAO_ENCONTRADA.equals(aviso)) {
            model.addAttribute("aviso", "Sala não encontrada ou já inativada.");
        }
        return "salas/lista";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, Authentication authentication) {
        model.addAttribute("sala", new SalaResponseDTO(service.buscarPorId(id)));
        model.addAttribute("administrador", administrador(authentication));
        return "salas/detalhe";
    }

    @GetMapping("/novo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String novo(Model model) {
        model.addAttribute("form", new Form());
        prepararFormulario(model, null);
        return "salas/formulario";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String editar(@PathVariable Long id, Model model) {
        Sala sala = service.buscarPorId(id);
        model.addAttribute("form", new Form(sala.getNome(), String.valueOf(sala.getCapacidade()),
                sala.getLocalizacao()));
        prepararFormulario(model, id);
        return "salas/formulario";
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

    @GetMapping("/{id}/inativar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String confirmarInativacao(@PathVariable Long id, Model model) {
        model.addAttribute("sala", new SalaResponseDTO(service.buscarPorId(id)));
        return "salas/confirmar-inativacao";
    }

    @PostMapping("/{id}/inativar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String inativar(@PathVariable Long id, RedirectAttributes redirect) {
        service.buscarPorId(id);
        service.inativar(id);
        redirect.addFlashAttribute("sucesso", "Sala inativada. O histórico de reservas foi preservado.");
        return "redirect:/salas";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String salaNaoEncontrada() {
        return "redirect:/salas?aviso=" + AVISO_NAO_ENCONTRADA;
    }

    private String salvar(Form form, BindingResult erros, Model model,
                           RedirectAttributes redirect, Long id) {
        prepararFormulario(model, id);
        if (erros.hasErrors() || !capacidadeNoIntervalo(form.capacidade(), erros)) {
            return "salas/formulario";
        }
        try {
            if (id == null) {
                service.criar(form.paraSala());
            } else {
                service.buscarPorId(id);
                service.atualizar(id, form.paraSala());
            }
        } catch (EntityExistsException ex) {
            erros.rejectValue("nome", "duplicado", "Já existe uma sala com este nome.");
            return "salas/formulario";
        }
        redirect.addFlashAttribute("sucesso",
                id == null ? "Sala cadastrada com sucesso." : "Sala atualizada com sucesso.");
        return "redirect:/salas";
    }

    private void prepararFormulario(Model model, Long id) {
        model.addAttribute("titulo", id == null ? "Cadastrar sala" : "Editar sala");
        model.addAttribute("acao", id == null ? "/salas" : "/salas/" + id);
    }

    private static boolean capacidadeNoIntervalo(String texto, BindingResult erros) {
        try {
            if (Integer.parseInt(texto.trim()) >= 1) {
                return true;
            }
            erros.rejectValue("capacidade", "positiva", "A capacidade deve ser um número inteiro positivo.");
        } catch (NumberFormatException ex) {
            erros.rejectValue("capacidade", "grande",
                    "A capacidade informada é grande demais. O máximo é " + Integer.MAX_VALUE + ".");
        }
        return false;
    }

    /** Número da página pedido, 0 quando ausente, ou -1 quando o valor não é um inteiro maior ou igual a zero. */
    private static int converterPagina(String texto) {
        if (!preenchido(texto)) {
            return 0;
        }
        try {
            int valor = Integer.parseInt(texto.trim());
            return valor >= 0 ? valor : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static boolean preenchido(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static Integer converterCapacidadeMinima(String texto) {
        try {
            int valor = Integer.parseInt(texto);
            return valor >= 1 ? valor : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean administrador(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }
}