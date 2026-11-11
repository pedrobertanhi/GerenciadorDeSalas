package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.cadastros.NavegadorE2E.Tela;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.Competencia;
import br.com.senac.gerenciadordesalas.professor.CompetenciaRepository;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Capturas reais das telas da ENT-03 (#185) em celular, tablet e desktop, com dados de exemplo:
 * reservas, professores e competencias, relatorios com dados, com erro de periodo e vazio, e a visao
 * do Solicitante. As imagens ficam em target/capturas/ISSUE-185 e a CI publica a pasta como artefato.
 */
class CapturasDeTelaNavegadorTest extends NavegadorBase {

    private static final Path PASTA = Path.of("target", "capturas", "ISSUE-185");
    private static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);
    private static final String NOVEMBRO = "/relatorios?de=2026-11-01&ate=2026-11-30";

    @Autowired SalaRepository salas;
    @Autowired MaterialRepository materiais;
    @Autowired CompetenciaRepository competencias;
    @Autowired ProfessorRepository professores;

    @ParameterizedTest
    @EnumSource(Tela.class)
    void capturaAsTelasDaEntrega(Tela tela) throws Exception {
        Long professor = prepararDados();
        navegador.usarTela(tela);

        navegador.entrarComo(ADMIN_EMAIL, SENHA);
        Map<String, String> administrador = new LinkedHashMap<>();
        administrador.put("01-reservas", "/reservas");
        administrador.put("02-professores", "/professores");
        administrador.put("03-competencias", "/professores/" + professor + "/competencias");
        administrador.put("04-relatorios-administrador", NOVEMBRO);
        administrador.put("05-relatorios-periodo-invalido", "/relatorios?de=2026-11-30&ate=2026-11-01");
        administrador.put("06-relatorios-vazio", "/relatorios?de=2027-01-01&ate=2027-01-31");
        capturarTodas(tela, administrador);

        navegador.sair();
        navegador.entrarComo(SOLICITANTE_EMAIL, SENHA);
        Map<String, String> solicitante = new LinkedHashMap<>();
        solicitante.put("07-professores-solicitante", "/professores");
        solicitante.put("08-relatorios-solicitante", NOVEMBRO);
        capturarTodas(tela, solicitante);
    }

    private void capturarTodas(Tela tela, Map<String, String> telas) throws Exception {
        for (Map.Entry<String, String> item : telas.entrySet()) {
            navegador.ir(item.getValue());
            assertThat(navegador.bootstrapCarregado()).as("Bootstrap aplicado em %s", item.getValue()).isTrue();
            Path arquivo = PASTA.resolve(tela.name().toLowerCase(Locale.ROOT) + "-" + item.getKey() + ".png");
            navegador.capturar(arquivo);
            assertThat(Files.size(arquivo)).as("captura %s", arquivo).isPositive();
        }
    }

    private Long prepararDados() {
        Long laboratorio = salas.saveAndFlush(new Sala("Laboratório de Química", 30, "Bloco B, 2º andar")).getId();
        Long auditorio = salas.saveAndFlush(new Sala("Auditório", 120, "Bloco A, térreo")).getId();
        salas.saveAndFlush(new Sala("Sala 105", 40, "Bloco A, 1º andar"));
        Long projetor = materiais.saveAndFlush(new Material("Projetor", "Projetor multimídia",
                MaterialTipo.EQUIPAMENTO, "PAT-0185", 1, "Almoxarifado", false)).getId();
        Competencia quimica = competencias.saveAndFlush(new Competencia("química"));
        Competencia fisica = competencias.saveAndFlush(new Competencia("física"));
        Long professor = professores.saveAndFlush(new Professor("Paula Souza", "paula.souza@exemplo.test",
                Set.of(quimica, fisica))).getId();
        professores.saveAndFlush(new Professor("Carlos Lima", "carlos.lima@exemplo.test", Set.of(fisica)));

        Long aula = reserva(SOLICITANTE_EMAIL, laboratorio, 10, 8, 10, "APROVADA", "Aula prática de titulação");
        jdbcTemplate.update("INSERT INTO tb_reservas_materiais (reserva_id, material_id, quantidade) VALUES (?, ?, ?)",
                aula, projetor, 1);
        reserva(ADMIN_EMAIL, auditorio, 12, 19, 22, "CONCLUIDA", "Palestra de abertura");
        reserva(SOLICITANTE_EMAIL, auditorio, 18, 14, 16, "CANCELADA", "Ensaio da apresentação");
        reserva(SOLICITANTE_EMAIL, laboratorio, 24, 8, 12, "CRIADA", "Laboratório de pesquisa");
        return professor;
    }

    private Long reserva(String email, Long sala, int dia, int inicio, int fim, String status, String finalidade) {
        Long solicitante = jdbcTemplate.queryForObject("SELECT id FROM tb_usuarios WHERE email = ?", Long.class, email);
        return jdbcTemplate.queryForObject("""
                INSERT INTO tb_reservas (solicitante_id, sala_id, inicio, termino, status, finalidade)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, solicitante, sala,
                OffsetDateTime.of(2026, 11, dia, inicio, 0, 0, 0, BRASILIA),
                OffsetDateTime.of(2026, 11, dia, fim, 0, 0, 0, BRASILIA), status, finalidade);
    }
}
