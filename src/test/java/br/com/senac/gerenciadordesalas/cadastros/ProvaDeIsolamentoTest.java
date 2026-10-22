package br.com.senac.gerenciadordesalas.cadastros;

import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critério de aceite "dados de um teste não contaminam outro": a mesma gravação, com
 * valores únicos, é repetida. Se uma repetição deixasse linhas no banco, a seguinte
 * falharia na verificação de banco vazio ou na constraint de unicidade.
 */
class ProvaDeIsolamentoTest extends IntegracaoPostgresBase {

    @RepeatedTest(3)
    void cadaRepeticaoComecaComBancoLimpoEPodeReusarOsMesmosDados() {
        jdbcTemplate.update("INSERT INTO salas (nome, capacidade, localizacao) "
                + "VALUES ('Sala Isolamento', 10, 'Bloco 1')");
        jdbcTemplate.update("INSERT INTO tb_materiais (nome, tipo, patrimonio, quantidade_total, localizacao) "
                + "VALUES ('Projetor', 'EQUIPAMENTO', 'PAT-ISOLAMENTO', 1, 'Sala 1')");

        assertThat(contar("salas")).isEqualTo(1);
        assertThat(contar("tb_materiais")).isEqualTo(1);
    }
}