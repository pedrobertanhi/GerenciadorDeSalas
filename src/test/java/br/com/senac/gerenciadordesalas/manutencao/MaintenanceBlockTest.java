package br.com.senac.gerenciadordesalas.manutencao;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceBlockTest {

    private static final Instant CRIACAO = Instant.parse("2026-11-01T12:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-11-03T11:00:00Z");
    private static final Instant DURANTE = Instant.parse("2026-11-03T15:00:00Z");
    private static final Instant FIM = Instant.parse("2026-11-03T21:00:00Z");

    private MaintenanceBlock novo() {
        return new MaintenanceBlock(TipoRecurso.SALA, 12L, "Troca do projetor", INICIO, FIM, CRIACAO);
    }

    private MaintenanceBlock concluido() {
        MaintenanceBlock bloqueio = novo();
        bloqueio.concluir(DURANTE);
        return bloqueio;
    }

    private MaintenanceBlock cancelado() {
        MaintenanceBlock bloqueio = novo();
        bloqueio.cancelar(DURANTE);
        return bloqueio;
    }

    @Test
    void novoBloqueioNasceAtivoSemEncerramento() {
        MaintenanceBlock bloqueio = novo();

        assertThat(bloqueio.getStatus()).isEqualTo(StatusBloqueio.ATIVO);
        assertThat(bloqueio.getEncerradoEm()).isNull();
        assertThat(bloqueio.getCriadoEm()).isEqualTo(CRIACAO);
        assertThat(bloqueio.getAtualizadoEm()).isEqualTo(CRIACAO);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -3600})
    void periodoComFimIgualOuAnteriorAoInicioEhRecusado(long segundosEmRelacaoAoInicio) {
        Instant fimInvalido = INICIO.plusSeconds(segundosEmRelacaoAoInicio);

        assertThatThrownBy(() ->
                new MaintenanceBlock(TipoRecurso.SALA, 12L, "Motivo", INICIO, fimInvalido, CRIACAO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atualizarBloqueioAtivoGravaNovosDados() {
        MaintenanceBlock bloqueio = novo();
        Instant novoFim = FIM.plusSeconds(7200);

        bloqueio.atualizar("Troca do projetor e do cabo", INICIO, novoFim, DURANTE);

        assertThat(bloqueio.getMotivo()).isEqualTo("Troca do projetor e do cabo");
        assertThat(bloqueio.getFim()).isEqualTo(novoFim);
        assertThat(bloqueio.getAtualizadoEm()).isEqualTo(DURANTE);
        assertThat(bloqueio.getStatus()).isEqualTo(StatusBloqueio.ATIVO);
    }

    @Test
    void atualizarComPeriodoInvalidoEhRecusado() {
        MaintenanceBlock bloqueio = novo();

        assertThatThrownBy(() -> bloqueio.atualizar("Motivo", FIM, INICIO, DURANTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelarBloqueioAtivoRegistraOEncerramento() {
        MaintenanceBlock bloqueio = novo();

        bloqueio.cancelar(DURANTE);

        assertThat(bloqueio.getStatus()).isEqualTo(StatusBloqueio.CANCELADO);
        assertThat(bloqueio.getEncerradoEm()).isEqualTo(DURANTE);
    }

    @Test
    void concluirBloqueioQueJaComecouRegistraOEncerramento() {
        MaintenanceBlock bloqueio = novo();

        bloqueio.concluir(DURANTE);

        assertThat(bloqueio.getStatus()).isEqualTo(StatusBloqueio.CONCLUIDO);
        assertThat(bloqueio.getEncerradoEm()).isEqualTo(DURANTE);
    }

    @Test
    void concluirBloqueioQueAindaNaoComecouEhRecusado() {
        MaintenanceBlock bloqueio = novo();

        assertThatThrownBy(() -> bloqueio.concluir(INICIO.minusSeconds(1)))
                .isInstanceOf(ConflitoDeEstadoException.class);
        assertThat(bloqueio.getStatus()).isEqualTo(StatusBloqueio.ATIVO);
    }

    @Test
    void bloqueioConcluidoNaoPermiteMaisNenhumaAlteracao() {
        MaintenanceBlock bloqueio = concluido();

        assertThatThrownBy(() -> bloqueio.atualizar("Outro", INICIO, FIM, FIM))
                .isInstanceOf(ConflitoDeEstadoException.class);
        assertThatThrownBy(() -> bloqueio.cancelar(FIM)).isInstanceOf(ConflitoDeEstadoException.class);
        assertThatThrownBy(() -> bloqueio.concluir(FIM)).isInstanceOf(ConflitoDeEstadoException.class);
        assertThat(bloqueio.getStatus()).isEqualTo(StatusBloqueio.CONCLUIDO);
        assertThat(bloqueio.getEncerradoEm()).isEqualTo(DURANTE);
    }

    @Test
    void bloqueioCanceladoNaoPermiteMaisNenhumaAlteracao() {
        MaintenanceBlock bloqueio = cancelado();

        assertThatThrownBy(() -> bloqueio.atualizar("Outro", INICIO, FIM, FIM))
                .isInstanceOf(ConflitoDeEstadoException.class);
        assertThatThrownBy(() -> bloqueio.cancelar(FIM)).isInstanceOf(ConflitoDeEstadoException.class);
        assertThatThrownBy(() -> bloqueio.concluir(FIM)).isInstanceOf(ConflitoDeEstadoException.class);
        assertThat(bloqueio.getStatus()).isEqualTo(StatusBloqueio.CANCELADO);
    }

    @Test
    void bloqueioAtivoImpedeOPeriodoSobreposto() {
        MaintenanceBlock bloqueio = novo();

        assertThat(bloqueio.bloqueiaPeriodo(DURANTE, DURANTE.plusSeconds(3600))).isTrue();
        assertThat(bloqueio.bloqueiaPeriodo(INICIO.minusSeconds(3600), INICIO.plusSeconds(60))).isTrue();
    }

    @Test
    void periodosQueApenasSeTocamNaoSeSobrepoem() {
        MaintenanceBlock bloqueio = novo();

        assertThat(bloqueio.bloqueiaPeriodo(INICIO.minusSeconds(3600), INICIO)).isFalse();
        assertThat(bloqueio.bloqueiaPeriodo(FIM, FIM.plusSeconds(3600))).isFalse();
    }

    @Test
    void bloqueioCanceladoNaoImpedeOUsoDoRecurso() {
        assertThat(cancelado().bloqueiaPeriodo(DURANTE, DURANTE.plusSeconds(3600))).isFalse();
    }

    @Test
    void bloqueioConcluidoLiberaORecursoAPartirDaConclusao() {
        MaintenanceBlock bloqueio = concluido();

        assertThat(bloqueio.fimEfetivo()).isEqualTo(DURANTE);
        assertThat(bloqueio.bloqueiaPeriodo(INICIO, DURANTE)).isTrue();
        assertThat(bloqueio.bloqueiaPeriodo(DURANTE, DURANTE.plusSeconds(3600))).isFalse();
    }

    @Test
    void bloqueioConcluidoDepoisDoFimPrevistoMantemOFimOriginal() {
        MaintenanceBlock bloqueio = novo();

        bloqueio.concluir(FIM.plusSeconds(3600));

        assertThat(bloqueio.fimEfetivo()).isEqualTo(FIM);
    }
}