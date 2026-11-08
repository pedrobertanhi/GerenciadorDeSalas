package br.com.senac.gerenciadordesalas.aprovacao;

import br.com.senac.gerenciadordesalas.erro.ErroResposta;
import br.com.senac.gerenciadordesalas.reservas.ReservationDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Aprovacoes", description = "Fila de decisoes do Responsavel sobre reservas")
@SecurityRequirement(name = "sessao")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Sessao ausente ou expirada",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "403", description = "Somente o perfil RESPONSAVEL decide, ou token CSRF invalido",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "404", description = "Reserva nao encontrada",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "409", description = "Decisao nao esta pendente, decisao repetida ou conflito "
                + "surgido depois da solicitacao (manutencao, sobreposicao, recurso inativo ou sem saldo)",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno, sem detalhes sensiveis",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class)))
})
@RestController
@RequestMapping("/api/reservas")
public class DecisaoReservaController {

    private final AprovacaoService service;

    public DecisaoReservaController(AprovacaoService service) {
        this.service = service;
    }

    @Operation(summary = "Aprovar recurso restrito da reserva",
            description = "Exige RESPONSAVEL, sessao e CSRF. Revalida recursos ativos, manutencao e sobreposicao "
                    + "antes de aprovar. A reserva passa para APROVADA quando nao resta outra pendencia.")
    @ApiResponse(responseCode = "200", description = "Recurso aprovado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReservationDto.Saida.class)))
    @SecurityRequirement(name = "csrf")
    @PostMapping("/{id}/aprovar")
    public ResponseEntity<ReservationDto.Saida> aprovar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails autenticado) {
        return ResponseEntity.ok(service.aprovar(id, autenticado.getUsername()));
    }

    @Operation(summary = "Rejeitar recurso restrito da reserva",
            description = "Exige RESPONSAVEL, sessao e CSRF. O motivo e obrigatorio e fica preservado; "
                    + "a reserva passa para REJEITADA.")
    @ApiResponse(responseCode = "200", description = "Reserva rejeitada",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReservationDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "Motivo ausente, em branco ou com mais de 500 caracteres",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @SecurityRequirement(name = "csrf")
    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<ReservationDto.Saida> rejeitar(
            @PathVariable Long id,
            @RequestBody @Valid AprovacaoDto.Rejeicao corpo,
            @AuthenticationPrincipal UserDetails autenticado) {
        return ResponseEntity.ok(service.rejeitar(id, corpo.motivo(), autenticado.getUsername()));
    }
}
