package br.com.senac.gerenciadordesalas.aprovacao;

import br.com.senac.gerenciadordesalas.erro.ErroResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Aprovacoes", description = "Fila de decisoes do Responsavel sobre reservas")
@SecurityRequirement(name = "sessao")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Sessao ausente ou expirada",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "403", description = "Somente o perfil RESPONSAVEL acessa a fila",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno, sem detalhes sensiveis",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class)))
})
@RestController
@RequestMapping("/api/aprovacoes")
public class AprovacaoController {

    private final AprovacaoService service;

    public AprovacaoController(AprovacaoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar reservas pendentes de decisao",
            description = "Exige RESPONSAVEL. Retorna reservas CRIADA com aprovacao de recurso restrito "
                    + "ou validacao docente pendente, da solicitacao mais antiga para a mais recente, "
                    + "com solicitante, periodo, sala, professor e materiais.")
    @ApiResponse(responseCode = "200", description = "Fila de pendencias",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AprovacaoDto.ItemFila.class))))
    @GetMapping
    public List<AprovacaoDto.ItemFila> listarPendentes() {
        return service.listarPendentes();
    }
}
