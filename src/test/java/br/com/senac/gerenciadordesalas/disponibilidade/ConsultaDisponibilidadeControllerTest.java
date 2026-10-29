package br.com.senac.gerenciadordesalas.disponibilidade;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import java.util.List;
import org.mockito.Mockito;

class ConsultaDisponibilidadeControllerTest {

    private final ConsultaDisponibilidadeService service = Mockito.mock(ConsultaDisponibilidadeService.class);
    private final ConsultaDisponibilidadeController controller = new ConsultaDisponibilidadeController(service);

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
    void consultaIntegradaRetornaRecursosDisponiveis() {
        Mockito.when(service.consultar(Mockito.any())).thenReturn(
                new ConsultaDisponibilidadeService.RecursosDisponiveis(List.of(1L), List.of(2L), List.of(3L)));
        var resposta = controller.consultar("2026-10-22T10:00:00Z", "2026-10-22T11:00:00Z");
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("CONSULTA_OK", resposta.getBody().codigo());
        assertEquals(List.of(1L), resposta.getBody().salas());
        assertEquals(List.of(2L), resposta.getBody().professores());
        assertEquals(List.of(3L), resposta.getBody().materiais());
        assertEquals(OffsetDateTime.parse("2026-10-22T07:00:00-03:00"), resposta.getBody().inicio());
        assertEquals(OffsetDateTime.parse("2026-10-22T08:00:00-03:00"), resposta.getBody().termino());
    }
}
