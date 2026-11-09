package br.com.senac.gerenciadordesalas.aprovacao;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.reservas.SituacaoPendencia;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tela de aprovacoes e validacao docente (UI-11, #34). Somente o Responsavel acessa e decide.
 * As regras ficam no {@link AprovacaoService}: a tela so traduz o resultado em mensagens.
 * Conflitos (decisao repetida, manutencao, sobreposicao, decisao concorrente) aparecem como erro
 * na fila, sem detalhes internos.
 */
@Controller
@RequestMapping("/aprovacoes")
@PreAuthorize("hasRole('RESPONSAVEL')")
public class AprovacaoWebController {

    static final int TAMANHO_MAXIMO_MOTIVO = 500;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter EXIBICAO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AprovacaoService service;

    public AprovacaoWebController(AprovacaoService service) {
        this.service = service;
    }

    /** Linha da fila e conteudo da tela de decisao, ja formatados para exibicao. */
    public record Item(Long id, String solicitadaEm, String solicitante, String sala, String professor,
                       String competencias, String periodo, String finalidade, List<String> materiais,
                       List<String> pendencias, boolean recursoPendente, boolean docentePendente) {
    }

    @FunctionalInterface
    private interface Decisao {
        void executar();
    }

    @GetMapping
    public String fila(Model model, Authentication autenticacao) {
        model.addAttribute("itens", service.listarPendentes(autenticacao.getName()).stream().map(AprovacaoWebController::item).toList());
        return "aprovacoes/fila";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, Authentication autenticacao,
                          RedirectAttributes redirecionamento) {
        Optional<AprovacaoDto.ItemFila> pendente = service.listarPendentes(autenticacao.getName()).stream()
                .filter(item -> item.reservaId().equals(id))
                .findFirst();
        if (pendente.isEmpty()) {
            redirecionamento.addFlashAttribute("erro", "Esta reserva não está mais aguardando decisão.");
            return "redirect:/aprovacoes";
        }
        model.addAttribute("item", item(pendente.get()));
        model.addAttribute("tamanhoMaximoMotivo", TAMANHO_MAXIMO_MOTIVO);
        return "aprovacoes/detalhe";
    }

    @PostMapping("/{id}/aprovar")
    public String aprovar(@PathVariable Long id, Authentication autenticacao,
                          RedirectAttributes redirecionamento) {
        return decidir(redirecionamento, "Recurso restrito aprovado.",
                () -> service.aprovar(id, autenticacao.getName()));
    }

    @PostMapping("/{id}/rejeitar")
    public String rejeitar(@PathVariable Long id, @RequestParam(defaultValue = "") String motivo,
                           Authentication autenticacao, RedirectAttributes redirecionamento) {
        String problema = problemaNoMotivo(motivo);
        if (problema != null) {
            redirecionamento.addFlashAttribute("erro", problema);
            return "redirect:/aprovacoes/" + id;
        }
        return decidir(redirecionamento, "Reserva rejeitada e motivo registrado.",
                () -> service.rejeitar(id, motivo, autenticacao.getName()));
    }

    @PostMapping("/{id}/validar-docente")
    public String validarDocente(@PathVariable Long id, Authentication autenticacao,
                                 RedirectAttributes redirecionamento) {
        return decidir(redirecionamento, "Alocação docente validada.",
                () -> service.validarDocente(id, autenticacao.getName()));
    }

    @PostMapping("/{id}/rejeitar-docente")
    public String rejeitarDocente(@PathVariable Long id, @RequestParam(defaultValue = "") String motivo,
                                  Authentication autenticacao, RedirectAttributes redirecionamento) {
        String problema = problemaNoMotivo(motivo);
        if (problema != null) {
            redirecionamento.addFlashAttribute("erro", problema);
            return "redirect:/aprovacoes/" + id;
        }
        return decidir(redirecionamento, "Alocação docente rejeitada e motivo registrado.",
                () -> service.rejeitarDocente(id, motivo, autenticacao.getName()));
    }

    private static String decidir(RedirectAttributes redirecionamento, String sucesso, Decisao decisao) {
        try {
            decisao.executar();
            redirecionamento.addFlashAttribute("sucesso", sucesso);
        } catch (ConflitoDeEstadoException ex) {
            redirecionamento.addFlashAttribute("erro", "Não foi possível concluir a decisão: " + ex.getMessage());
        } catch (AccessDeniedException ex) {
            redirecionamento.addFlashAttribute("erro", "Sua conta não está ativa ou não possui permissão para decidir.");
        } catch (RecursoNaoEncontradoException ex) {
            redirecionamento.addFlashAttribute("erro", "Reserva não encontrada.");
        }
        return "redirect:/aprovacoes";
    }

    private static String problemaNoMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return "Informe o motivo da rejeição.";
        }
        if (motivo.trim().length() > TAMANHO_MAXIMO_MOTIVO) {
            return "O motivo deve ter no máximo " + TAMANHO_MAXIMO_MOTIVO + " caracteres.";
        }
        return null;
    }

    private static Item item(AprovacaoDto.ItemFila fila) {
        List<String> pendencias = new ArrayList<>();
        boolean recursoPendente = fila.aprovacaoRecurso() == SituacaoPendencia.PENDENTE;
        boolean docentePendente = fila.validacaoDocente() == SituacaoPendencia.PENDENTE;
        if (recursoPendente) {
            pendencias.add("Recurso restrito");
        }
        if (docentePendente) {
            pendencias.add("Validação docente");
        }
        return new Item(
                fila.reservaId(),
                formatar(fila.solicitadaEm()),
                fila.solicitanteNome() + " (" + fila.solicitanteEmail() + ")",
                fila.salaNome(),
                fila.professorNome() == null ? "Não informado" : fila.professorNome(),
                fila.professorCompetencias().isEmpty() ? "Nenhuma informada"
                        : String.join(", ", fila.professorCompetencias()),
                formatar(fila.inicio()) + " a " + formatar(fila.termino()),
                fila.finalidade(),
                fila.materiais().stream()
                        .map(m -> m.nome() + " (" + m.quantidade() + ")" + (m.restrito() ? ", restrito" : ""))
                        .toList(),
                pendencias,
                recursoPendente,
                docentePendente);
    }

    private static String formatar(Instant instante) {
        return instante == null ? "" : instante.atZone(FUSO).format(EXIBICAO);
    }
}
