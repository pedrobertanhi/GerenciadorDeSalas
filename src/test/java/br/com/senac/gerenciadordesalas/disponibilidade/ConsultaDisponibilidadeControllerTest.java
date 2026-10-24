package br.com.senac.gerenciadordesalas.disponibilidade;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;

class ConsultaDisponibilidadeControllerTest {

    private final ConsultaDisponibilidadeController controller = new ConsultaDisponibilidadeController();

    @Test
    void intervaloInvalidoRetorna400() {
        var resposta = controller.consultar("2026-10-22T10:00:00Z", "2026-10-22T10:00:00Z");
        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("INTERVALO_INVALIDO", resposta.getBody().codigo());
    }

    @Test
    void dataSemDeslocamentoRetorna400() {
        var resposta = controller.consultar("2026-10-22T10:00:00", "2026-10-22T11:00:00Z");
        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
    }

    @Test
    void parametrosAusentesRetornamMesmoContrato() {
        var semInicio = controller.consultar(null, "2026-10-22T11:00:00Z");
        var semTermino = controller.consultar("2026-10-22T10:00:00Z", null);
        assertEquals(HttpStatus.BAD_REQUEST, semInicio.getStatusCode());
        assertEquals("INTERVALO_INVALIDO", semInicio.getBody().codigo());
        assertEquals(HttpStatus.BAD_REQUEST, semTermino.getStatusCode());
        assertEquals("INTERVALO_INVALIDO", semTermino.getBody().codigo());
    }

    @Test
    void dataExtremaRetorna400() {
        var resposta = controller.consultar(
                "+999999999-12-31T23:59:59-18:00",
                "+999999999-12-31T23:59:59Z");
        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("INTERVALO_INVALIDO", resposta.getBody().codigo());
    }

    @Test
    void consultaAindaNaoIntegradaNaoDeclaraDisponibilidade() {
        var resposta = controller.consultar("2026-10-22T10:00:00Z", "2026-10-22T11:00:00Z");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resposta.getStatusCode());
        assertEquals("CONSULTA_INDISPONIVEL", resposta.getBody().codigo());
        assertEquals(OffsetDateTime.parse("2026-10-22T07:00:00-03:00"), resposta.getBody().inicio());
        assertEquals(OffsetDateTime.parse("2026-10-22T08:00:00-03:00"), resposta.getBody().termino());
    }
}
