package br.com.senac.gerenciadordesalas.seguranca;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProtegidoTestController {

    @PostMapping("/reservas")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public String criarReserva() {
        return "Reserva criada";
    }

    @GetMapping("/reservas/minhas/{email}")
    // O PreAuthorize garante que apenas o próprio utilizador possa aceder às suas reservas
    @PreAuthorize("hasRole('SOLICITANTE') and #email == authentication.name")
    public String acessarMinhasReservas(@PathVariable String email) {
        return "Minhas reservas de " + email;
    }

    @PostMapping("/reservas/{id}/aprovar")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public String aprovarReserva(@PathVariable Long id) {
        return "Reserva " + id + " aprovada";
    }

    @PostMapping("/admin/usuarios")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String gerenciarUsuarios() {
        return "Gerenciamento de usuários";
    }

    @GetMapping("/disponibilidade")
    public String verificarDisponibilidade() {
        return "Disponível";
    }
}