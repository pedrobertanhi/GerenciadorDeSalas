package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Sessão de quem foi inativado depois de entrar deixa de valer (ENT-02). */
class FiltroUsuarioAtivoTest {

    private final UserRepository usuarios = mock(UserRepository.class);
    private final FiltroUsuarioAtivo filtro = new FiltroUsuarioAtivo(usuarios);
    private final MockHttpServletRequest requisicao = new MockHttpServletRequest();
    private final MockHttpServletResponse resposta = new MockHttpServletResponse();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String email) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                email, "n/a", AuthorityUtils.createAuthorityList("ROLE_SOLICITANTE")));
    }

    private static User usuario(boolean ativo) {
        User usuario = new User("Ana", "ana@exemplo.test", "hash", Role.SOLICITANTE);
        if (!ativo) {
            usuario.deactivate();
        }
        return usuario;
    }

    @Test
    void usuarioInativadoPerdeASessaoEASegueComoAnonimo() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        requisicao.setSession(sessao);
        autenticar("Ana@Exemplo.test");
        when(usuarios.findByEmail("ana@exemplo.test")).thenReturn(Optional.of(usuario(false)));
        MockFilterChain cadeia = new MockFilterChain();

        filtro.doFilter(requisicao, resposta, cadeia);

        assertThat(sessao.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(AnonymousAuthenticationToken.class);
        assertThat(cadeia.getRequest()).as("a requisição continua pela cadeia, que vai negar como anônima").isNotNull();
    }

    @Test
    void usuarioAtivoMantemSessaoEAutenticacao() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        requisicao.setSession(sessao);
        autenticar("ana@exemplo.test");
        when(usuarios.findByEmail("ana@exemplo.test")).thenReturn(Optional.of(usuario(true)));

        filtro.doFilter(requisicao, resposta, new MockFilterChain());

        assertThat(sessao.isInvalid()).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("ana@exemplo.test");
    }

    @Test
    void usuarioQueNaoExisteNoBancoNaoEhTocadoPeloFiltro() throws Exception {
        autenticar("fantasma@exemplo.test");
        when(usuarios.findByEmail("fantasma@exemplo.test")).thenReturn(Optional.empty());

        filtro.doFilter(requisicao, resposta, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("fantasma@exemplo.test");
    }

    @Test
    void semAutenticacaoOuComAnonimoNaoConsultaOBanco() throws Exception {
        filtro.doFilter(requisicao, resposta, new MockFilterChain());
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "chave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        filtro.doFilter(new MockHttpServletRequest(), resposta, new MockFilterChain());

        verifyNoInteractions(usuarios);
    }

    @Test
    void inativoSemSessaoHttpTambemViraAnonimoSemErro() throws Exception {
        autenticar("ana@exemplo.test");
        when(usuarios.findByEmail("ana@exemplo.test")).thenReturn(Optional.of(usuario(false)));

        filtro.doFilter(requisicao, resposta, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(AnonymousAuthenticationToken.class);
    }
}
