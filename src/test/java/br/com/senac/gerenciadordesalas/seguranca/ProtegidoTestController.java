package br.com.senac.gerenciadordesalas.seguranca;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
class ProtegidoTestController {

    @GetMapping("/perfil")
    String protegido() {
        return "ok";
    }

    @PostMapping("/reservas")
    String criarReserva(
            @RequestBody(required = false) Map<String, Object> corpo
    ) {
        return "ok";
    }

    @GetMapping("/reservas/minhas/{email}")
    String minhasReservas(@PathVariable String email) {
        return "ok";
    }

    @PostMapping("/reservas/{id}/aprovar")
    String aprovar() {
        return "ok";
    }

    @PostMapping("/admin/usuarios")
    String adminUsuarios() {
        return "ok";
    }

    @GetMapping("/disponibilidade")
    String disponibilidade() {
        return "ok";
    }
}