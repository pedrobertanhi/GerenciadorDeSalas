package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.cadastros.NavegadorE2E.Tela;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roteiro E2E do menu comum (ENT-03, #185) em navegador real: itens por perfil, navegacao
 * entre telas, botao Sair e menu recolhido em telas pequenas sem rolagem horizontal.
 */
class NavegacaoNavegadorTest extends NavegadorBase {

    private static final String RESPONSAVEL_EMAIL = "responsavel.menu@exemplo.test";
    private static final List<String> TELAS = List.of(
            "/reservas", "/salas", "/recursos/pesquisa", "/manutencoes", "/professores");

    @Test
    void solicitanteNavegaPeloMenuESai() {
        navegador.usarTela(Tela.DESKTOP);
        navegador.entrarComo(SOLICITANTE_EMAIL, SENHA);

        navegador.clicarLink("Reservas");
        navegador.esperarCaminho("/reservas");
        navegador.clicarLink("Manutenções");
        navegador.esperarCaminho("/manutencoes");
        assertThat(navegador.existeLink("Aprovações")).isFalse();
        assertThat(navegador.existeLink("Usuários")).isFalse();
        assertThat(navegador.textoDaPagina()).contains(SOLICITANTE_EMAIL);

        navegador.clicarBotao("Sair");
        navegador.esperarTexto("Você saiu do sistema.");
        navegador.ir("/reservas");
        navegador.esperarCaminho("/login");
    }

    @Test
    void responsavelVeAprovacoesEAdministradorVeUsuarios() {
        navegador.usarTela(Tela.DESKTOP);
        usuarios.save(new User("Responsavel Menu", RESPONSAVEL_EMAIL, codificador.encode(SENHA), Role.RESPONSAVEL));

        navegador.entrarComo(RESPONSAVEL_EMAIL, SENHA);
        assertThat(navegador.existeLink("Aprovações")).isTrue();
        assertThat(navegador.existeLink("Usuários")).isFalse();
        navegador.clicarLink("Aprovações");
        navegador.esperarTexto("Aprovações pendentes");
        navegador.sair();

        navegador.entrarComo(ADMIN_EMAIL, SENHA);
        assertThat(navegador.existeLink("Usuários")).isTrue();
        assertThat(navegador.existeLink("Aprovações")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Tela.class)
    void menuNaoCausaRolagemHorizontal(Tela tela) {
        navegador.usarTela(tela);
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        for (String caminho : TELAS) {
            navegador.ir(caminho);
            assertThat(navegador.bootstrapCarregado())
                    .as("Bootstrap aplicado em %s (%s)", caminho, tela).isTrue();
            assertThat(navegador.larguraDoConteudo())
                    .as("a página %s não pode rolar na horizontal em %s", caminho, tela)
                    .isLessThanOrEqualTo(navegador.larguraDaJanela());
        }
    }
}
