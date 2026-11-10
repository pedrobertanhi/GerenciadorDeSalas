package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.cadastros.NavegadorE2E.Tela;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UsuariosNavegadorTest extends NavegadorBase {

    private static final String EMAIL = "maria.usuarios@exemplo.test";
    private static final String NOME = "Maria Usuários E2E";

    @ParameterizedTest
    @EnumSource(value = Tela.class, names = {"DESKTOP", "CELULAR"})
    void administradorGerenciaUsuarioPelaInterface(Tela tela) {
        navegador.usarTela(tela);
        navegador.entrarComo(ADMIN_EMAIL, SENHA);
        navegador.ir("/salas");
        navegador.clicarLink("Gerenciar usuários");
        navegador.esperarCaminho("/usuarios");
        verificarLayout(tela);
        assertThat(navegador.rolagemHorizontalDe(".table-responsive")).isEqualTo("auto");

        navegador.clicarLink("Cadastrar usuário");
        navegador.esperarCaminho("/usuarios/novo");
        verificarLayout(tela);
        for (String campo : List.of("nome", "email", "senha", "perfil")) {
            assertThat(navegador.campoDentroDaJanela(campo)).as("campo %s em %s", campo, tela).isTrue();
        }
        navegador.preencher("nome", NOME);
        navegador.preencher("email", EMAIL);
        navegador.preencher("senha", SENHA);
        navegador.escolher("perfil", Role.SOLICITANTE.name());
        navegador.clicarBotao("Salvar");
        navegador.esperarTexto("Usuário cadastrado.");

        navegador.clicarNaLinha(NOME, "Editar");
        navegador.esperarCaminho("/editar");
        verificarLayout(tela);
        String nomeEditado = NOME + " Editada";
        navegador.preencher("nome", nomeEditado);
        navegador.escolher("perfil", Role.RESPONSAVEL.name());
        navegador.clicarBotao("Salvar");
        navegador.esperarTexto("Usuário atualizado.");
        assertThat(usuarios.findByEmail(EMAIL).orElseThrow().getProfile()).isEqualTo(Role.RESPONSAVEL);

        navegador.clicarNaLinha(nomeEditado, "Inativar");
        navegador.esperarTexto("Confirmar inativação");
        verificarLayout(tela);
        navegador.clicarBotao("Inativar usuário");
        navegador.esperarTexto("Usuário inativado.");
        assertThat(usuarios.findByEmail(EMAIL).orElseThrow().isActive()).isFalse();

        navegador.clicarNaLinha(nomeEditado, "Ativar");
        navegador.esperarTexto("Confirmar ativação");
        verificarLayout(tela);
        navegador.clicarBotao("Ativar usuário");
        navegador.esperarTexto("Usuário ativado.");
        assertThat(usuarios.findByEmail(EMAIL).orElseThrow().isActive()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SOLICITANTE", "RESPONSAVEL"})
    void outrosPerfisNaoAcessamAsTelas(Role perfil) {
        String email = "perfil.usuarios@exemplo.test";
        User usuario = usuarios.save(new User("Perfil E2E", email, codificador.encode(SENHA), perfil));
        navegador.entrarComo(email, SENHA);

        for (String caminho : List.of("/usuarios", "/usuarios/novo",
                "/usuarios/" + usuario.getId() + "/editar", "/usuarios/" + usuario.getId() + "/situacao")) {
            navegador.ir(caminho);
            navegador.esperarTexto("ACESSO_NEGADO");
        }
    }

    @Test
    void administradorNavegaEntrePaginas() {
        String hash = codificador.encode(SENHA);
        for (int i = 0; i < 10; i++) {
            usuarios.save(new User("Conta " + i, "conta" + i + "@exemplo.test", hash, Role.SOLICITANTE));
        }
        navegador.entrarComo(ADMIN_EMAIL, SENHA);
        navegador.ir("/usuarios");
        navegador.esperarTexto("Página 1 de 2");
        navegador.clicarLink("Próxima");
        navegador.esperarTexto("Página 2 de 2");
        navegador.clicarLink("Anterior");
        navegador.esperarTexto("Página 1 de 2");
    }

    private void verificarLayout(Tela tela) {
        assertThat(navegador.bootstrapCarregado()).as("Bootstrap em %s", tela).isTrue();
        assertThat(navegador.larguraDoConteudo())
                .as("sem rolagem horizontal na página em %s", tela)
                .isLessThanOrEqualTo(navegador.larguraDaJanela());
    }
}
