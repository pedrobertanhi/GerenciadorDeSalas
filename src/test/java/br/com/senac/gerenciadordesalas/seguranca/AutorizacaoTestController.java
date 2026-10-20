package br.com.senac.gerenciadordesalas.seguranca;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
class AutorizacaoTestController {

    private final AtomicInteger alteracoesAdministrativas = new AtomicInteger();

    @GetMapping("/disponibilidade")
    String consultarDisponibilidade() {
        return "disponivel";
    }

    @PostMapping("/reservas")
    String criarReserva() {
        return "reserva-criada";
    }

    @PostMapping("/reservas/{id}/aprovar")
    String aprovarReserva(@PathVariable Long id) {
        return "reserva-aprovada-" + id;
    }

    @PostMapping("/movimentacoes/retirada")
    String registrarRetirada() {
        return "retirada-registrada";
    }

    @PostMapping("/admin/usuarios")
    String criarUsuario() {
        alteracoesAdministrativas.incrementAndGet();
        return "usuario-criado";
    }

    @GetMapping("/reservas/minhas/{emailDoDono}")
    @PreAuthorize("hasRole('SOLICITANTE') and "
            + "@autorizacaoDePropriedade.ehDono(authentication, #emailDoDono)")
    String consultarReservaPropria(@PathVariable String emailDoDono) {
        return "reserva-propria";
    }

    int totalDeAlteracoesAdministrativas() {
        return alteracoesAdministrativas.get();
    }
}
