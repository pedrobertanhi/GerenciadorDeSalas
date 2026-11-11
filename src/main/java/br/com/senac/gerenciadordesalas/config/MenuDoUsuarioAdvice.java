package br.com.senac.gerenciadordesalas.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Dados do cabecalho comum das telas (ENT-03, #185): quem esta conectado e quais itens do
 * menu o perfil pode ver. Fica disponivel em todos os templates como {@code menu}.
 *
 * <p>So controla a exibicao: a autorizacao continua no SecurityConfig e nos controllers.
 */
@ControllerAdvice(annotations = Controller.class)
public class MenuDoUsuarioAdvice {

    public record Menu(boolean autenticado, String usuario, boolean responsavel, boolean administrador) {

        static final Menu VISITANTE = new Menu(false, "", false, false);
    }

    @ModelAttribute("menu")
    public Menu menu(Authentication autenticacao) {
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            return Menu.VISITANTE;
        }
        return new Menu(true, autenticacao.getName(),
                temPerfil(autenticacao, "ROLE_RESPONSAVEL"),
                temPerfil(autenticacao, "ROLE_ADMINISTRADOR"));
    }

    private static boolean temPerfil(Authentication autenticacao, String perfil) {
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(perfil::equals);
    }
}
