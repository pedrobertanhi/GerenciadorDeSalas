package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.cadastros.NavegadorE2E.Tela;
import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlock;
import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlockRepository;
import br.com.senac.gerenciadordesalas.manutencao.TipoRecurso;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validação de período e motivo, autorização por perfil e responsividade da tela de manutenção em
 * navegador real (Issue #15).
 */
class ManutencaoValidacaoAutorizacaoResponsividadeNavegadorTest extends NavegadorBase {

    @Autowired
    SalaRepository salas;

    @Autowired
    MaintenanceBlockRepository blocos;

    private Long salaComBloqueioEmAndamento() {
        Sala sala = salas.save(new Sala("Sala Manut", 30, "Bloco M"));
        return blocos.saveAndFlush(new MaintenanceBlock(TipoRecurso.SALA, sala.getId(), "Troca do projetor",
                Instant.parse("2020-01-01T11:00:00Z"), Instant.parse("2099-12-31T21:00:00Z"), Instant.now())).getId();
    }

    @Test
    void periodoEMotivoInvalidosMostramMensagemCompreensivelENaoGravam() {
        salas.save(new Sala("Sala Manut", 30, "Bloco M"));
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        // o navegador barra os campos obrigatórios vazios, sem enviar o formulário
        navegador.ir("/manutencoes/novo");
        navegador.clicarBotao("Salvar");
        assertThat(navegador.campoValido("recurso")).isFalse();
        assertThat(navegador.campoValido("motivo")).isFalse();
        assertThat(navegador.caminhoAtual()).isEqualTo("/manutencoes/novo");

        enviarFormulario("Troca", "2099-01-10T18:00", "2099-01-10T08:00");
        navegador.esperarTexto("O fim deve ser posterior ao início.");

        enviarFormulario("Troca", "2020-01-01T08:00", "2020-01-02T08:00");
        navegador.esperarTexto("O fim deve estar no futuro.");

        enviarFormulario("   ", "2099-01-10T08:00", "2099-01-10T18:00");
        navegador.esperarTexto("O motivo é obrigatório.");

        assertThat(blocos.buscar(null, null, null)).isEmpty();
    }

    @Test
    void solicitanteConsultaAManutencaoMasNaoGerenciaBloqueios() {
        Long id = salaComBloqueioEmAndamento();
        navegador.entrarComo(SOLICITANTE_EMAIL, SENHA);

        navegador.ir("/manutencoes");
        navegador.esperarTexto("Manutenção de recursos");
        assertThat(navegador.existeLink("Registrar bloqueio")).isFalse();
        assertThat(navegador.textoDaPagina()).contains("Sala · Sala Manut");

        navegador.ir("/manutencoes/" + id);
        navegador.esperarTexto("Impacto sobre a disponibilidade");
        assertThat(navegador.existeBotao("Concluir manutenção")).isFalse();
        assertThat(navegador.existeBotao("Cancelar bloqueio")).isFalse();
        assertThat(navegador.existeLink("Editar")).isFalse();

        navegador.ir("/manutencoes/novo");
        navegador.esperarTexto("ACESSO_NEGADO");
        navegador.ir("/manutencoes/" + id + "/editar");
        navegador.esperarTexto("ACESSO_NEGADO");
    }

    @Test
    void visitanteSemLoginVaiParaATelaDeLogin() {
        navegador.ir("/manutencoes");

        navegador.esperarCaminho("/login");
        assertThat(navegador.textoDaPagina()).contains("Entrar no sistema");
    }

    @ParameterizedTest
    @EnumSource(Tela.class)
    void telasDeManutencaoSeAdaptamAoTamanhoDaJanela(Tela tela) {
        Long id = salaComBloqueioEmAndamento();
        navegador.usarTela(tela);
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        for (String caminho : List.of("/manutencoes", "/manutencoes/" + id, "/manutencoes/novo")) {
            navegador.ir(caminho);
            assertThat(navegador.bootstrapCarregado())
                    .as("Bootstrap aplicado em %s (%s)", caminho, tela).isTrue();
            assertThat(navegador.larguraDoConteudo())
                    .as("a página %s não pode rolar na horizontal em %s", caminho, tela)
                    .isLessThanOrEqualTo(navegador.larguraDaJanela());
        }

        navegador.ir("/manutencoes");
        assertThat(navegador.rolagemHorizontalDe(".table-responsive")).isEqualTo("auto");
        long janela = navegador.larguraDaJanela();
        long campoDoFiltro = navegador.larguraDoCampo("tipo");
        if (tela == Tela.CELULAR) {
            assertThat(campoDoFiltro).as("no celular o filtro ocupa a linha inteira").isGreaterThan(janela * 6 / 10);
        } else {
            assertThat(campoDoFiltro).as("em telas largas os campos do filtro ficam lado a lado").isLessThan(janela / 2);
        }

        navegador.ir("/manutencoes/novo");
        for (String campo : List.of("recurso", "motivo", "inicio", "fim")) {
            assertThat(navegador.campoDentroDaJanela(campo)).as("campo %s dentro da janela em %s", campo, tela).isTrue();
        }
        assertThat(navegador.botaoDentroDaJanela("Salvar")).isTrue();
    }

    private void enviarFormulario(String motivo, String inicio, String fim) {
        navegador.ir("/manutencoes/novo");
        navegador.escolher("recurso", "Sala Manut");
        navegador.preencher("motivo", motivo);
        navegador.definirValor("inicio", inicio);
        navegador.definirValor("fim", fim);
        navegador.clicarBotao("Salvar");
    }
}