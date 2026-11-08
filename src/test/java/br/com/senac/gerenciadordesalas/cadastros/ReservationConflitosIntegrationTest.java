package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.reservas.ReservationDto;
import br.com.senac.gerenciadordesalas.reservas.ReservationService;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Exclusao mutua entre reservas simultaneas (RSV-10, #27), com PostgreSQL real.
 *
 * Roteiro deterministico de cada teste:
 * <ol>
 *   <li>a reserva A e criada dentro de uma transacao que fica aberta (sem commit);</li>
 *   <li>a reserva B, do mesmo recurso, comeca em outra thread e precisa ficar
 *       parada esperando a trava do banco;</li>
 *   <li>A faz commit; B segue, enxerga A e e recusada com conflito;</li>
 *   <li>o banco termina com exatamente uma reserva.</li>
 * </ol>
 * Se a trava for removida, B nao espera: termina antes do commit de A, sem
 * enxergar a reserva dela, e grava uma segunda reserva. O teste falha nesse caso.
 *
 * Diferente dos outros testes de integracao, aqui cada operacao precisa de commit
 * de verdade, por isso a transacao de teste e desligada e os dados sao apagados
 * no fim de cada teste.
 */
@Import(RelogioFixoTestConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservationConcorrenciaIntegrationTest extends IntegracaoPostgresBase {

    private static final String EMAIL = "solicitante.concorrencia@exemplo.test";
    private static final OffsetDateTime INICIO = OffsetDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TERMINO = INICIO.plusHours(2);

    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired ProfessorRepository professores;
    @Autowired MaterialRepository materiais;
    @Autowired ReservationService reservationService;
    @Autowired PlatformTransactionManager transactionManager;

    private Long salaA;
    private Long salaB;
    private Long professorId;
    private Long patrimonioId;

    @BeforeEach
    void preparar() {
        usuarios.save(new User("Ana Concorrencia", EMAIL, "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        salaA = salas.save(new Sala("Sala Concorrencia A", 30, "Bloco A")).getId();
        salaB = salas.save(new Sala("Sala Concorrencia B", 30, "Bloco B")).getId();
        professorId = professores.save(new Professor(
                "Carlos Concorrencia", "carlos.concorrencia@exemplo.test", Set.of())).getId();
        patrimonioId = materiais.save(new Material(
                "Notebook", "Notebook patrimoniado", MaterialTipo.EQUIPAMENTO, "PAT-0002",
                1, "Almoxarifado", false)).getId();
    }

    @AfterEach
    void limpar() {
        jdbcTemplate.update("DELETE FROM tb_reservas_materiais");
        jdbcTemplate.update("DELETE FROM tb_reservas");
        jdbcTemplate.update("DELETE FROM tb_materiais");
        jdbcTemplate.update("DELETE FROM tb_professor_competencias");
        jdbcTemplate.update("DELETE FROM tb_professores");
        jdbcTemplate.update("DELETE FROM salas");
        jdbcTemplate.update("DELETE FROM tb_usuarios WHERE email = ?", EMAIL);
    }

    /** Sem transacao de teste, cada operacao ja foi comitada: nao ha nada pendente no Hibernate. */
    @Override
    protected void sincronizar() {
    }

    @Test
    void mesmaSala_segundaReservaEsperaAPrimeiraEDepoisRecebeConflito() throws Exception {
        segundaEsperaAPrimeiraEDepoisRecebeConflito(
                entrada(salaA, null, List.of()),
                entrada(salaA, null, List.of()),
                "Sala ja possui reserva");
    }

    @Test
    void mesmoProfessorEmSalasDiferentes_segundaReservaEsperaAPrimeiraEDepoisRecebeConflito() throws Exception {
        segundaEsperaAPrimeiraEDepoisRecebeConflito(
                entrada(salaA, professorId, List.of()),
                entrada(salaB, professorId, List.of()),
                "Professor ja possui reserva");
    }

    @Test
    void mesmoPatrimonioEmSalasDiferentes_segundaReservaEsperaAPrimeiraEDepoisRecebeConflito() throws Exception {
        List<ReservationDto.MaterialPedido> notebook = List.of(new ReservationDto.MaterialPedido(patrimonioId, 1));
        segundaEsperaAPrimeiraEDepoisRecebeConflito(
                entrada(salaA, null, notebook),
                entrada(salaB, null, notebook),
                "nao possui quantidade disponivel");
    }

    private void segundaEsperaAPrimeiraEDepoisRecebeConflito(ReservationDto.Entrada primeira,
                                                             ReservationDto.Entrada segunda,
                                                             String mensagemEsperada) throws Exception {
        TransactionTemplate transacao = new TransactionTemplate(transactionManager);
        CountDownLatch primeiraGravou = new CountDownLatch(1);
        CountDownLatch liberarCommitDaPrimeira = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = executor.submit(() -> transacao.executeWithoutResult(status -> {
                reservationService.criar(primeira, EMAIL);
                primeiraGravou.countDown();
                aguardar(liberarCommitDaPrimeira);
            }));
            assertThat(primeiraGravou.await(15, SECONDS))
                    .as("a primeira reserva devia ser gravada (ainda sem commit)")
                    .isTrue();

            Future<ReservationDto.Saida> b = executor.submit(() -> reservationService.criar(segunda, EMAIL));

            assertThat(catchThrowable(() -> b.get(3, SECONDS)))
                    .as("a segunda reserva devia ficar esperando a trava enquanto a primeira nao faz commit")
                    .isInstanceOf(TimeoutException.class);

            liberarCommitDaPrimeira.countDown();
            a.get(15, SECONDS);

            Throwable erro = catchThrowable(() -> b.get(15, SECONDS));
            assertThat(erro)
                    .as("a segunda reserva devia ser recusada depois do commit da primeira")
                    .isInstanceOf(ExecutionException.class);
            assertThat(erro.getCause())
                    .isInstanceOf(ConflitoDeEstadoException.class)
                    .hasMessageContaining(mensagemEsperada);

            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_reservas", Integer.class))
                    .as("o banco deve terminar com exatamente uma reserva")
                    .isEqualTo(1);
        } finally {
            liberarCommitDaPrimeira.countDown();
            executor.shutdownNow();
            executor.awaitTermination(15, SECONDS);
        }
    }

    private ReservationDto.Entrada entrada(Long salaId, Long professor,
                                           List<ReservationDto.MaterialPedido> pedidos) {
        return new ReservationDto.Entrada(salaId, professor, INICIO, TERMINO, "Aula concorrente", pedidos);
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(15, SECONDS)) {
                throw new IllegalStateException("tempo esgotado esperando a liberacao do teste");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}