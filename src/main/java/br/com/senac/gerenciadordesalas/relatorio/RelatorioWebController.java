package br.com.senac.gerenciadordesalas.relatorio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela de relatorios (ENT-03, #185): filtro de periodo, estados de erro, vazio, falha e dados.
 * Sem filtro, mostra o mes atual. O que cada perfil enxerga e decidido no {@link RelatorioService}.
 */
@Controller
@RequestMapping("/relatorios")
@PreAuthorize("isAuthenticated()")
public class RelatorioWebController {

    private static final Logger LOG = LoggerFactory.getLogger(RelatorioWebController.class);

    private final RelatorioService relatorios;
    private final Clock relogio;

    public RelatorioWebController(RelatorioService relatorios, Clock relogio) {
        this.relatorios = relatorios;
        this.relogio = relogio;
    }

    @GetMapping
    public String painel(@RequestParam(required = false) String de, @RequestParam(required = false) String ate,
                         Model model, Authentication autenticacao) {
        boolean operacional = operacional(autenticacao);
        List<String> erros = new ArrayList<>();
        PeriodoRelatorio periodo;
        if (de == null && ate == null) {
            periodo = PeriodoRelatorio.doMes(LocalDate.now(relogio.withZone(PeriodoRelatorio.FUSO)));
            de = periodo.de().toString();
            ate = periodo.ate().toString();
        } else {
            periodo = ler(de, ate, erros);
        }

        model.addAttribute("de", de == null ? "" : de);
        model.addAttribute("ate", ate == null ? "" : ate);
        model.addAttribute("operacional", operacional);
        model.addAttribute("erros", erros);
        if (periodo != null) {
            try {
                model.addAttribute("relatorio", relatorios.gerar(periodo, autenticacao.getName(), operacional));
            } catch (DataAccessException ex) {
                LOG.error("Falha ao gerar relatorio de {} a {}", periodo.de(), periodo.ate(), ex);
                model.addAttribute("falha", "Não foi possível gerar o relatório agora. Tente novamente em instantes.");
            }
        }
        return "relatorios/painel";
    }

    private static PeriodoRelatorio ler(String de, String ate, List<String> erros) {
        LocalDate inicio = data(de, "Informe a data inicial.", "Data inicial inválida.", erros);
        LocalDate fim = data(ate, "Informe a data final.", "Data final inválida.", erros);
        if (inicio == null || fim == null) {
            return null;
        }
        try {
            return new PeriodoRelatorio(inicio, fim);
        } catch (IllegalArgumentException ex) {
            erros.add(ex.getMessage());
            return null;
        }
    }

    private static LocalDate data(String valor, String vazio, String invalida, List<String> erros) {
        if (valor == null || valor.isBlank()) {
            erros.add(vazio);
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            erros.add(invalida);
            return null;
        }
    }

    private static boolean operacional(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream()
                .anyMatch(perfil -> perfil.getAuthority().equals("ROLE_RESPONSAVEL")
                        || perfil.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }
}
