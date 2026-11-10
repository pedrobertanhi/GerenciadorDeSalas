package br.com.senac.gerenciadordesalas.usuario;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras da administração de usuários (ENT-02): senha só como hash, e-mail único, inativação sem
 * exclusão e proteção do último administrador ativo.
 */
class UsuarioAdminServiceTest {

    private final UserRepository usuarios = mock(UserRepository.class);
    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final UsuarioAdminService service = new UsuarioAdminService(usuarios, codificador, mock(br.com.senac.gerenciadordesalas.auditoria.AuditService.class));

    @BeforeEach
    void gravacaoDevolveOQueRecebe() {
        when(usuarios.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static User usuario(String nome, String email, Role perfil) {
        return new User(nome, email, "hash-existente", perfil);
    }

    // ---------- criação ----------

    @Test
    void criaComEmailNormalizadoEGuardaSomenteOHashDaSenha() {
        UsuarioDto.Saida saida = service.criar(
                new UsuarioDto.Entrada("  Maria Souza ", " MARIA@Exemplo.TEST ", "Senha#Segura2026", Role.RESPONSAVEL), "admin@exemplo.test");

        assertThat(saida.nome()).isEqualTo("Maria Souza");
        assertThat(saida.email()).isEqualTo("maria@exemplo.test");
        assertThat(saida.perfil()).isEqualTo(Role.RESPONSAVEL);
        assertThat(saida.ativo()).isTrue();
        org.mockito.ArgumentCaptor<User> gravado = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(usuarios).saveAndFlush(gravado.capture());
        assertThat(gravado.getValue().getPasswordHash()).isNotEqualTo("Senha#Segura2026").startsWith("$2");
        assertThat(codificador.matches("Senha#Segura2026", gravado.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void asSaidasNuncaTrazemCredencial() {
        UsuarioDto.Saida saida = service.criar(
                new UsuarioDto.Entrada("Maria", "maria@exemplo.test", "Senha#Segura2026", Role.SOLICITANTE), "admin@exemplo.test");

        assertThat(saida.toString()).doesNotContain("Senha#Segura2026").doesNotContain("$2");
    }

    @Test
    void rejeitaEmailDuplicadoSemGravar() {
        when(usuarios.existsByEmailIgnoreCase("maria@exemplo.test")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(
                new UsuarioDto.Entrada("Maria", "MARIA@exemplo.test", "Senha#Segura2026", Role.SOLICITANTE), "admin@exemplo.test"))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessage("E-mail ja cadastrado");
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void converteViolacaoDeUnicidadeDoBancoEmConflito() {
        when(usuarios.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uk_email"));

        assertThatThrownBy(() -> service.criar(
                new UsuarioDto.Entrada("Maria", "maria@exemplo.test", "Senha#Segura2026", Role.SOLICITANTE), "admin@exemplo.test"))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageNotContaining("uk_email");
    }

    // ---------- consulta ----------

    @Test
    void listaOrdenadaPorNomeEFiltraPorPerfilESituacao() {
        User ana = usuario("ana", "ana@exemplo.test", Role.SOLICITANTE);
        User bruno = usuario("Bruno", "bruno@exemplo.test", Role.RESPONSAVEL);
        User carla = usuario("Carla", "carla@exemplo.test", Role.SOLICITANTE);
        carla.deactivate();
        when(usuarios.findAll()).thenReturn(List.of(carla, bruno, ana));

        assertThat(service.listar(null, null)).extracting(UsuarioDto.Saida::nome).containsExactly("ana", "Bruno", "Carla");
        assertThat(service.listar(Role.SOLICITANTE, null)).extracting(UsuarioDto.Saida::nome).containsExactly("ana", "Carla");
        assertThat(service.listar(null, true)).extracting(UsuarioDto.Saida::nome).containsExactly("ana", "Bruno");
        assertThat(service.listar(Role.SOLICITANTE, false)).extracting(UsuarioDto.Saida::nome).containsExactly("Carla");
    }

    @Test
    void usuarioInexistenteVira404NeutroEmTodasAsOperacoes() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());
        when(usuarios.bloquearPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.atualizar(99L, new UsuarioDto.Atualizacao("A", Role.SOLICITANTE), "admin@exemplo.test"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.inativar(99L, "admin@exemplo.test")).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.ativar(99L, "admin@exemplo.test")).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- atualização ----------

    @Test
    void atualizaNomeEPerfilSemMexerNoEmailNemNaSenha() {
        User maria = usuario("Maria", "maria@exemplo.test", Role.SOLICITANTE);
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(maria));

        UsuarioDto.Saida saida = service.atualizar(1L, new UsuarioDto.Atualizacao(" Maria Souza ", Role.RESPONSAVEL), "admin@exemplo.test");

        assertThat(saida.nome()).isEqualTo("Maria Souza");
        assertThat(saida.perfil()).isEqualTo(Role.RESPONSAVEL);
        assertThat(maria.getEmail()).isEqualTo("maria@exemplo.test");
        assertThat(maria.getPasswordHash()).isEqualTo("hash-existente");
    }

    @Test
    void naoRemoveOPerfilDoUltimoAdministradorAtivo() {
        User admin = usuario("Admin", "admin@exemplo.test", Role.ADMINISTRADOR);
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(admin));
        when(usuarios.bloquearAtivosDoPerfil(Role.ADMINISTRADOR)).thenReturn(List.of(admin));

        assertThatThrownBy(() -> service.atualizar(1L, new UsuarioDto.Atualizacao("Admin", Role.SOLICITANTE), "admin@exemplo.test"))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining("ultimo administrador ativo");
        assertThat(admin.getProfile()).isEqualTo(Role.ADMINISTRADOR);
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void rebaixaAdministradorQuandoExisteOutroAtivo() {
        User admin = usuario("Admin", "admin@exemplo.test", Role.ADMINISTRADOR);
        User outro = usuario("Outro", "outro@exemplo.test", Role.ADMINISTRADOR);
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(admin));
        when(usuarios.bloquearAtivosDoPerfil(Role.ADMINISTRADOR)).thenReturn(List.of(admin, outro));

        UsuarioDto.Saida saida = service.atualizar(1L, new UsuarioDto.Atualizacao("Admin", Role.RESPONSAVEL), "outro@exemplo.test");

        assertThat(saida.perfil()).isEqualTo(Role.RESPONSAVEL);
    }

    @Test
    void mudarONomeDoUnicoAdministradorSemMudarOPerfilEPermitido() {
        User admin = usuario("Admin", "admin@exemplo.test", Role.ADMINISTRADOR);
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(admin));

        UsuarioDto.Saida saida = service.atualizar(1L, new UsuarioDto.Atualizacao("Admin Geral", Role.ADMINISTRADOR), "admin@exemplo.test");

        assertThat(saida.nome()).isEqualTo("Admin Geral");
    }

    // ---------- inativação e reativação ----------

    @Test
    void inativaUsuarioComumSemApagarNada() {
        User maria = usuario("Maria", "maria@exemplo.test", Role.SOLICITANTE);
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(maria));

        UsuarioDto.Saida saida = service.inativar(1L, "admin@exemplo.test");

        assertThat(saida.ativo()).isFalse();
        assertThat(maria.isActive()).isFalse();
    }

    @Test
    void naoInativaOUltimoAdministradorAtivo() {
        User admin = usuario("Admin", "admin@exemplo.test", Role.ADMINISTRADOR);
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(admin));
        when(usuarios.bloquearAtivosDoPerfil(Role.ADMINISTRADOR)).thenReturn(List.of(admin));

        assertThatThrownBy(() -> service.inativar(1L, "admin@exemplo.test"))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining("ultimo administrador ativo");
        assertThat(admin.isActive()).isTrue();
    }

    @Test
    void inativaAdministradorQuandoExisteOutroAtivo() {
        User admin = usuario("Admin", "admin@exemplo.test", Role.ADMINISTRADOR);
        User outro = usuario("Outro", "outro@exemplo.test", Role.ADMINISTRADOR);
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(admin));
        when(usuarios.bloquearAtivosDoPerfil(Role.ADMINISTRADOR)).thenReturn(List.of(admin, outro));

        assertThat(service.inativar(1L, "outro@exemplo.test").ativo()).isFalse();
    }

    @Test
    void inativarDuasVezesEConflito() {
        User maria = usuario("Maria", "maria@exemplo.test", Role.SOLICITANTE);
        maria.deactivate();
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(maria));

        assertThatThrownBy(() -> service.inativar(1L, "admin@exemplo.test"))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessage("Usuario ja esta inativo");
    }

    @Test
    void reativaUsuarioInativoEImpedeReativarDuasVezes() {
        User maria = usuario("Maria", "maria@exemplo.test", Role.SOLICITANTE);
        maria.deactivate();
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(maria));

        assertThat(service.ativar(1L, "admin@exemplo.test").ativo()).isTrue();
        assertThatThrownBy(() -> service.ativar(1L, "admin@exemplo.test"))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessage("Usuario ja esta ativo");
    }
}
