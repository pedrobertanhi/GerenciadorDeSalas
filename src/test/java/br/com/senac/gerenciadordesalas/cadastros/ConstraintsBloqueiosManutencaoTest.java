package br.com.senac.gerenciadordesalas.cadastros;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Prova, direto no PostgreSQL, que a migration V6 barra bloqueios de manutenção inválidos.
 * Cada caso de erro confere o nome da constraint que disparou. Os casos válidos garantem que
 * o SQL de teste, por si só, está correto e registram a política adotada: bloqueios
 * sobrepostos no mesmo recurso são aceitos.
 */
class ConstraintsBloqueiosManutencaoTest extends IntegracaoPostgresBase {

    private static final String BLOQUEIO = "INSERT INTO tb_bloqueios_manutencao "
            + "(recurso_tipo, recurso_id, motivo, inicio, fim, status, encerrado_em) VALUES ";
    private static final String INICIO = "'2099-01-01T08:00:00-03:00'";
    private static final String FIM = "'2099-01-01T18:00:00-03:00'";
    private static final String ATIVO_VALIDO =
            BLOQUEIO + "('SALA', 12, 'Troca do projetor', " + INICIO + ", " + FIM + ", 'ATIVO', NULL)";

    private static final String[] NADA = {};

    static Stream<Arguments> casosQueDevemFalhar() {
        return Stream.of(
                arguments("tipo de recurso invalido",
                        BLOQUEIO + "('OUTRO', 12, 'Troca', " + INICIO + ", " + FIM + ", 'ATIVO', NULL)",
                        "\"ck_bloqueios_recurso_tipo\""),
                arguments("recurso_id zero",
                        BLOQUEIO + "('SALA', 0, 'Troca', " + INICIO + ", " + FIM + ", 'ATIVO', NULL)",
                        "\"ck_bloqueios_recurso_id\""),
                arguments("motivo em branco",
                        BLOQUEIO + "('SALA', 12, '   ', " + INICIO + ", " + FIM + ", 'ATIVO', NULL)",
                        "\"ck_bloqueios_motivo\""),
                arguments("fim igual ao inicio",
                        BLOQUEIO + "('SALA', 12, 'Troca', " + INICIO + ", " + INICIO + ", 'ATIVO', NULL)",
                        "\"ck_bloqueios_periodo\""),
                arguments("fim antes do inicio",
                        BLOQUEIO + "('SALA', 12, 'Troca', " + FIM + ", " + INICIO + ", 'ATIVO', NULL)",
                        "\"ck_bloqueios_periodo\""),
                arguments("status invalido",
                        BLOQUEIO + "('SALA', 12, 'Troca', " + INICIO + ", " + FIM + ", 'ARQUIVADO', " + INICIO + ")",
                        "\"ck_bloqueios_status\""),
                arguments("ATIVO com encerrado_em preenchido",
                        BLOQUEIO + "('SALA', 12, 'Troca', " + INICIO + ", " + FIM + ", 'ATIVO', " + INICIO + ")",
                        "\"ck_bloqueios_encerramento\""),
                arguments("CONCLUIDO sem encerrado_em",
                        BLOQUEIO + "('SALA', 12, 'Troca', " + INICIO + ", " + FIM + ", 'CONCLUIDO', NULL)",
                        "\"ck_bloqueios_encerramento\""),
                arguments("CANCELADO sem encerrado_em",
                        BLOQUEIO + "('SALA', 12, 'Troca', " + INICIO + ", " + FIM + ", 'CANCELADO', NULL)",
                        "\"ck_bloqueios_encerramento\""),
                arguments("motivo nulo",
                        "INSERT INTO tb_bloqueios_manutencao (recurso_tipo, recurso_id, inicio, fim) VALUES "
                                + "('SALA', 12, " + INICIO + ", " + FIM + ")",
                        "null value in column \"motivo\"")
        );
    }

    static Stream<Arguments> casosValidos() {
        return Stream.of(
                arguments("ativo sem encerramento", new String[]{ATIVO_VALIDO}),
                arguments("concluido com encerramento",
                        new String[]{BLOQUEIO + "('MATERIAL', 3, 'Revisao', " + INICIO + ", " + FIM
                                + ", 'CONCLUIDO', " + INICIO + ")"}),
                arguments("cancelado com encerramento",
                        new String[]{BLOQUEIO + "('PROFESSOR', 5, 'Licenca', " + INICIO + ", " + FIM
                                + ", 'CANCELADO', " + INICIO + ")"}),
                arguments("dois bloqueios sobrepostos no mesmo recurso sao aceitos",
                        new String[]{ATIVO_VALIDO, ATIVO_VALIDO})
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosQueDevemFalhar")
    void bancoRecusaBloqueioInvalido(String descricao, String comandoInvalido, String esperado) {
        assertThatThrownBy(() -> jdbcTemplate.update(comandoInvalido))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(esperado);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosValidos")
    void bancoAceitaBloqueioValido(String descricao, String[] comandos) {
        for (String comando : comandos) {
            jdbcTemplate.update(comando);
        }

        assertThat(contar("tb_bloqueios_manutencao")).isEqualTo(comandos.length);
    }

    @org.junit.jupiter.api.Test
    void valoresPadraoSaoAtivoEVersaoZero() {
        jdbcTemplate.update("INSERT INTO tb_bloqueios_manutencao (recurso_tipo, recurso_id, motivo, inicio, fim) "
                + "VALUES ('SALA', 12, 'Troca', " + INICIO + ", " + FIM + ")");

        assertThat(consultar("SELECT status FROM tb_bloqueios_manutencao", String.class)).isEqualTo("ATIVO");
        assertThat(consultar("SELECT versao FROM tb_bloqueios_manutencao", Long.class)).isZero();
    }
}