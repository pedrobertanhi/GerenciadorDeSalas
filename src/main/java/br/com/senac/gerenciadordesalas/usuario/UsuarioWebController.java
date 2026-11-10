package br.com.senac.gerenciadordesalas.usuario;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import jakarta.validation.Valid;
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

@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioWebController {

    private static final String LISTA = "redirect:/usuarios";
    private static final String FORMULARIO = "usuarios/formulario";
    private static final int TAMANHO_PAGINA = 10;
    private static final Sort ORDEM = Sort.by("name", "id");

    private final UsuarioAdminService service;

    public UsuarioWebController(UsuarioAdminService service) {
        this.service = service;
    }

    @ModelAttribute("perfis")
    public Role[] perfis() {
        return Role.values();
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "0") int page, Model model) {
        int numero = Math.max(0, Math.min(page, Integer.MAX_VALUE / TAMANHO_PAGINA));
        var resultado = service.paginar(PageRequest.of(numero, TAMANHO_PAGINA, ORDEM));
        if (numero > 0 && resultado.isEmpty()) {
            return LISTA;
        }
        model.addAttribute("resultado", resultado);
        return "usuarios/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new UsuarioDto.Entrada("", "", "", null));
        return FORMULARIO;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") UsuarioDto.Entrada form, BindingResult erros,
                        Authentication autenticacao, RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            return FORMULARIO;
        }
        try {
            service.criar(form, autenticacao.getName());
        } catch (ConflitoDeEstadoException ex) {
            erros.rejectValue("email", "email.duplicado", "Este e-mail já está cadastrado.");
            return FORMULARIO;
        }
        redirect.addFlashAttribute("sucesso", "Usuário cadastrado.");
        return LISTA;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        var usuario = service.consultar(id);
        model.addAttribute("id", id);
        model.addAttribute("form", new UsuarioDto.Atualizacao(usuario.nome(), usuario.perfil()));
        return FORMULARIO;
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") UsuarioDto.Atualizacao form,
                            BindingResult erros, Model model, Authentication autenticacao,
                            RedirectAttributes redirect) {
        model.addAttribute("id", id);
        if (erros.hasErrors()) {
            return FORMULARIO;
        }
        try {
            service.atualizar(id, form, autenticacao.getName());
        } catch (ConflitoDeEstadoException ex) {
            model.addAttribute("aviso", "Mantenha pelo menos um administrador ativo.");
            return FORMULARIO;
        }
        redirect.addFlashAttribute("sucesso", "Usuário atualizado.");
        return LISTA;
    }

    @GetMapping("/{id}/situacao")
    public String confirmarSituacao(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", service.consultar(id));
        return "usuarios/confirmar-situacao";
    }

    @PostMapping("/{id}/situacao")
    public String mudarSituacao(@PathVariable Long id, @RequestParam boolean ativar,
                                Authentication autenticacao, RedirectAttributes redirect) {
        try {
            if (ativar) {
                service.ativar(id, autenticacao.getName());
            } else {
                service.inativar(id, autenticacao.getName());
            }
            redirect.addFlashAttribute("sucesso", ativar ? "Usuário ativado." : "Usuário inativado.");
        } catch (ConflitoDeEstadoException ex) {
            redirect.addFlashAttribute("aviso",
                    "A situação não pôde ser alterada. Confira o estado da conta e mantenha um administrador ativo.");
        }
        return LISTA;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public String usuarioNaoEncontrado(RedirectAttributes redirect) {
        redirect.addFlashAttribute("aviso", "Usuário não encontrado.");
        return LISTA;
    }
}
