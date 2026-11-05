package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlock;
import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlockRepository;
import br.com.senac.gerenciadordesalas.manutencao.StatusBloqueio;
import br.com.senac.gerenciadordesalas.manutencao.TipoRecurso;
import br.com.senac.gerenciadordesalas.reservas.Reservation;
import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import br.com.senac.gerenciadordesalas.reservas.StatusReserva;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxos da tela de manutenção em navegador real (Issue #15): o administrador registra, edita,
 * conclui e cancela bloqueios pela interface, e o histórico permanece consultável.
 */
class ManutencaoNavegadorTest extends NavegadorBase {

    @Autowired
    SalaRepository salas;

    @Autowired
    MaintenanceBlockRepository blocos;

    @Autowired
    ReservationRepository reservas;

    @Test
    void administradorRegistraEditaEConcluiUmBloqueioEmAndamento() {
        salas.save(new Sala("Sala Manut", 30, "Bloco M"));
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        registrarBloqueio("Troca do projetor", "2020-01-01T08:00", "2099-12-31T18:00");

        navegador.esperarTexto("Bloqueio registrado.");
        assertThat(navegador.textoDaPagina())
                .contains("Impacto sobre a disponibilidade")
                .contains("consta como indisponível por manutenção")
                .contains("Nenhuma reserva ativa neste período.");
        assertThat(blocos.buscar(null, null, null)).hasSize(1);

        navegador.clicarLink("Editar");
        navegador.esperarCaminho("/editar");
        navegador.preencher("motivo", "Troca do projetor e do cabo");
        navegador.clicarBotao("Salvar");
        navegador.esperarTexto("Bloqueio atualizado.");
        assertThat(navegador.textoDaPagina()).contains("Troca do projetor e do cabo");

        navegador.clicarBotao("Concluir manutenção");
        navegador.esperarTexto("Bloqueio concluído.");
        assertThat(navegador.textoDaPagina()).contains("Concluído").contains("Encerrado em");
        assertThat(navegador.existeBotao("Concluir manutenção")).isFalse();
        assertThat(navegador.existeBotao("Cancelar bloqueio")).isFalse();
        assertThat(navegador.existeLink("Editar")).isFalse();

        // histórico preservado: o bloqueio concluído continua na agenda e no banco
        navegador.ir("/manutencoes");
        assertThat(navegador.textoDaCelula("Sala · Sala Manut", 4)).isEqualTo("Concluído");
        assertThat(blocos.buscar(null, null, null)).hasSize(1);
        assertThat(blocos.buscar(null, null, null).get(0).getStatus()).isEqualTo(StatusBloqueio.CONCLUIDO);
    }

    @Test
    void cancelamentoMantemOBloqueioNoHistoricoESemAcoes() {
        salas.save(new Sala("Sala Manut", 30, "Bloco M"));
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        registrarBloqueio("Revisão elétrica", "2099-01-10T08:00", "2099-01-10T18:00");

        navegador.esperarTexto("Bloqueio registrado.");
        assertThat(navegador.existeBotao("Concluir manutenção")).as("ainda não começou").isFalse();

        navegador.clicarBotao("Cancelar bloqueio");
        navegador.esperarTexto("Bloqueio cancelado.");
        assertThat(navegador.textoDaPagina()).contains("não afeta a disponibilidade");
        assertThat(navegador.existeBotao("Cancelar bloqueio")).isFalse();

        navegador.ir("/manutencoes");
        assertThat(navegador.textoDaCelula("Sala · Sala Manut", 4)).isEqualTo("Cancelado");
        assertThat(blocos.buscar(null, null, null)).hasSize(1);
        assertThat(blocos.buscar(null, null, null).get(0).getStatus()).isEqualTo(StatusBloqueio.CANCELADO);
    }

    @Test
    void impactoContaAReservaAtivaDoPeriodoSemCancelaLa() {
        Sala sala = salas.save(new Sala("Sala Manut", 30, "Bloco M"));
        blocos.saveAndFlush(new MaintenanceBlock(TipoRecurso.SALA, sala.getId(), "Pintura",
                Instant.parse("2020-01-01T11:00:00Z"), Instant.parse("2099-12-31T21:00:00Z"), Instant.now()));
        User solicitante = usuarios.findByEmail(SOLICITANTE_EMAIL).orElseThrow();
        Reservation reserva = reservas.saveAndFlush(new Reservation(solicitante, sala, null,
                Instant.parse("2099-01-10T11:00:00Z"), Instant.parse("2099-01-10T13:00:00Z"), "Aula de laboratorio"));
        navegador.entrarComo(ADMIN_EMAIL, SENHA);

        navegador.ir("/manutencoes/" + blocos.buscar(null, null, null).get(0).getId());

        navegador.esperarTexto("Impacto sobre a disponibilidade");
        assertThat(navegador.textoDaPagina())
                .contains("reserva(s) ativa(s) neste período")
                .contains("não são canceladas nem alteradas automaticamente");
        assertThat(reservas.findById(reserva.getId()).orElseThrow().getStatus()).isEqualTo(StatusReserva.CRIADA);
    }

    private void registrarBloqueio(String motivo, String inicio, String fim) {
        navegador.ir("/manutencoes/novo");
        navegador.escolher("recurso", "Sala Manut");
        navegador.preencher("motivo", motivo);
        navegador.definirValor("inicio", inicio);
        navegador.definirValor("fim", fim);
        navegador.clicarBotao("Salvar");
    }
}