package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.cadastros.NavegadorE2E.Tela;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roteiro E2E dos relatorios (ENT-03, #185): filtro de periodo pela tela, erro de periodo,
 * visao restrita do Solicitante e layout sem rolagem horizontal em celular, tablet e desktop.
 */
class RelatoriosNavegadorTest extends NavegadorBase {

    private static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);

    @Autowired
    SalaRepository salas;

    @BeforeEach
    void prepararReservas() {
        Long sala = salas.saveAndFlush(new Sala("Sala dos Relatórios", 25, "Bloco E")).getId();
        reserva(SOLICITANTE_EMAIL, sala, 10, "APROVADA");
        reserva(ADMIN_EMAIL, sala, 11, "CONCLUIDA");
    }

    @Test
    void administradorFiltraOPeriodoEVeAUtilizacao() {
        navegador.usarTela(Tela.DESKTOP);
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        navegador.clicarLink("Relatórios");
        navegador.esperarTexto("Data inicial");
        navegador.definirValor("de", "2026-11-01");
        navegador.definirValor("ate", "2026-11-30");
        navegador.clicarBotao("Gerar relatório");

        navegador.esperarTexto("01/11/2026 a 30/11/2026");
        assertThat(navegador.textoDaPagina()).contains("Utilização de salas", "Sala dos Relatórios", "4,0 h",
                "Solicitante E2E", "Administrador E2E");
    }

    @Test
    void periodoInvertidoMostraOErro() {
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        navegador.ir("/relatorios?de=2026-11-30&ate=2026-11-01");

        navegador.esperarTexto("A data final deve ser igual ou posterior à data inicial.");
    }

    @Test
    void solicitanteVeSoAsPropriasReservas() {
        navegador.entrarComo(SOLICITANTE_EMAIL, SENHA);

        navegador.ir("/relatorios?de=2026-11-01&ate=2026-11-30");

        navegador.esperarTexto("Minhas reservas no período");
        assertThat(navegador.textoDaPagina()).contains("Solicitante E2E")
                .doesNotContain("Administrador E2E", "Utilização de salas");
    }

    @ParameterizedTest
    @EnumSource(Tela.class)
    void relatorioNaoCausaRolagemHorizontal(Tela tela) {
        navegador.usarTela(tela);
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        navegador.ir("/relatorios?de=2026-11-01&ate=2026-11-30");

        navegador.esperarTexto("Utilização de salas");
        assertThat(navegador.bootstrapCarregado()).isTrue();
        assertThat(navegador.larguraDoConteudo())
                .as("o relatório não pode rolar na horizontal em %s", tela)
                .isLessThanOrEqualTo(navegador.larguraDaJanela());
    }

    /** Duas horas, das 8h as 10h no horario de Brasilia, no dia de novembro informado. */
    private void reserva(String email, Long sala, int dia, String status) {
        Long solicitante = jdbcTemplate.queryForObject("SELECT id FROM tb_usuarios WHERE email = ?", Long.class, email);
        jdbcTemplate.update("""
                INSERT INTO tb_reservas (solicitante_id, sala_id, inicio, termino, status, finalidade)
                VALUES (?, ?, ?, ?, ?, 'Relatorio E2E')
                """, solicitante, sala, OffsetDateTime.of(2026, 11, dia, 8, 0, 0, 0, BRASILIA),
                OffsetDateTime.of(2026, 11, dia, 10, 0, 0, 0, BRASILIA), status);
    }
}
