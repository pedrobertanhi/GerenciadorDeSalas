package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioDetailsServiceTest {

    private static final String EMAIL = "ana.sintetica@exemplo.test";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UsuarioDetailsService servico = new UsuarioDetailsService(userRepository);

    private User usuario(Role perfil) {
        return new User("Ana Sintetica", EMAIL, "$2a$10$hashsinteticodeteste", perfil);
    }

    @Test
    void usuarioAtivo_viraUserDetailsHabilitadoComAutoridadeDoPerfil() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(Role.RESPONSAVEL)));

        UserDetails detalhes = servico.loadUserByUsername(EMAIL);

        assertThat(detalhes.getUsername()).isEqualTo(EMAIL);
        assertThat(detalhes.getPassword()).isEqualTo("$2a$10$hashsinteticodeteste");
        assertThat(detalhes.isEnabled()).isTrue();
        assertThat(detalhes.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_RESPONSAVEL");
    }

    @Test
    void usuarioInativo_viraUserDetailsDesabilitado() {
        User inativo = usuario(Role.SOLICITANTE);
        inativo.deactivate();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(inativo));

        UserDetails detalhes = servico.loadUserByUsername(EMAIL);

        assertThat(detalhes.isEnabled()).isFalse();
    }

    @Test
    void emailEhNormalizadoAntesDaBusca() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(Role.SOLICITANTE)));

        servico.loadUserByUsername("  ANA.Sintetica@Exemplo.TEST ");

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void usuarioInexistente_lancaUsernameNotFound() {
        assertThatThrownBy(() -> servico.loadUserByUsername("ninguem@exemplo.test"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
} 