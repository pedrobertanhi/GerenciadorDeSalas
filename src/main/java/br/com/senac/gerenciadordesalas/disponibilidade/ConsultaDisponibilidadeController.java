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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta recursos ativos e disponiveis considerando reservas e manutencao.
 */
@Tag(name = "Disponibilidade",
        description = "Consulta recursos ativos sem conflito de reserva ou manutencao.")
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

    private final ConsultaDisponibilidadeService service;

    public ConsultaDisponibilidadeController(ConsultaDisponibilidadeService service) {
        this.service = service;
    }

    @Operation(summary = "Consultar disponibilidade de recurso em um intervalo",
            description = "Qualquer usuario autenticado. Intervalo invalido retorna 400; "
                    + "consulta valida retorna IDs de recursos disponiveis.")
    @ApiResponse(responseCode = "200", description = "Recursos disponiveis no intervalo",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ConsultaDisponibilidadeResposta.class)))
    @ApiResponse(responseCode = "400", description = "Intervalo invalido (inicio e termino em ISO 8601 com deslocamento, termino posterior ao inicio)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ConsultaDisponibilidadeResposta.class)))
    @GetMapping("/consulta")
    public ResponseEntity<ConsultaDisponibilidadeResposta> consultar(
            @Parameter(description = "Inicio em ISO 8601 com deslocamento, por exemplo 2026-11-10T09:00:00-03:00")
            @RequestParam(required = true) String inicio,
            @Parameter(description = "Termino em ISO 8601 com deslocamento, posterior ao inicio")
            @RequestParam(required = true) String termino) {
        IntervaloDisponibilidade intervalo;
        try {
            intervalo = IntervaloDisponibilidade.de(inicio, termino);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ConsultaDisponibilidadeResposta(
                    "INTERVALO_INVALIDO", ex.getMessage(), null, null));
        }
        var disponiveis = service.consultar(intervalo);
        return ResponseEntity.ok(new ConsultaDisponibilidadeResposta(
                "CONSULTA_OK", "Recursos disponiveis no intervalo consultado.",
                ConsultaDisponibilidadeResposta.noFusoLocal(intervalo.inicio()),
                ConsultaDisponibilidadeResposta.noFusoLocal(intervalo.termino()),
                disponiveis.salas(), disponiveis.professores(), disponiveis.materiais()));
    }
}