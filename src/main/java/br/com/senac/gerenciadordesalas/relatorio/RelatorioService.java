package br.com.senac.gerenciadordesalas.relatorio;

import br.com.senac.gerenciadordesalas.relatorio.RelatorioDto.LinhaReserva;
import br.com.senac.gerenciadordesalas.relatorio.RelatorioDto.Relatorio;
import br.com.senac.gerenciadordesalas.relatorio.RelatorioDto.TotalPorStatus;
import br.com.senac.gerenciadordesalas.relatorio.RelatorioDto.UsoMaterial;
import br.com.senac.gerenciadordesalas.relatorio.RelatorioDto.UsoSala;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static br.com.senac.gerenciadordesalas.relatorio.RelatorioDto.LIMITE_LINHAS;

/**
 * Relatorios de utilizacao de salas, materiais e reservas (ENT-03, #185; RF-12).
 *
 * <ul>
 *   <li>Uma reserva entra no periodo quando o intervalo dela cruza o periodo pedido.</li>
 *   <li>Utilizacao (salas e materiais) conta so reservas efetivas: APROVADA, EM_ANDAMENTO e
 *       CONCLUIDA. As horas sao recortadas ao periodo, entao uma reserva que atravessa a meia-noite
 *       do primeiro dia so conta a parte de dentro.</li>
 *   <li>A lista de reservas mostra todos os status, com totais por status.</li>
 *   <li>Responsavel e Administrador veem tudo. O Solicitante recebe so as proprias reservas e nao
 *       recebe os relatorios de salas e materiais, que juntam dados de outros solicitantes.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private static final String STATUS_EFETIVOS = "('APROVADA', 'EM_ANDAMENTO', 'CONCLUIDA')";
    private static final String PERIODO = "WITH p AS (SELECT CAST(? AS timestamptz) AS ini, CAST(? AS timestamptz) AS fim) ";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final String SQL_SALAS = PERIODO + """
            SELECT s.nome, s.localizacao, s.ativa,
                   COUNT(r.id) AS reservas,
                   COALESCE(SUM(EXTRACT(EPOCH FROM (LEAST(r.termino, p.fim) - GREATEST(r.inicio, p.ini)))), 0) AS segundos
              FROM salas s
              CROSS JOIN p
              LEFT JOIN tb_reservas r
                     ON r.sala_id = s.id
                    AND r.status IN """ + STATUS_EFETIVOS + """

                    AND r.inicio < p.fim
                    AND r.termino > p.ini
             WHERE s.ativa OR r.id IS NOT NULL
             GROUP BY s.id, s.nome, s.localizacao, s.ativa
             ORDER BY segundos DESC, s.nome
            """;

    private static final String SQL_MATERIAIS = PERIODO + """
            SELECT m.nome, m.tipo, m.patrimonio, m.restrito,
                   COUNT(DISTINCT r.id) AS reservas,
                   SUM(rm.quantidade) AS quantidade
              FROM tb_reservas_materiais rm
              JOIN tb_reservas r ON r.id = rm.reserva_id
              JOIN tb_materiais m ON m.id = rm.material_id
              CROSS JOIN p
             WHERE r.status IN """ + STATUS_EFETIVOS + """

               AND r.inicio < p.fim
               AND r.termino > p.ini
             GROUP BY m.id, m.nome, m.tipo, m.patrimonio, m.restrito
             ORDER BY quantidade DESC, m.nome
            """;

    private static final String FROM_RESERVAS = """
              FROM tb_reservas r
              JOIN tb_usuarios u ON u.id = r.solicitante_id
              JOIN salas s ON s.id = r.sala_id
              CROSS JOIN p
             WHERE r.inicio < p.fim
               AND r.termino > p.ini
            """;

    private final JdbcTemplate jdbc;

    public RelatorioService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Gera os relatorios do periodo.
     *
     * @param email       usuario conectado; filtra a lista quando ele nao e operacional
     * @param operacional {@code true} para Responsavel e Administrador
     */
    public Relatorio gerar(PeriodoRelatorio periodo, String email, boolean operacional) {
        Objects.requireNonNull(periodo, "periodo e obrigatorio");
        Objects.requireNonNull(email, "email e obrigatorio");
        OffsetDateTime ini = periodo.inicio();
        OffsetDateTime fim = periodo.fimExclusivo();

        List<UsoSala> salas = operacional ? usoDeSalas(ini, fim) : List.of();
        List<UsoMaterial> materiais = operacional ? usoDeMateriais(ini, fim) : List.of();
        List<LinhaReserva> reservas = reservas(ini, fim, email, operacional);
        List<TotalPorStatus> totais = totaisPorStatus(ini, fim, email, operacional);
        long total = totais.stream().mapToLong(TotalPorStatus::total).sum();
        boolean cortada = reservas.size() > LIMITE_LINHAS;

        return new Relatorio(periodo.de().format(DATA) + " a " + periodo.ate().format(DATA), operacional,
                salas, materiais, cortada ? reservas.subList(0, LIMITE_LINHAS) : reservas, totais, total, cortada);
    }

    private List<UsoSala> usoDeSalas(OffsetDateTime ini, OffsetDateTime fim) {
        return jdbc.query(SQL_SALAS, (rs, i) -> new UsoSala(
                rs.getString("nome"), rs.getString("localizacao"), rs.getBoolean("ativa"),
                rs.getLong("reservas"), rs.getDouble("segundos") / 3600.0), ini, fim);
    }

    private List<UsoMaterial> usoDeMateriais(OffsetDateTime ini, OffsetDateTime fim) {
        return jdbc.query(SQL_MATERIAIS, (rs, i) -> new UsoMaterial(
                rs.getString("nome"), rs.getString("tipo"), rs.getString("patrimonio"), rs.getBoolean("restrito"),
                rs.getLong("reservas"), rs.getLong("quantidade")), ini, fim);
    }

    private List<LinhaReserva> reservas(OffsetDateTime ini, OffsetDateTime fim, String email, boolean operacional) {
        String sql = PERIODO + "SELECT r.id, u.nome AS solicitante, s.nome AS sala, r.inicio, r.termino, r.status "
                + FROM_RESERVAS + filtroDoSolicitante(operacional)
                + " ORDER BY r.inicio, r.id LIMIT " + (LIMITE_LINHAS + 1);
        return jdbc.query(sql, (rs, i) -> new LinhaReserva(
                rs.getLong("id"), rs.getString("solicitante"), rs.getString("sala"),
                periodoDaReserva(rs.getObject("inicio", OffsetDateTime.class), rs.getObject("termino", OffsetDateTime.class)),
                rs.getString("status")), argumentos(ini, fim, email, operacional));
    }

    private List<TotalPorStatus> totaisPorStatus(OffsetDateTime ini, OffsetDateTime fim, String email,
                                                 boolean operacional) {
        String sql = PERIODO + "SELECT r.status, COUNT(*) AS total " + FROM_RESERVAS + filtroDoSolicitante(operacional)
                + " GROUP BY r.status ORDER BY r.status";
        return jdbc.query(sql, (rs, i) -> new TotalPorStatus(rs.getString("status"), rs.getLong("total")),
                argumentos(ini, fim, email, operacional));
    }

    private static String filtroDoSolicitante(boolean operacional) {
        return operacional ? "" : " AND u.email = ?";
    }

    private static Object[] argumentos(OffsetDateTime ini, OffsetDateTime fim, String email, boolean operacional) {
        List<Object> argumentos = new ArrayList<>(List.of(ini, fim));
        if (!operacional) {
            argumentos.add(email.trim().toLowerCase());
        }
        return argumentos.toArray();
    }

    private static String periodoDaReserva(OffsetDateTime inicio, OffsetDateTime termino) {
        var ini = inicio.atZoneSameInstant(PeriodoRelatorio.FUSO);
        var fim = termino.atZoneSameInstant(PeriodoRelatorio.FUSO);
        String termina = ini.toLocalDate().equals(fim.toLocalDate()) ? fim.format(HORA) : fim.format(DATA_HORA);
        return ini.format(DATA_HORA) + " às " + termina;
    }
}
