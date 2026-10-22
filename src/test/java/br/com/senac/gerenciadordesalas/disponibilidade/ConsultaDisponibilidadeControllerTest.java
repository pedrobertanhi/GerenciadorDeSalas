package br.com.senac.gerenciadordesalas.disponibilidade;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
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
    void consultaAindaNaoIntegradaNaoDeclaraDisponibilidade() {
        var resposta = controller.consultar("2026-10-22T10:00:00Z", "2026-10-22T11:00:00Z");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resposta.getStatusCode());
        assertEquals("CONSULTA_INDISPONIVEL", resposta.getBody().codigo());
        assertNotNull(resposta.getBody().inicio());
        assertNotNull(resposta.getBody().termino());
    }
}
