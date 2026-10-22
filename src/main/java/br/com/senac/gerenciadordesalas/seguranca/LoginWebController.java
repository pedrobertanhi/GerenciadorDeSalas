package br.com.senac.gerenciadordesalas.seguranca;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginWebController {
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
