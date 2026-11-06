package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import br.com.senac.gerenciadordesalas.reservas.StatusReserva;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roteiro E2E da entrega RSV-03/UI-09/UI-10 (#25): o solicitante cria, altera e
 * cancela a própria reserva pela interface, sem apagar o registro histórico.
 */
class ReservasNavegadorTest extends NavegadorBase {

    @Autowired
    SalaRepository salas;

    @Autowired
    ReservationRepository reservas;

    @Test
    void solicitanteCriaAlteraECancelaReservaPreservandoHistorico() {
        salas.saveAndFlush(new Sala("Laboratório de Reservas", 30, "Bloco R"));
        navegador.entrarComo(SOLICITANTE_EMAIL, SENHA);

        navegador.ir("/reservas/nova");
        navegador.escolher("salaId", "Laboratório de Reservas");
        navegador.definirValor("inicio", "2099-10-10T08:00");
        navegador.definirValor("termino", "2099-10-10T10:00");
        navegador.preencher("finalidade", "Aula prática de qualidade");
        navegador.clicarBotao("Criar reserva");

        navegador.esperarTexto("Reserva criada e aguardando o fluxo de aprovação.");
        assertThat(reservas.findAll()).hasSize(1);
        assertThat(reservas.findAll().get(0).getStatus()).isEqualTo(StatusReserva.CRIADA);

        navegador.clicarLink("Alterar reserva");
        navegador.esperarTexto("Alterar reserva");
        navegador.preencher("finalidade", "Aula prática de qualidade - revisada");
        navegador.clicarBotao("Salvar alteração");
        navegador.esperarTexto("Reserva alterada com sucesso.");
        assertThat(navegador.textoDaPagina()).contains("Aula prática de qualidade - revisada");

        navegador.clicarLink("Cancelar reserva");
        navegador.esperarTexto("Confirmar cancelamento");
        navegador.clicarBotao("Confirmar cancelamento");
        navegador.esperarTexto("Reserva cancelada e preservada no histórico.");
        assertThat(navegador.textoDaPagina()).contains("CANCELADA");
        assertThat(reservas.findAll()).hasSize(1);
        assertThat(reservas.findAll().get(0).getStatus()).isEqualTo(StatusReserva.CANCELADA);
    }
}
