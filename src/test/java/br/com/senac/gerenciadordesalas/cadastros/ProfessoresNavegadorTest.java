package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.cadastros.NavegadorE2E.Tela;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roteiro E2E de professores e competencias (ENT-03, #185): cadastro, e-mail duplicado, inclusao e
 * remocao de competencia com aviso, competencia repetida, desativacao, visao somente consulta do
 * Solicitante e telas sem rolagem horizontal em celular, tablet e desktop.
 */
class ProfessoresNavegadorTest extends NavegadorBase {

    @Autowired
    ProfessorRepository professores;

    @Test
    void administradorCadastraGerenciaCompetenciasEDesativa() {
        navegador.usarTela(Tela.DESKTOP);
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        navegador.clicarLink("Cadastrar professor");
        navegador.preencher("nome", "Paula E2E");
        navegador.preencher("email", "paula.e2e@exemplo.test");
        navegador.clicarBotao("Salvar");
        navegador.esperarTexto("Cadastro salvo com sucesso.");

        navegador.clicarLink("Cadastrar professor");
        navegador.preencher("nome", "Outra Paula");
        navegador.preencher("email", "PAULA.E2E@exemplo.test");
        navegador.clicarBotao("Salvar");
        navegador.esperarTexto("Este e-mail já está cadastrado.");
        navegador.clicarLink("Cancelar");

        navegador.clicarNaLinha("Paula E2E", "Competências");
        navegador.esperarTexto("Nenhuma competência cadastrada.");
        navegador.preencher("competencia", "Química");
        navegador.clicarBotao("Incluir competência");
        navegador.esperarTexto("Competência incluída.");
        assertThat(navegador.textoDaPagina()).contains("química");

        navegador.preencher("competencia", "QUÍMICA");
        navegador.clicarBotao("Incluir competência");
        navegador.esperarTexto("O professor já possui a competência");

        navegador.clicarBotao("Remover");
        navegador.esperarTexto("Competência removida.");
        navegador.esperarTexto("Nenhuma competência cadastrada.");

        navegador.clicarLink("Voltar para professores");
        navegador.clicarBotao("Desativar");
        navegador.esperarTexto("Professor desativado.");
        navegador.esperarTexto("Nenhum professor cadastrado.");
    }

    @Test
    void solicitanteSoConsulta() {
        professores.saveAndFlush(new Professor("Carlos E2E", "carlos.e2e@exemplo.test", Set.of()));
        navegador.entrarComo(SOLICITANTE_EMAIL, SENHA);

        navegador.esperarTexto("Carlos E2E");
        assertThat(navegador.textoDaPagina()).contains("Somente consulta");
        assertThat(navegador.existeLink("Cadastrar professor")).isFalse();
        assertThat(navegador.existeLink("Competências")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Tela.class)
    void telasDeProfessoresNaoCausamRolagemHorizontal(Tela tela) {
        Long id = professores.saveAndFlush(new Professor("Professora Responsiva", "responsiva.e2e@exemplo.test",
                Set.of())).getId();
        navegador.usarTela(tela);
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        for (String caminho : List.of("/professores", "/professores/novo", "/professores/" + id + "/competencias")) {
            navegador.ir(caminho);
            assertThat(navegador.bootstrapCarregado()).as("Bootstrap aplicado em %s (%s)", caminho, tela).isTrue();
            assertThat(navegador.larguraDoConteudo())
                    .as("a página %s não pode rolar na horizontal em %s", caminho, tela)
                    .isLessThanOrEqualTo(navegador.larguraDaJanela());
        }
    }
}
