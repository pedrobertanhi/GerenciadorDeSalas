package br.com.senac.gerenciadordesalas.disponibilidade;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato inicial da RES-06. Nao afirma disponibilidade antes de
 * integrar os conflitos de reservas e periodos de manutencao.
 */
@RestController
@RequestMapping("/api/disponibilidade")
@PreAuthorize("isAuthenticated()")
public class ConsultaDisponibilidadeController {

    @GetMapping("/consulta")
    public ResponseEntity<ConsultaDisponibilidadeResposta> consultar(
            @RequestParam(required = false) String inicio, @RequestParam(required = false) String termino) {
        IntervaloDisponibilidade intervalo;
        try {
            intervalo = IntervaloDisponibilidade.de(inicio, termino);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ConsultaDisponibilidadeResposta(
                    "INTERVALO_INVALIDO", ex.getMessage(), null, null));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                new ConsultaDisponibilidadeResposta(
                        "CONSULTA_INDISPONIVEL",
                        "A verificacao de reservas e manutencao ainda nao foi integrada.",
                        ConsultaDisponibilidadeResposta.noFusoLocal(intervalo.inicio()),
                        ConsultaDisponibilidadeResposta.noFusoLocal(intervalo.termino())));
    }
}
