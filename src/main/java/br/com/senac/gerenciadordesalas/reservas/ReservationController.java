package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ErroResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Reservas", description = "Criacao e consulta de reservas de salas, professores e materiais")
@SecurityRequirement(name = "sessao")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Sessao ausente ou expirada",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissao ou token CSRF invalido",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno, sem detalhes sensiveis",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class)))
})
@RestController
@RequestMapping("/api/reservas")
public class ReservationController {

    private final ReservationService service;
    private final ReservationManagementService managementService;

    public ReservationController(ReservationService service, ReservationManagementService managementService) {
        this.service = service;
        this.managementService = managementService;
    }

    @Operation(summary = "Criar reserva",
            description = "Exige SOLICITANTE, sessao e token CSRF. O dono da reserva e o usuario autenticado. "
                    + "Sala, professor (opcional) e materiais sao validados como ativos; recursos restritos "
                    + "seguem o fluxo de aprovacao (status inicial CRIADA).")
    @ApiResponse(responseCode = "201", description = "Reserva criada",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReservationDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "Dados invalidos (campos obrigatorios ou formato)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "404", description = "Solicitante, sala, professor ou material nao encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Material duplicado no pedido ou quantidade acima do saldo",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @SecurityRequirement(name = "csrf")
    @PostMapping
    public ResponseEntity<ReservationDto.Saida> criar(
            @RequestBody @Valid ReservationDto.Entrada entrada,
            @AuthenticationPrincipal UserDetails autenticado) {
        ReservationDto.Saida criada = service.criar(entrada, autenticado.getUsername());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @Operation(summary = "Alterar reserva criada",
            description = "Exige sessao e CSRF. Somente o dono, RESPONSAVEL ou ADMINISTRADOR pode alterar; "
                    + "a reserva precisa estar em CRIADA.")
    @PutMapping("/{id}")
    public ResponseEntity<ReservationDto.Saida> alterar(
            @PathVariable Long id,
            @RequestBody @Valid ReservationDto.Alteracao entrada,
            @AuthenticationPrincipal UserDetails autenticado) {
        return ResponseEntity.ok(managementService.alterar(id, entrada, autenticado.getUsername()));
    }

    @Operation(summary = "Cancelar reserva",
            description = "Exige sessao e CSRF. Cancela sem apagar o historico; reservas em uso ou finais nao podem ser canceladas.")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservationDto.Saida> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails autenticado) {
        return ResponseEntity.ok(managementService.cancelar(id, autenticado.getUsername()));
    }
}
