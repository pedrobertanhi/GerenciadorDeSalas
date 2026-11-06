package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Telas de criação, consulta, alteração e cancelamento de reservas (RSV-03/UI-09/UI-10, #25). */
@Controller
@RequestMapping("/reservas")
@PreAuthorize("isAuthenticated()")
public class ReservationWebController {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter CAMPO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ReservationService reservas;
    private final ReservationManagementService gestao;
    private final SalaRepository salas;
    private final ProfessorRepository professores;
    private final MaterialRepository materiais;

    public ReservationWebController(ReservationService reservas, ReservationManagementService gestao,
                                    SalaRepository salas, ProfessorRepository professores,
                                    MaterialRepository materiais) {
        this.reservas = reservas;
        this.gestao = gestao;
        this.salas = salas;
        this.professores = professores;
        this.materiais = materiais;
    }

    public record Form(Long salaId, Long professorId, String inicio, String termino,
                       @NotBlank(message = "A finalidade é obrigatória.") String finalidade,
                       Long materialId1, Integer quantidade1, Long materialId2, Integer quantidade2,
                       Long materialId3, Integer quantidade3) {
        public Form() { this(null, null, "", "", "", null, null, null, null, null, null); }
    }

    public record Linha(Long id, String sala, String periodo, String finalidade, String status,
                        boolean permiteAlteracao, boolean permiteCancelamento) { }

    public record Detalhe(Long id, String sala, String professor, String periodo, String finalidade,
                          String status, List<String> materiais, boolean permiteAlteracao,
                          boolean permiteCancelamento) { }

    @GetMapping
    public String listar(Model model, Authentication autenticacao) {
        List<Linha> linhas = gestao.listarPara(autenticacao.getName()).stream().map(this::linha).toList();
        model.addAttribute("reservas", linhas);
        model.addAttribute("operacional", operacional(autenticacao));
        return "reservas/lista";
    }

    @GetMapping("/nova")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public String nova(Model model) {
        model.addAttribute("form", new Form());
        prepararFormulario(model, false, null);
        return "reservas/formulario";
    }

    @PostMapping
    @PreAuthorize("hasRole('SOLICITANTE')")
    public String criar(@Valid @ModelAttribute("form") Form form, BindingResult erros, Model model,
                        Authentication autenticacao, RedirectAttributes redirecionamento) {
        ReservationDto.Entrada entrada = entrada(form, erros);
        if (entrada == null) {
            prepararFormulario(model, false, null);
            return "reservas/formulario";
        }
        try {
            ReservationDto.Saida criada = reservas.criar(entrada, autenticacao.getName());
            redirecionamento.addFlashAttribute("sucesso", "Reserva criada e aguardando o fluxo de aprovação.");
            return "redirect:/reservas/" + criada.id();
        } catch (ConflitoDeEstadoException | RecursoNaoEncontradoException ex) {
            erros.reject("reserva", mensagemSegura(ex));
            prepararFormulario(model, false, null);
            return "reservas/formulario";
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, Authentication autenticacao) {
        ReservationDto.Saida reserva = gestao.consultarPara(id, autenticacao.getName());
        model.addAttribute("reserva", detalhe(reserva));
        return "reservas/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, Authentication autenticacao,
                         RedirectAttributes redirecionamento) {
        ReservationDto.Saida reserva = gestao.consultarPara(id, autenticacao.getName());
        if (reserva.status() != StatusReserva.CRIADA) {
            redirecionamento.addFlashAttribute("erro", "Somente reservas criadas podem ser alteradas.");
            return "redirect:/reservas/" + id;
        }
        model.addAttribute("form", de(reserva));
        prepararFormulario(model, true, id);
        return "reservas/formulario";
    }

    @PostMapping("/{id}")
    public String alterar(@PathVariable Long id, @Valid @ModelAttribute("form") Form form, BindingResult erros,
                          Model model, Authentication autenticacao, RedirectAttributes redirecionamento) {
        ReservationDto.Alteracao entrada = alteracao(form, erros);
        if (entrada == null) {
            prepararFormulario(model, true, id);
            return "reservas/formulario";
        }
        try {
            gestao.alterar(id, entrada, autenticacao.getName());
            redirecionamento.addFlashAttribute("sucesso", "Reserva alterada com sucesso.");
            return "redirect:/reservas/" + id;
        } catch (ConflitoDeEstadoException | RecursoNaoEncontradoException ex) {
            erros.reject("reserva", mensagemSegura(ex));
            prepararFormulario(model, true, id);
            return "reservas/formulario";
        }
    }

    @GetMapping("/{id}/cancelar")
    public String confirmarCancelamento(@PathVariable Long id, Model model, Authentication autenticacao,
                                        RedirectAttributes redirecionamento) {
        ReservationDto.Saida reserva = gestao.consultarPara(id, autenticacao.getName());
        if (!podeCancelar(reserva.status())) {
            redirecionamento.addFlashAttribute("erro", "Reserva em andamento ou finalizada não pode ser cancelada.");
            return "redirect:/reservas/" + id;
        }
        model.addAttribute("reserva", detalhe(reserva));
        return "reservas/confirmar-cancelamento";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, Authentication autenticacao, RedirectAttributes redirecionamento) {
        try {
            gestao.cancelar(id, autenticacao.getName());
            redirecionamento.addFlashAttribute("sucesso", "Reserva cancelada e preservada no histórico.");
        } catch (ConflitoDeEstadoException ex) {
            redirecionamento.addFlashAttribute("erro", "Reserva em andamento ou finalizada não pode ser cancelada.");
        }
        return "redirect:/reservas/" + id;
    }

    private ReservationDto.Entrada entrada(Form form, BindingResult erros) {
        Dados dados = dados(form, erros);
        return dados == null ? null : new ReservationDto.Entrada(form.salaId(), form.professorId(), dados.inicio,
                dados.termino, form.finalidade(), dados.materiais);
    }

    private ReservationDto.Alteracao alteracao(Form form, BindingResult erros) {
        Dados dados = dados(form, erros);
        return dados == null ? null : new ReservationDto.Alteracao(form.salaId(), form.professorId(), dados.inicio,
                dados.termino, form.finalidade(), dados.materiais);
    }

    private Dados dados(Form form, BindingResult erros) {
        if (form.salaId() == null) erros.rejectValue("salaId", "obrigatoria", "Escolha uma sala.");
        if (form.finalidade() == null || form.finalidade().isBlank()) {
            erros.rejectValue("finalidade", "obrigatoria", "A finalidade é obrigatória.");
        }
        OffsetDateTime inicio = data(form.inicio(), "inicio", erros);
        OffsetDateTime termino = data(form.termino(), "termino", erros);
        if (inicio != null && termino != null && !termino.isAfter(inicio)) {
            erros.rejectValue("termino", "periodo", "O término deve ser posterior ao início.");
        }
        List<ReservationDto.MaterialPedido> pedidos = materiais(form, erros);
        return erros.hasErrors() ? null : new Dados(inicio, termino, pedidos);
    }

    private OffsetDateTime data(String texto, String campo, BindingResult erros) {
        if (texto == null || texto.isBlank()) {
            erros.rejectValue(campo, "obrigatorio", "Informe a data e hora.");
            return null;
        }
        try { return LocalDateTime.parse(texto.trim()).atZone(FUSO).toOffsetDateTime(); }
        catch (DateTimeParseException ex) {
            erros.rejectValue(campo, "formato", "A data e hora não são válidas.");
            return null;
        }
    }

    private List<ReservationDto.MaterialPedido> materiais(Form form, BindingResult erros) {
        List<ReservationDto.MaterialPedido> resultado = new ArrayList<>();
        adicionarMaterial(form.materialId1(), form.quantidade1(), "quantidade1", resultado, erros);
        adicionarMaterial(form.materialId2(), form.quantidade2(), "quantidade2", resultado, erros);
        adicionarMaterial(form.materialId3(), form.quantidade3(), "quantidade3", resultado, erros);
        return resultado;
    }

    private void adicionarMaterial(Long id, Integer quantidade, String campo,
                                   List<ReservationDto.MaterialPedido> resultado, BindingResult erros) {
        if (id == null && quantidade == null) return;
        if (id == null || quantidade == null || quantidade < 1) {
            erros.rejectValue(campo, "material", "Informe material e quantidade maior que zero.");
            return;
        }
        resultado.add(new ReservationDto.MaterialPedido(id, quantidade));
    }

    private void prepararFormulario(Model model, boolean edicao, Long id) {
        model.addAttribute("edicao", edicao);
        model.addAttribute("acao", id == null ? "/reservas" : "/reservas/" + id);
        model.addAttribute("salas", salas.findAll().stream().filter(s -> Boolean.TRUE.equals(s.getAtiva())).toList());
        model.addAttribute("professores", professores.buscarAtivos(null));
        model.addAttribute("materiais", materiais.buscarAtivos(null, null));
    }

    private Linha linha(ReservationDto.Saida reserva) {
        return new Linha(reserva.id(), "Sala #" + reserva.salaId(), periodo(reserva), reserva.finalidade(),
                rotulo(reserva.status()), reserva.status() == StatusReserva.CRIADA, podeCancelar(reserva.status()));
    }

    private Detalhe detalhe(ReservationDto.Saida reserva) {
        return new Detalhe(reserva.id(), "Sala #" + reserva.salaId(),
                reserva.professorId() == null ? "Não informado" : "Professor #" + reserva.professorId(),
                periodo(reserva), reserva.finalidade(), rotulo(reserva.status()),
                reserva.materiais().stream().map(m -> m.nome() + " — " + m.quantidade()).toList(),
                reserva.status() == StatusReserva.CRIADA, podeCancelar(reserva.status()));
    }

    private Form de(ReservationDto.Saida r) {
        List<ReservationDto.MaterialReservado> m = r.materiais();
        return new Form(r.salaId(), r.professorId(), r.inicio().atZone(FUSO).format(CAMPO),
                r.termino().atZone(FUSO).format(CAMPO), r.finalidade(),
                id(m, 0), quantidade(m, 0), id(m, 1), quantidade(m, 1), id(m, 2), quantidade(m, 2));
    }

    private static Long id(List<ReservationDto.MaterialReservado> m, int i) { return m.size() > i ? m.get(i).id() : null; }
    private static Integer quantidade(List<ReservationDto.MaterialReservado> m, int i) { return m.size() > i ? m.get(i).quantidade() : null; }
    private static String periodo(ReservationDto.Saida r) { return r.inicio().atZone(FUSO).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " a " + r.termino().atZone(FUSO).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
    private static String rotulo(StatusReserva s) { return s.name().replace('_', ' '); }
    private static boolean podeCancelar(StatusReserva s) { return s == StatusReserva.CRIADA || s == StatusReserva.APROVADA; }
    private static boolean operacional(Authentication a) { return a.getAuthorities().stream().anyMatch(x -> !x.getAuthority().equals("ROLE_SOLICITANTE")); }
    private static String mensagemSegura(Exception ex) { return ex.getMessage() == null ? "Não foi possível concluir a operação." : ex.getMessage(); }
    private record Dados(OffsetDateTime inicio, OffsetDateTime termino, List<ReservationDto.MaterialPedido> materiais) { }
}
