package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Constraints de banco do agregado Reservation (RSV-01, #23):
 * valida que o PostgreSQL rejeita, mesmo quando a validacao do dominio Java
 * eh contornada por inserts diretos, periodo invertido/igual, quantidade de
 * material zero ou negativa e o mesmo material duplicado na mesma reserva.
 */
class ReservationConstraintsTest extends IntegracaoPostgresBase {

    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired MaterialRepository materiais;

    private Long solicitanteId;
    private Long salaId;
    private Long materialId;
    private final Instant inicio = Instant.parse("2026-11-10T09:00:00Z");
    private final Instant termino = inicio.plus(2, ChronoUnit.HOURS);

    @BeforeEach
    void preparar() {
        User solicitante = usuarios.save(new User(
                "Ana Souza", "ana@exemplo.test", "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        Sala sala = salas.save(new Sala("Sala 101", 30, "Bloco A - 1o andar"));
        Material projetor = materiais.save(new Material(
                "Projetor", "Projetor HDMI", MaterialTipo.EQUIPAMENTO, "P-0001",
                5, "Almoxarifado", false));
        sincronizar();
        this.solicitanteId = solicitante.getId();
        this.salaId = sala.getId();
        this.materialId = projetor.getId();
    }

    @Test
    void periodoInvertido_eRejeitadoPorCheckDoBanco() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO tb_reservas (solicitante_id, sala_id, inicio, termino, status) "
                        + "VALUES (?, ?, ?, ?, 'CRIADA')",
                solicitanteId, salaId,
                Timestamp.from(termino),
                Timestamp.from(inicio)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_reservas_periodo");
    }

    @Test
    void periodoIgual_eRejeitadoPorCheckDoBanco() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO tb_reservas (solicitante_id, sala_id, inicio, termino, status) "
                        + "VALUES (?, ?, ?, ?, 'CRIADA')",
                solicitanteId, salaId,
                Timestamp.from(inicio),
                Timestamp.from(inicio)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_reservas_periodo");
    }

    @Test
    void quantidadeDeMaterialZero_eRejeitadaPorCheckDoBanco() {
        Long reservaId = inserirReservaValida();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO tb_reservas_materiais (reserva_id, material_id, quantidade) "
                        + "VALUES (?, ?, ?)",
                reservaId, materialId, 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_reservas_materiais_quantidade");
    }

    @Test
    void quantidadeDeMaterialNegativa_eRejeitadaPorCheckDoBanco() {
        Long reservaId = inserirReservaValida();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO tb_reservas_materiais (reserva_id, material_id, quantidade) "
                        + "VALUES (?, ?, ?)",
                reservaId, materialId, -1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_reservas_materiais_quantidade");
    }

    @Test
    void materialDuplicadoNaMesmaReserva_eRejeitadoPorUniqueDoBanco() {
        Long reservaId = inserirReservaValida();
        jdbcTemplate.update(
                "INSERT INTO tb_reservas_materiais (reserva_id, material_id, quantidade) "
                        + "VALUES (?, ?, ?)",
                reservaId, materialId, 1);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO tb_reservas_materiais (reserva_id, material_id, quantidade) "
                        + "VALUES (?, ?, ?)",
                reservaId, materialId, 2))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_reservas_materiais_reserva_material");
    }

    private Long inserirReservaValida() {
        jdbcTemplate.update(
                "INSERT INTO tb_reservas (solicitante_id, sala_id, inicio, termino, status) "
                        + "VALUES (?, ?, ?, ?, 'CRIADA')",
                solicitanteId, salaId,
                Timestamp.from(inicio),
                Timestamp.from(termino));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tb_reservas ORDER BY id DESC LIMIT 1", Long.class);
    }
}