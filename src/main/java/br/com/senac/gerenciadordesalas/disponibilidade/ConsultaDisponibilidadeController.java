package br.com.senac.gerenciadordesalas.disponibilidade;

import br.com.senac.gerenciadordesalas.erro.ErroResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Disponibilidade",
        description = "Consulta de disponibilidade de recursos em um intervalo. "
                + "Enquanto a integracao com reservas e manutencao nao e concluida, "
                + "o endpoint responde 503 com codigo CONSULTA_INDISPONIVEL.")
@SecurityRequirement(name = "sessao")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Sessao ausente ou expirada",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissao",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno, sem detalhes sensiveis",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class)))
})
@RestController
@RequestMapping("/api/disponibilidade")
@PreAuthorize("isAuthenticated()")
public class ConsultaDisponibilidadeController {

    @Operation(summary = "Consultar disponibilidade de recurso em um intervalo",
            description = "Qualquer usuario autenticado. Enquanto a integracao com reservas e manutencao "
                    + "nao e concluida, a resposta e sempre 503 com codigo CONSULTA_INDISPONIVEL. "
                    + "Intervalo invalido retorna 400 com codigo INTERVALO_INVALIDO.")
    @ApiResponse(responseCode = "503", description = "Consulta temporariamente indisponivel ate a integracao",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ConsultaDisponibilidadeResposta.class)))
    @ApiResponse(responseCode = "400", description = "Intervalo invalido (inicio e termino em ISO 8601 com deslocamento, termino posterior ao inicio)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ConsultaDisponibilidadeResposta.class)))
    @GetMapping("/consulta")
    public ResponseEntity<ConsultaDisponibilidadeResposta> consultar(
            @Parameter(description = "Inicio em ISO 8601 com deslocamento, por exemplo 2026-11-10T09:00:00-03:00")
            @RequestParam(required = false) String inicio,
            @Parameter(description = "Termino em ISO 8601 com deslocamento, posterior ao inicio")
            @RequestParam(required = false) String termino) {
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