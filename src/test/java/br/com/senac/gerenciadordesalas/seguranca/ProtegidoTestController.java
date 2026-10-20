package br.com.senac.gerenciadordesalas.seguranca;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class ProtegidoTestController {

    @GetMapping("/perfil")
    String protegido() {
        return "ok";
    }
}
