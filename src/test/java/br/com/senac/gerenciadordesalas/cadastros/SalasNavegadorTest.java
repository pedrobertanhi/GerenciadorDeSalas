package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CRUD de salas em navegador real (Issue #15): o administrador cadastra, edita e inativa pela
 * interface, e as validações do navegador e do servidor aparecem para o usuário.
 */
class SalasNavegadorTest extends NavegadorBase {

    @Autowired
    SalaRepository salas;

    @Test
    void administradorCadastraEditaEInativaUmaSalaPelaInterface() {
        navegador.entrarComo(ADMIN_EMAIL, SENHA);
        navegador.ir("/salas");
        navegador.clicarLink("Cadastrar sala");
        navegador.esperarCaminho("/salas/novo");

        navegador.preencher("nome", "Sala E2E 1");
        navegador.preencher("capacidade", "45");
        navegador.preencher("localizacao", "Bloco E2E");
        navegador.clicarBotao("Salvar");

        navegador.esperarTexto("Sala cadastrada com sucesso.");
        assertThat(navegador.textoDaPagina()).contains("Sala E2E 1").contains("Bloco E2E");
        assertThat(navegador.textoDaCelula("Sala E2E 1", 2)).isEqualTo("45");
        assertThat(salas.findAll()).hasSize(1);

        navegador.clicarNaLinha("Sala E2E 1", "Editar");
        navegador.esperarCaminho("/editar");
        navegador.preencher("capacidade", "60");
        navegador.clicarBotao("Salvar");

        navegador.esperarTexto("Sala atualizada com sucesso.");
        assertThat(navegador.textoDaCelula("Sala E2E 1", 2)).isEqualTo("60");
        assertThat(salas.findAll().get(0).getCapacidade()).isEqualTo(60);

        navegador.clicarNaLinha("Sala E2E 1", "Inativar");
        navegador.esperarTexto("Confirmar inativação");
        navegador.clicarBotao("Confirmar inativação");

        navegador.esperarTexto("Sala inativada. O histórico de reservas foi preservado.");
        assertThat(navegador.textoDaPagina()).doesNotContain("Sala E2E 1");
        assertThat(salas.findAll()).hasSize(1);
        assertThat(salas.findAll().get(0).getAtiva()).isFalse();
    }

    @Test
    void validacoesDoNavegadorEDoServidorAparecemNoFormularioDeSalas() {
        salas.save(new Sala("Sala Duplicada", 20, "Bloco D"));
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        // o navegador barra campos obrigatórios vazios, sem enviar o formulário
        navegador.ir("/salas/novo");
        navegador.clicarBotao("Salvar");
        assertThat(navegador.campoValido("nome")).isFalse();
        assertThat(navegador.campoValido("capacidade")).isFalse();
        assertThat(navegador.campoValido("localizacao")).isFalse();
        assertThat(navegador.caminhoAtual()).isEqualTo("/salas/novo");

        // capacidade acima do limite do Integer: quem responde é a validação do servidor
        enviarFormulario("Sala Grande", "2147483648", "Bloco G");
        navegador.esperarTexto("A capacidade informada é grande demais");

        // espaços em branco passam pelo navegador, mas não pelo servidor
        enviarFormulario("Sala Branca", "10", "   ");
        navegador.esperarTexto("A localização é obrigatória.");

        // nome já usado por outra sala
        enviarFormulario("Sala Duplicada", "10", "Bloco X");
        navegador.esperarTexto("Já existe uma sala com este nome.");

        assertThat(salas.findAll()).hasSize(1);
    }

    private void enviarFormulario(String nome, String capacidade, String localizacao) {
        navegador.ir("/salas/novo");
        navegador.preencher("nome", nome);
        navegador.preencher("capacidade", capacidade);
        navegador.preencher("localizacao", localizacao);
        navegador.clicarBotao("Salvar");
    }
}