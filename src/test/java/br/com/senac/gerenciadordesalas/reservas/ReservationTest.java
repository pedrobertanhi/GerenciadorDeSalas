package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.usuario.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Regras do agregado {@link Reservation} (RSV-01, #23):
 * criacao, periodo, transicao de status e manipulacao de materiais.
 */
class ReservationTest {

    private final User solicitante = Mockito.mock(User.class);
    private final Sala sala = Mockito.mock(Sala.class);
    private final Professor professor = Mockito.mock(Professor.class);
    private final Material material = Mockito.mock(Material.class);
    private final Instant inicio = Instant.parse("2026-11-10T09:00:00Z");
    private final Instant termino = inicio.plus(2, ChronoUnit.HOURS);

    private Reservation novaReserva() {
        return new Reservation(solicitante, sala, professor, inicio, termino);
    }

    @Test
    void aoCriar_statusInicialEhCRIADA() {
        Reservation r = novaReserva();
        assertThat(r.getStatus()).isEqualTo(StatusReserva.CRIADA);
        assertThat(r.getMateriais()).isEmpty();
    }

    @Test
    void professorEhOpcional() {
        Reservation r = new Reservation(solicitante, sala, null, inicio, termino);
        assertThat(r.getProfessor()).isNull();
    }

    @Test
    void solicitanteSalaInicioETerminoSaoObrigatorios() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Reservation(null, sala, professor, inicio, termino));
        assertThatNullPointerException()
                .isThrownBy(() -> new Reservation(solicitante, null, professor, inicio, termino));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(solicitante, sala, professor, null, termino));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(solicitante, sala, professor, inicio, null));
    }

    @Test
    void periodoInvertidoOuZeradoEhRejeitado() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(solicitante, sala, professor, termino, inicio));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(solicitante, sala, professor, inicio, inicio));
    }

    @Test
    void transicionarPara_valida_aplicaMudanca() {
        Reservation r = novaReserva();
        r.transicionarPara(StatusReserva.APROVADA);
        assertThat(r.getStatus()).isEqualTo(StatusReserva.APROVADA);
        r.transicionarPara(StatusReserva.EM_ANDAMENTO);
        r.transicionarPara(StatusReserva.CONCLUIDA);
        assertThat(r.getStatus()).isEqualTo(StatusReserva.CONCLUIDA);
    }

    @Test
    void transicionarPara_invalida_lancaIllegalState() {
        Reservation r = novaReserva();
        assertThatIllegalStateException()
                .isThrownBy(() -> r.transicionarPara(StatusReserva.CONCLUIDA));
    }

    @Test
    void aposStatusFinal_naoTransicionaMais() {
        Reservation r = novaReserva();
        r.transicionarPara(StatusReserva.CANCELADA);
        assertThatIllegalStateException()
                .isThrownBy(() -> r.transicionarPara(StatusReserva.APROVADA));
    }

    @Test
    void adicionarMaterial_registraItemComQuantidade() {
        Reservation r = novaReserva();
        ReservationMaterial item = r.adicionarMaterial(material, 3);
        assertThat(r.getMateriais()).containsExactly(item);
        assertThat(item.getQuantidade()).isEqualTo(3);
        assertThat(item.getReserva()).isSameAs(r);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    void quantidadeInvalidaEhRejeitada(int quantidadeRuim) {
        Reservation r = novaReserva();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> r.adicionarMaterial(material, quantidadeRuim));
    }

    @Test
    void removerMaterial_desassociaDaReserva() {
        Reservation r = novaReserva();
        ReservationMaterial item = r.adicionarMaterial(material, 2);
        r.removerMaterial(item);
        assertThat(r.getMateriais()).isEmpty();
        assertThat(item.getReserva()).isNull();
    }

    @Test
    void materiaisRetornadosSaoImutaveis() {
        Reservation r = novaReserva();
        assertThat(r.getMateriais()).isUnmodifiable();
    }

    @Test
    void definirPeriodo_atualizaInicioETerminoEnquantoCRIADA() {
        Reservation r = novaReserva();
        Instant novoInicio = inicio.plus(1, ChronoUnit.DAYS);
        Instant novoTermino = termino.plus(1, ChronoUnit.DAYS);
        r.definirPeriodo(novoInicio, novoTermino);
        assertThat(r.getInicio()).isEqualTo(novoInicio);
        assertThat(r.getTermino()).isEqualTo(novoTermino);
    }

    @Test
    void definirPeriodo_rejeitadoAposTransicaoDeStatus() {
        Reservation r = novaReserva();
        r.transicionarPara(StatusReserva.APROVADA);
        assertThatIllegalStateException()
                .isThrownBy(() -> r.definirPeriodo(
                        inicio.plus(1, ChronoUnit.DAYS),
                        termino.plus(1, ChronoUnit.DAYS)));
    }

    @Test
    void adicionarMaterial_rejeitaDuplicata() {
        Reservation r = novaReserva();
        r.adicionarMaterial(material, 2);
        assertThatIllegalStateException()
                .isThrownBy(() -> r.adicionarMaterial(material, 1));
        assertThat(r.getMateriais()).hasSize(1);
    }
}
