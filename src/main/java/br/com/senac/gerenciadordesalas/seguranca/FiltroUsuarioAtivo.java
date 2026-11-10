package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Derruba a sessão de quem foi inativado depois de entrar (ENT-02). O login já recusa usuário inativo,
 * mas uma sessão aberta antes da inativação continuaria valendo. A cada requisição autenticada o filtro
 * confere a situação atual no banco: se o usuário existe e está inativo, a sessão é invalidada e a
 * requisição segue como anônima, o que resulta em 401 (ou no redirecionamento para o login).
 * Usuário que não existe no banco não é tocado aqui: a autenticação sempre nasce do banco.
 */
public class FiltroUsuarioAtivo extends OncePerRequestFilter {

    private final UserRepository usuarios;

    public FiltroUsuarioAtivo(UserRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null
                && autenticacao.isAuthenticated()
                && !(autenticacao instanceof AnonymousAuthenticationToken)
                && sessaoDesatualizada(autenticacao)) {
            HttpSession sessao = requisicao.getSession(false);
            if (sessao != null) {
                sessao.invalidate();
            }
            SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                    "usuario-inativo", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private boolean sessaoDesatualizada(Authentication autenticacao) {
        return usuarios.findByEmail(User.normalizarEmail(autenticacao.getName()))
                .map(usuario -> !usuario.isActive() || autenticacao.getAuthorities().stream()
                        .noneMatch(papel -> papel.getAuthority().equals("ROLE_" + usuario.getProfile().name())))
                .orElse(false);
    }
}
