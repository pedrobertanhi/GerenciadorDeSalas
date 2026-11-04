package br.com.senac.gerenciadordesalas.manutencao;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.material.MaterialService;
import br.com.senac.gerenciadordesalas.professor.ProfessorService;
import br.com.senac.gerenciadordesalas.reservas.ReservationService;
import br.com.senac.gerenciadordesalas.salas.SalaService;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Telas de manutenção (MNT/UI-08, #15): agenda de bloqueios, registro, edição, conclusão e
 * cancelamento. Qualquer perfil autenticado consulta; somente o administrador escreve. Os bloqueios
 * nunca são apagados, e os concluídos e cancelados continuam na lista como histórico. As regras de
 * período e de motivo são as mesmas da API: a tela valida em português e confere a entrada com a
 * mesma validação do {@link BloqueioDto.Entrada} antes de chamar o serviço.
 */
@Controller
@RequestMapping("/manutencoes")
@PreAuthorize("isAuthenticated()")
public class MaintenanceBlockWebController {

    static final int TAMANHO_PAGINA = 10;
    static final String AVISO_NAO_ENCONTRADO = "nao-encontrado";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter EXIBICAO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter CAMPO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final MaintenanceBlockService service;
    private final SalaService salas;
    private final ProfessorService professores;
    private final MaterialService materiais;
    private final ReservationService reservas;
    private final Validator validador;

    public MaintenanceBlockWebController(MaintenanceBlockService service, SalaService salas,
                                         ProfessorService professores, MaterialService materiais,
                                         ReservationService reservas, Validator validador) {
        this.service = service;
        this.salas = salas;
        this.professores = professores;
        this.materiais = materiais;
        this.reservas = reservas;
        this.validador = validador;
    }

    public record Form(String recurso, String motivo, String inicio, String fim) {
        public Form() {
            this("", "", "", "");
        }
    }

    public record Opcao(String valor, String rotulo) {
    }

    public record Grupo(String titulo, List<Opcao> opcoes) {
    }

    public record Linha(Long id, String recurso, String periodo, String motivo,
                        String estado, String classeEstado, boolean ativo) {
    }

    public record Detalhe(Long id, String recurso, String periodo, String motivo,
                          String estado, String classeEstado, String encerradoEm,
                          boolean ativo, boolean podeConcluir, boolean bloqueiaDisponibilidade,
                          String disponibilidade, long reservasNoPeriodo) {
    }

    // ---------- consulta ----------

    @GetMapping
    public String listar(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String tipo,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String aviso,
                         Model model, Authentication authentication) {
        TipoRecurso tipoFiltro = lerEnum(TipoRecurso.class, tipo);
        StatusBloqueio statusFiltro = lerEnum(StatusBloqueio.class, status);
        if ((preenchido(tipo) && tipoFiltro == null) || (preenchido(status) && statusFiltro == null)) {
            model.addAttribute("erroFiltro", "O filtro informado não é válido e foi ignorado.");
        }
        Map<String, String> nomes = nomesDosRecursos();
        List<BloqueioDto.Saida> todos = service.listar(tipoFiltro, null, statusFiltro);
        int pagina = Math.max(page, 0);
        int de = Math.min(pagina * TAMANHO_PAGINA, todos.size());
        int ate = Math.min(de + TAMANHO_PAGINA, todos.size());
        List<Linha> linhas = todos.subList(de, ate).stream().map(b -> linha(b, nomes)).toList();
        Page<Linha> resultado = new PageImpl<>(linhas, PageRequest.of(pagina, TAMANHO_PAGINA), todos.size());

        model.addAttribute("administrador", administrador(authentication));
        model.addAttribute("resultado", resultado);
        model.addAttribute("tipo", tipoFiltro == null ? "" : tipoFiltro.name());
        model.addAttribute("status", statusFiltro == null ? "" : statusFiltro.name());
        model.addAttribute("tipos", List.of(
                new Opcao("SALA", "Sala"), new Opcao("PROFESSOR", "Professor"), new Opcao("MATERIAL", "Material")));
        model.addAttribute("estados", List.of(
                new Opcao("ATIVO", "Ativo"), new Opcao("CONCLUIDO", "Concluído"), new Opcao("CANCELADO", "Cancelado")));
        if (AVISO_NAO_ENCONTRADO.equals(aviso)) {
            model.addAttribute("aviso", "Bloqueio não encontrado.");
        }
        return "manutencoes/lista";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, Authentication authentication) {
        BloqueioDto.Saida bloqueio = service.consultar(id);
        model.addAttribute("bloqueio", detalhe(bloqueio, nomesDosRecursos()));
        model.addAttribute("administrador", administrador(authentication));
        return "manutencoes/detalhe";
    }

    // ---------- registro e edição ----------

    @GetMapping("/novo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String novo(Model model) {
        model.addAttribute("form", new Form());
        prepararFormulario(model, null, null);
        return "manutencoes/formulario";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String criar(@ModelAttribute("form") Form form, BindingResult erros,
                        Model model, RedirectAttributes redirect) {
        prepararFormulario(model, null, null);
        BloqueioDto.Entrada entrada = montarEntrada(form, erros);
        if (entrada == null) {
            return "manutencoes/formulario";
        }
        try {
            BloqueioDto.Saida salvo = service.criar(entrada);
            redirect.addFlashAttribute("sucesso",
                    "Bloqueio registrado. O recurso consta como indisponível no período.");
            return "redirect:/manutencoes/" + salvo.id();
        } catch (RecursoNaoEncontradoException ex) {
            erros.rejectValue("recurso", "inexistente", "O recurso escolhido não existe mais.");
            return "manutencoes/formulario";
        }
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        BloqueioDto.Saida bloqueio = service.consultar(id);
        if (bloqueio.status() != StatusBloqueio.ATIVO) {
            redirect.addFlashAttribute("erro",
                    "Este bloqueio não pode mais ser alterado: ele já foi concluído ou cancelado.");
            return "redirect:/manutencoes/" + id;
        }
        Form form = new Form(
                bloqueio.recursoTipo().name() + ":" + bloqueio.recursoId(),
                bloqueio.motivo(),
                bloqueio.inicio().atZoneSameInstant(FUSO).format(CAMPO),
                bloqueio.fim().atZoneSameInstant(FUSO).format(CAMPO));
        model.addAttribute("form", form);
        prepararFormulario(model, id, form);
        return "manutencoes/formulario";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String atualizar(@PathVariable Long id, @ModelAttribute("form") Form form,
                            BindingResult erros, Model model, RedirectAttributes redirect) {
        prepararFormulario(model, id, form);
        BloqueioDto.Entrada entrada = montarEntrada(form, erros);
        if (entrada == null) {
            return "manutencoes/formulario";
        }
        try {
            service.atualizar(id, entrada);
        } catch (ConflitoDeEstadoException ex) {
            redirect.addFlashAttribute("erro",
                    "Este bloqueio não pode mais ser alterado: ele já foi concluído ou cancelado.");
            return "redirect:/manutencoes/" + id;
        }
        redirect.addFlashAttribute("sucesso", "Bloqueio atualizado.");
        return "redirect:/manutencoes/" + id;
    }

    // ---------- encerramento ----------

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String concluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.concluir(id);
            redirect.addFlashAttribute("sucesso",
                    "Bloqueio concluído. O recurso volta a ficar disponível a partir de agora.");
        } catch (ConflitoDeEstadoException ex) {
            redirect.addFlashAttribute("erro", service.consultar(id).status() == StatusBloqueio.ATIVO
                    ? "O bloqueio ainda não começou: aguarde o início ou cancele-o."
                    : "Este bloqueio não está mais ativo.");
        }
        return "redirect:/manutencoes/" + id;
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.cancelar(id);
            redirect.addFlashAttribute("sucesso",
                    "Bloqueio cancelado. Ele permanece no histórico e não afeta mais a disponibilidade.");
        } catch (ConflitoDeEstadoException ex) {
            redirect.addFlashAttribute("erro", "Este bloqueio não está mais ativo.");
        }
        return "redirect:/manutencoes/" + id;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public String bloqueioNaoEncontrado() {
        return "redirect:/manutencoes?aviso=" + AVISO_NAO_ENCONTRADO;
    }

    // ---------- validação do formulário ----------

    private BloqueioDto.Entrada montarEntrada(Form form, BindingResult erros) {
        TipoRecurso tipo = null;
        Long recursoId = null;
        String[] partes = form.recurso() == null ? new String[0] : form.recurso().trim().split(":");
        if (partes.length == 2) {
            tipo = lerEnum(TipoRecurso.class, partes[0]);
            recursoId = lerLong(partes[1]);
        }
        if (tipo == null || recursoId == null || recursoId < 1) {
            erros.rejectValue("recurso", "invalido", "Escolha o recurso.");
        }

        String motivo = form.motivo() == null ? "" : form.motivo().trim();
        if (motivo.isEmpty()) {
            erros.rejectValue("motivo", "obrigatorio", "O motivo é obrigatório.");
        } else if (motivo.length() > 500) {
            erros.rejectValue("motivo", "tamanho", "O motivo deve ter no máximo 500 caracteres.");
        }

        OffsetDateTime inicio = lerData(form.inicio(), "inicio", "início", erros);
        OffsetDateTime fim = lerData(form.fim(), "fim", "fim", erros);
        if (inicio != null && fim != null && !fim.isAfter(inicio)) {
            erros.rejectValue("fim", "periodo", "O fim deve ser posterior ao início.");
        } else if (fim != null && !fim.toInstant().isAfter(Instant.now())) {
            erros.rejectValue("fim", "passado", "O fim deve estar no futuro.");
        }
        if (erros.hasErrors()) {
            return null;
        }

        BloqueioDto.Entrada entrada = new BloqueioDto.Entrada(tipo, recursoId, motivo, inicio, fim);
        if (!validador.validate(entrada).isEmpty()) {
            erros.reject("invalido", "Os dados informados não são válidos.");
            return null;
        }
        return entrada;
    }

    private static OffsetDateTime lerData(String texto, String campo, String nome, BindingResult erros) {
        if (!preenchido(texto)) {
            erros.rejectValue(campo, "obrigatorio", "Informe o " + nome + " do período.");
            return null;
        }
        try {
            return LocalDateTime.parse(texto.trim()).atZone(FUSO).toOffsetDateTime();
        } catch (DateTimeParseException ex) {
            erros.rejectValue(campo, "formato", "A data de " + nome + " não é válida.");
            return null;
        }
    }

    // ---------- apoio ----------

    private void prepararFormulario(Model model, Long id, Form form) {
        model.addAttribute("edicao", id != null);
        model.addAttribute("titulo", id == null ? "Registrar bloqueio de manutenção" : "Editar bloqueio de manutenção");
        model.addAttribute("acao", id == null ? "/manutencoes" : "/manutencoes/" + id);
        if (id == null) {
            model.addAttribute("grupos", gruposDeRecursos());
        } else {
            model.addAttribute("rotuloRecurso", rotuloDoRecursoDoForm(form));
        }
    }

    private List<Grupo> gruposDeRecursos() {
        List<Grupo> grupos = new ArrayList<>();
        grupos.add(new Grupo("Salas", salas.listarTodas().stream()
                .filter(s -> Boolean.TRUE.equals(s.getAtiva()))
                .map(s -> new Opcao("SALA:" + s.getId(), s.getNome()))
                .toList()));
        grupos.add(new Grupo("Professores", professores.listar(null).stream()
                .map(p -> new Opcao("PROFESSOR:" + p.id(), p.nome()))
                .toList()));
        grupos.add(new Grupo("Materiais", materiais.listar(null, null).stream()
                .map(m -> new Opcao("MATERIAL:" + m.id(), m.nome()))
                .toList()));
        return grupos;
    }

    private Map<String, String> nomesDosRecursos() {
        Map<String, String> nomes = new HashMap<>();
        salas.listarTodas().forEach(s -> nomes.put("SALA:" + s.getId(), s.getNome()));
        professores.listar(null).forEach(p -> nomes.put("PROFESSOR:" + p.id(), p.nome()));
        materiais.listar(null, null).forEach(m -> nomes.put("MATERIAL:" + m.id(), m.nome()));
        return nomes;
    }

    private String rotuloDoRecursoDoForm(Form form) {
        String[] partes = form.recurso() == null ? new String[0] : form.recurso().trim().split(":");
        if (partes.length == 2) {
            TipoRecurso tipo = lerEnum(TipoRecurso.class, partes[0]);
            Long id = lerLong(partes[1]);
            if (tipo != null && id != null) {
                return rotuloDoRecurso(tipo, id, nomesDosRecursos());
            }
        }
        return "";
    }

    private static String rotuloDoRecurso(TipoRecurso tipo, Long id, Map<String, String> nomes) {
        String nome = nomes.get(tipo.name() + ":" + id);
        return rotuloDoTipo(tipo) + " · " + (nome != null ? nome : "#" + id);
    }

    private static String rotuloDoTipo(TipoRecurso tipo) {
        return switch (tipo) {
            case SALA -> "Sala";
            case PROFESSOR -> "Professor";
            case MATERIAL -> "Material";
        };
    }

    private static String rotuloDoEstado(StatusBloqueio status) {
        return switch (status) {
            case ATIVO -> "Ativo";
            case CONCLUIDO -> "Concluído";
            case CANCELADO -> "Cancelado";
        };
    }

    private static String classeDoEstado(StatusBloqueio status) {
        return switch (status) {
            case ATIVO -> "text-bg-warning";
            case CONCLUIDO -> "text-bg-success";
            case CANCELADO -> "text-bg-secondary";
        };
    }

    private static Linha linha(BloqueioDto.Saida b, Map<String, String> nomes) {
        return new Linha(b.id(), rotuloDoRecurso(b.recursoTipo(), b.recursoId(), nomes), periodo(b),
                b.motivo(), rotuloDoEstado(b.status()), classeDoEstado(b.status()),
                b.status() == StatusBloqueio.ATIVO);
    }

    private Detalhe detalhe(BloqueioDto.Saida b, Map<String, String> nomes) {
        boolean ativo = b.status() == StatusBloqueio.ATIVO;
        boolean cancelado = b.status() == StatusBloqueio.CANCELADO;
        OffsetDateTime fimEfetivo = b.fim();
        if (b.status() == StatusBloqueio.CONCLUIDO && b.encerradoEm() != null && b.encerradoEm().isBefore(b.fim())) {
            fimEfetivo = b.encerradoEm();
        }
        long reservasNoPeriodo = cancelado ? 0
                : reservas.contarReservasAtivasNoPeriodo(
                        b.recursoTipo(), b.recursoId(), b.inicio().toInstant(), fimEfetivo.toInstant());
        String disponibilidade = switch (b.status()) {
            case ATIVO -> "Neste período o recurso consta como indisponível por manutenção.";
            case CONCLUIDO -> "O bloqueio vigorou até " + fimEfetivo.atZoneSameInstant(FUSO).format(EXIBICAO)
                    + ". Depois disso o recurso voltou a ficar disponível.";
            case CANCELADO -> "Bloqueio cancelado: não afeta a disponibilidade do recurso.";
        };
        return new Detalhe(b.id(), rotuloDoRecurso(b.recursoTipo(), b.recursoId(), nomes), periodo(b), b.motivo(),
                rotuloDoEstado(b.status()), classeDoEstado(b.status()),
                b.encerradoEm() == null ? null : b.encerradoEm().atZoneSameInstant(FUSO).format(EXIBICAO),
                ativo, ativo && !b.inicio().toInstant().isAfter(Instant.now()), !cancelado,
                disponibilidade, reservasNoPeriodo);
    }

    private static String periodo(BloqueioDto.Saida b) {
        return b.inicio().atZoneSameInstant(FUSO).format(EXIBICAO) + " a "
                + b.fim().atZoneSameInstant(FUSO).format(EXIBICAO);
    }

    private static <E extends Enum<E>> E lerEnum(Class<E> tipo, String texto) {
        if (!preenchido(texto)) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, texto.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Long lerLong(String texto) {
        try {
            return Long.valueOf(texto.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean preenchido(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static boolean administrador(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }
}