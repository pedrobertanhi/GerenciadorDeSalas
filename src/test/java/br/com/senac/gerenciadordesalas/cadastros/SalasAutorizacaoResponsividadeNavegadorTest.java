package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.cadastros.NavegadorE2E.Tela;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Autorização e responsividade das telas de salas em navegador real (Issue #15): visitante vai para
 * a tela de login, só o administrador acessa o CRUD, e as telas se adaptam ao celular, ao tablet e
 * ao desktop.
 */
class SalasAutorizacaoResponsividadeNavegadorTest extends NavegadorBase {

    @Autowired
    SalaRepository salas;

    @Test
    void visitanteSemLoginVaiParaATelaDeLogin() {
        navegador.ir("/salas");

        navegador.esperarCaminho("/login");
        assertThat(navegador.textoDaPagina()).contains("Entrar no sistema");
    }

    @Test
    void solicitanteNaoAcessaNenhumaTelaDeSalas() {
        salas.save(new Sala("Sala Restrita", 30, "Bloco R"));
        navegador.entrarComo(SOLICITANTE_EMAIL, SENHA);

        navegador.ir("/salas");
        navegador.esperarTexto("ACESSO_NEGADO");
        assertThat(navegador.textoDaPagina()).doesNotContain("Sala Restrita").doesNotContain("Cadastrar sala");

        navegador.ir("/salas/novo");
        navegador.esperarTexto("ACESSO_NEGADO");
    }

    @ParameterizedTest
    @EnumSource(Tela.class)
    void telasDeSalasSeAdaptamAoTamanhoDaJanela(Tela tela) {
        salas.save(new Sala("Sala Responsiva", 30, "Bloco R"));
        navegador.usarTela(tela);
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        for (String caminho : List.of("/salas", "/salas/novo")) {
            navegador.ir(caminho);
            assertThat(navegador.bootstrapCarregado())
                    .as("Bootstrap aplicado em %s (%s)", caminho, tela).isTrue();
            assertThat(navegador.larguraDoConteudo())
                    .as("a página %s não pode rolar na horizontal em %s", caminho, tela)
                    .isLessThanOrEqualTo(navegador.larguraDaJanela());
        }

        navegador.ir("/salas");
        assertThat(navegador.rolagemHorizontalDe(".table-responsive")).isEqualTo("auto");
        long janela = navegador.larguraDaJanela();
        long campoDoFiltro = navegador.larguraDoCampo("capacidadeMinima");
        if (tela == Tela.CELULAR) {
            assertThat(campoDoFiltro).as("no celular o filtro ocupa a linha inteira").isGreaterThan(janela * 6 / 10);
        } else {
            assertThat(campoDoFiltro).as("em telas largas os campos do filtro ficam lado a lado").isLessThan(janela / 2);
        }

        navegador.ir("/salas/novo");
        for (String id : List.of("nome", "capacidade", "localizacao")) {
            assertThat(navegador.campoDentroDaJanela(id)).as("campo %s dentro da janela em %s", id, tela).isTrue();
        }
        assertThat(navegador.botaoDentroDaJanela("Salvar")).isTrue();
    }
}