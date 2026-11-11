package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Relatorios (ENT-03, #185) com PostgreSQL real: utilizacao de salas e materiais, lista de
 * reservas por periodo, recorte das horas no limite do periodo, validacao do filtro, estado vazio
 * e o que cada perfil pode ver.
 *
 * <p>As reservas sao gravadas direto por SQL para montar qualquer status e qualquer data, inclusive
 * no passado, sem passar pelas regras de criacao, que ja tem testes proprios.
 */
@Import(RelogioFixoTestConfig.class)
class RelatorioWebIntegrationTest extends IntegracaoPostgresBase {

    private static final String ANA = "ana.relatorio@exemplo.test";
    private static final String BETO = "beto.relatorio@exemplo.test";
    private static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);
    private static final String NOVEMBRO = "/relatorios?de=2026-11-01&ate=2026-11-30";

    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired MaterialRepository materiais;

    @BeforeEach
    void preparar() {
        Long ana = usuarios.save(new User("Ana Relatorio", ANA, "$2a$10$hashBcryptFalso", Role.SOLICITANTE)).getId();
        Long beto = usuarios.save(new User("Beto Relatorio", BETO, "$2a$10$hashBcryptFalso", Role.SOLICITANTE)).getId();
        Long salaA = salas.save(new Sala("Sala Relatorio A", 30, "Bloco R")).getId();
        Long salaB = salas.save(new Sala("Sala Relatorio B", 20, "Bloco R")).getId();
        Long projetor = materiais.save(new Material("Projetor Relatorio", "Projetor multimidia",
                MaterialTipo.EQUIPAMENTO, null, 5, "Almoxarifado", false)).getId();
        sincronizar();

        // Novembro: 2 h + 1 h efetivas na sala A; a cancelada na sala B nao conta como uso.
        Long r1 = reserva(ana, salaA, em(10, 10), em(10, 12), "APROVADA");
        jdbcTemplate.update("INSERT INTO tb_reservas_materiais (reserva_id, material_id, quantidade) VALUES (?, ?, ?)",
                r1, projetor, 2);
        reserva(beto, salaA, em(11, 8), em(11, 9), "CONCLUIDA");
        reserva(beto, salaB, em(12, 14), em(12, 16), "CANCELADA");
        // Atravessa a meia-noite de 31/10 para 01/11: so 1 h fica dentro de novembro.
        reserva(beto, salaB, OffsetDateTime.of(2026, 10, 31, 23, 0, 0, 0, BRASILIA), em(1, 1), "APROVADA");
        // Dezembro: fora do periodo.
        reserva(ana, salaB, OffsetDateTime.of(2026, 12, 5, 10, 0, 0, 0, BRASILIA),
                OffsetDateTime.of(2026, 12, 5, 11, 0, 0, 0, BRASILIA), "APROVADA");
    }

    @Test
    void responsavel_veUtilizacaoDeSalasMateriaisEReservasDoPeriodo() throws Exception {
        String html = pagina(NOVEMBRO, "rui.relatorio@exemplo.test", "RESPONSAVEL");

        assertThat(html).contains("Período: <strong>01/11/2026 a 30/11/2026</strong>",
                "Utilização de salas", "Sala Relatorio A", "3,0 h", "Sala Relatorio B", "1,0 h",
                "Materiais reservados", "Projetor Relatorio",
                "Reservas do período", "Ana Relatorio", "Beto Relatorio", "CANCELADA", "CONCLUIDA",
                "10/11/2026 10:00 às 12:00", "31/10/2026 23:00 às 01/11/2026 01:00");
        assertThat(html).contains("Total: <strong>4</strong>");
        assertThat(html).doesNotContain("05/12/2026");
    }

    @Test
    void administrador_tambemVeTodosOsRelatorios() throws Exception {
        String html = pagina(NOVEMBRO, "adm.relatorio@exemplo.test", "ADMINISTRADOR");

        assertThat(html).contains("Utilização de salas", "Materiais reservados", "Ana Relatorio", "Beto Relatorio");
    }

    @Test
    void solicitante_veApenasAsPropriasReservasESemRelatoriosDeUso() throws Exception {
        String html = pagina(NOVEMBRO, ANA, "SOLICITANTE");

        assertThat(html).contains("Você vê apenas as suas próprias reservas.", "Minhas reservas no período",
                "Ana Relatorio", "10/11/2026 10:00 às 12:00", "Total: <strong>1</strong>");
        assertThat(html).doesNotContain("Beto Relatorio", "Utilização de salas", "Materiais reservados",
                "Projetor Relatorio", "Sala Relatorio A</span>");
    }

    @Test
    void periodoSemReservas_mostraEstadoVazio() throws Exception {
        String html = pagina("/relatorios?de=2027-01-01&ate=2027-01-31", "rui.relatorio@exemplo.test", "RESPONSAVEL");

        assertThat(html).contains("Nenhuma sala foi utilizada no período.",
                "Nenhum material foi reservado no período.", "Nenhuma reserva no período.");
    }

    @Test
    void semFiltro_usaOMesAtualNoFusoDeBrasilia() throws Exception {
        // O relogio fixo marca 01/11/2026 00:00 UTC, que ainda e 31/10/2026 em Brasilia.
        String html = pagina("/relatorios", "rui.relatorio@exemplo.test", "RESPONSAVEL");

        assertThat(html).contains("value=\"2026-10-01\"", "value=\"2026-10-31\"",
                "Período: <strong>01/10/2026 a 31/10/2026</strong>", "31/10/2026 23:00 às 01/11/2026 01:00");
    }

    @Test
    void periodoInvertido_mostraErroEPreservaOsCampos() throws Exception {
        String html = pagina("/relatorios?de=2026-11-30&ate=2026-11-01", "rui.relatorio@exemplo.test", "RESPONSAVEL");

        assertThat(html).contains("Corrija o período:", "A data final deve ser igual ou posterior à data inicial.",
                "value=\"2026-11-30\"", "value=\"2026-11-01\"");
        assertThat(html).doesNotContain("Reservas do período", "Sala Relatorio A");
    }

    @Test
    void periodoLongoDemais_eRecusado() throws Exception {
        String html = pagina("/relatorios?de=2026-01-01&ate=2027-12-31", "rui.relatorio@exemplo.test", "RESPONSAVEL");

        assertThat(html).contains("O período pode ter no máximo 366 dias.");
    }

    @Test
    void datasVaziasOuInvalidas_saoApontadas() throws Exception {
        String html = pagina("/relatorios?de=abc&ate=", ANA, "SOLICITANTE");

        assertThat(html).contains("Data inicial inválida.", "Informe a data final.", "value=\"abc\"");
        assertThat(html).doesNotContain("Minhas reservas no período");
    }

    @Test
    void menuMostraRelatoriosParaQualquerPerfil() throws Exception {
        assertThat(pagina(NOVEMBRO, ANA, "SOLICITANTE")).contains("href=\"/relatorios\"");
    }

    @Test
    void visitante_eLevadoAoLogin() throws Exception {
        mockMvc.perform(get(NOVEMBRO).accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private String pagina(String caminho, String email, String perfil) throws Exception {
        return mockMvc.perform(get(caminho).with(user(email).roles(perfil)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private Long reserva(Long solicitante, Long sala, OffsetDateTime inicio, OffsetDateTime termino, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO tb_reservas (solicitante_id, sala_id, inicio, termino, status, finalidade)
                VALUES (?, ?, ?, ?, ?, 'Relatorio')
                RETURNING id
                """, Long.class, solicitante, sala, inicio, termino, status);
    }

    private static OffsetDateTime em(int diaDeNovembro, int hora) {
        return OffsetDateTime.of(2026, 11, diaDeNovembro, hora, 0, 0, 0, BRASILIA);
    }
}
