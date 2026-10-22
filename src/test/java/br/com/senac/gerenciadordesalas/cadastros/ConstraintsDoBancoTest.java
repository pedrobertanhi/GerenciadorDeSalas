package br.com.senac.gerenciadordesalas.cadastros;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Prova, direto no PostgreSQL e sem passar pela aplicação, que as constraints das
 * migrations V3, V4 e V5 realmente barram dado inválido. Cada caso de erro confere o
 * nome da constraint que disparou, para o teste não passar por outro motivo.
 * Os casos válidos garantem que o SQL de teste, por si só, está correto.
 */
class ConstraintsDoBancoTest extends IntegracaoPostgresBase {

    private static final String SALA = "INSERT INTO salas (nome, capacidade, localizacao) VALUES ";
    private static final String PROFESSOR = "INSERT INTO tb_professores (nome, email) VALUES ";
    private static final String COMPETENCIA = "INSERT INTO tb_competencias (nome) VALUES ";
    private static final String MATERIAL =
            "INSERT INTO tb_materiais (nome, tipo, patrimonio, quantidade_total, localizacao) VALUES ";
    private static final String VINCULO_ANA_JAVA =
            "INSERT INTO tb_professor_competencias (professor_id, competencia_id) "
                    + "SELECT p.id, c.id FROM tb_professores p, tb_competencias c "
                    + "WHERE p.email = 'ana@example.test' AND c.nome = 'java'";

    private static final String[] NADA = {};

    static Stream<Arguments> casosQueDevemFalhar() {
        return Stream.of(
                arguments("sala: nome duplicado",
                        new String[]{SALA + "('Sala A', 30, 'Bloco 1')"},
                        SALA + "('Sala A', 20, 'Bloco 2')",
                        "\"salas_nome_key\""),
                arguments("sala: capacidade zero", NADA,
                        SALA + "('Sala B', 0, 'Bloco 1')",
                        "\"salas_capacidade_check\""),
                arguments("sala: capacidade negativa", NADA,
                        SALA + "('Sala B', -5, 'Bloco 1')",
                        "\"salas_capacidade_check\""),
                arguments("sala: localizacao nula", NADA,
                        "INSERT INTO salas (nome, capacidade) VALUES ('Sala C', 10)",
                        "null value in column \"localizacao\""),
                arguments("sala: nome nulo", NADA,
                        "INSERT INTO salas (capacidade, localizacao) VALUES (10, 'Bloco 1')",
                        "null value in column \"nome\""),

                arguments("professor: e-mail duplicado",
                        new String[]{PROFESSOR + "('Ana', 'ana@example.test')"},
                        PROFESSOR + "('Outra Ana', 'ana@example.test')",
                        "\"uq_professores_email\""),
                arguments("professor: e-mail em maiusculas", NADA,
                        PROFESSOR + "('Ana', 'ANA@EXAMPLE.TEST')",
                        "\"ck_professores_email_minusculo\""),
                arguments("professor: nome em branco", NADA,
                        PROFESSOR + "('   ', 'branco@example.test')",
                        "\"ck_professores_nome\""),
                arguments("professor: e-mail sem formato valido", NADA,
                        PROFESSOR + "('Ana', 'semarroba')",
                        "\"ck_professores_email\""),

                arguments("competencia: nome duplicado",
                        new String[]{COMPETENCIA + "('java')"},
                        COMPETENCIA + "('java')",
                        "\"uq_competencias_nome\""),
                arguments("competencia: nome em branco", NADA,
                        COMPETENCIA + "('   ')",
                        "\"ck_competencias_nome\""),

                arguments("vinculo: professor inexistente",
                        new String[]{PROFESSOR + "('Ana', 'ana@example.test')", COMPETENCIA + "('java')"},
                        "INSERT INTO tb_professor_competencias (professor_id, competencia_id) "
                                + "SELECT 999999, id FROM tb_competencias WHERE nome = 'java'",
                        "\"tb_professor_competencias_professor_id_fkey\""),
                arguments("vinculo: competencia inexistente",
                        new String[]{PROFESSOR + "('Ana', 'ana@example.test')"},
                        "INSERT INTO tb_professor_competencias (professor_id, competencia_id) "
                                + "SELECT id, 999999 FROM tb_professores WHERE email = 'ana@example.test'",
                        "\"tb_professor_competencias_competencia_id_fkey\""),
                arguments("vinculo: associacao duplicada",
                        new String[]{PROFESSOR + "('Ana', 'ana@example.test')", COMPETENCIA + "('java')",
                                VINCULO_ANA_JAVA},
                        VINCULO_ANA_JAVA,
                        "\"tb_professor_competencias_pkey\""),

                arguments("material: tipo invalido", NADA,
                        MATERIAL + "('Projetor', 'OUTRO', NULL, 1, 'Sala 1')",
                        "\"ck_materiais_tipo\""),
                arguments("material: quantidade negativa", NADA,
                        MATERIAL + "('Projetor', 'EQUIPAMENTO', NULL, -1, 'Sala 1')",
                        "\"ck_materiais_quantidade\""),
                arguments("material: patrimonio em minusculas", NADA,
                        MATERIAL + "('Projetor', 'EQUIPAMENTO', 'pat-01', 1, 'Sala 1')",
                        "\"ck_materiais_patrimonio\""),
                arguments("material: patrimonio em branco", NADA,
                        MATERIAL + "('Projetor', 'EQUIPAMENTO', '   ', 1, 'Sala 1')",
                        "\"ck_materiais_patrimonio\""),
                arguments("material: patrimonio duplicado",
                        new String[]{MATERIAL + "('Projetor', 'EQUIPAMENTO', 'PAT-01', 1, 'Sala 1')"},
                        MATERIAL + "('Notebook', 'EQUIPAMENTO', 'PAT-01', 1, 'Sala 2')",
                        "\"uq_materiais_patrimonio\""),
                arguments("material: localizacao em branco", NADA,
                        MATERIAL + "('Projetor', 'EQUIPAMENTO', NULL, 1, '   ')",
                        "\"ck_materiais_localizacao\""),
                arguments("material: nome em branco", NADA,
                        MATERIAL + "('   ', 'EQUIPAMENTO', NULL, 1, 'Sala 1')",
                        "\"ck_materiais_nome\"")
        );
    }

    static Stream<Arguments> casosValidos() {
        return Stream.of(
                arguments("sala valida",
                        new String[]{SALA + "('Sala A', 30, 'Bloco 1')"}),
                arguments("professor, competencia e vinculo validos",
                        new String[]{PROFESSOR + "('Ana', 'ana@example.test')", COMPETENCIA + "('java')",
                                VINCULO_ANA_JAVA}),
                arguments("materiais sem patrimonio podem se repetir",
                        new String[]{MATERIAL + "('Cabo', 'MATERIAL', NULL, 10, 'Sala 1')",
                                MATERIAL + "('Caneta', 'MATERIAL', NULL, 5, 'Sala 1')"}),
                arguments("material com patrimonio em maiusculas e quantidade zero",
                        new String[]{MATERIAL + "('Projetor', 'EQUIPAMENTO', 'PAT-01', 0, 'Sala 1')"})
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosQueDevemFalhar")
    void bancoRecusaDadoInvalido(String descricao, String[] preparo, String comandoInvalido, String esperado) {
        for (String comando : preparo) {
            jdbcTemplate.update(comando);
        }

        assertThatThrownBy(() -> jdbcTemplate.update(comandoInvalido))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(esperado);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosValidos")
    void bancoAceitaDadoValido(String descricao, String[] comandos) {
        for (String comando : comandos) {
            jdbcTemplate.update(comando);
        }
    }
}