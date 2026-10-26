package br.com.senac.gerenciadordesalas.manutencao;

import br.com.senac.gerenciadordesalas.erro.ErroResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Manutenção", description = "Bloqueios de manutenção: indisponibilidade temporária de recursos")
@SecurityRequirement(name = "sessao")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Sessão ausente ou expirada",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissão ou token CSRF inválido",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno, sem detalhes sensíveis",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErroResposta.class)))
})
@RestController
@RequestMapping("/api/manutencoes")
public class MaintenanceBlockController {

    private final MaintenanceBlockService service;

    public MaintenanceBlockController(MaintenanceBlockService service) {
        this.service = service;
    }

    @Operation(summary = "Consultar bloqueios de manutenção",
            description = "Qualquer usuário autenticado. Inclui concluídos e cancelados, que permanecem como histórico. "
                    + "Filtros opcionais por tipo de recurso, recurso e estado.")
    @ApiResponse(responseCode = "200", description = "Lista de bloqueios, do mais recente ao mais antigo",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = BloqueioDto.Saida.class))))
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<BloqueioDto.Saida> listar(
            @Parameter(description = "SALA, PROFESSOR ou MATERIAL")
            @RequestParam(required = false) TipoRecurso recursoTipo,
            @Parameter(description = "Identificador do recurso")
            @RequestParam(required = false) Long recursoId,
            @Parameter(description = "ATIVO, CONCLUIDO ou CANCELADO")
            @RequestParam(required = false) StatusBloqueio status) {
        return service.listar(recursoTipo, recursoId, status);
    }

    @Operation(summary = "Consultar bloqueio por ID", description = "Qualquer usuário autenticado.")
    @ApiResponse(responseCode = "200", description = "Bloqueio encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BloqueioDto.Saida.class)))
    @ApiResponse(responseCode = "404", description = "Bloqueio não encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public BloqueioDto.Saida consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @Operation(summary = "Registrar bloqueio de manutenção",
            description = "Exige ADMINISTRADOR, sessão e token CSRF. O recurso deve existir, o fim deve ser "
                    + "posterior ao início e ainda estar no futuro.")
    @ApiResponse(responseCode = "201", description = "Bloqueio registrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BloqueioDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "Dados ou período inválidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public BloqueioDto.Saida criar(@Valid @RequestBody BloqueioDto.Entrada entrada) {
        return service.criar(entrada);
    }

    @Operation(summary = "Alterar bloqueio de manutenção",
            description = "Exige ADMINISTRADOR, sessão e token CSRF. Somente bloqueios ativos. "
                    + "O recurso de um bloqueio não pode ser trocado.")
    @ApiResponse(responseCode = "200", description = "Bloqueio alterado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BloqueioDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "Dados ou período inválidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "404", description = "Bloqueio não encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Bloqueio não está ativo",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public BloqueioDto.Saida alterar(@PathVariable Long id, @Valid @RequestBody BloqueioDto.Entrada entrada) {
        return service.atualizar(id, entrada);
    }

    @Operation(summary = "Concluir bloqueio de manutenção",
            description = "Exige ADMINISTRADOR e token CSRF. Somente bloqueios ativos que já começaram. "
                    + "O bloqueio permanece como histórico e libera o recurso a partir da conclusão.")
    @ApiResponse(responseCode = "200", description = "Bloqueio concluído",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BloqueioDto.Saida.class)))
    @ApiResponse(responseCode = "404", description = "Bloqueio não encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Bloqueio não está ativo ou ainda não começou",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public BloqueioDto.Saida concluir(@PathVariable Long id) {
        return service.concluir(id);
    }

    @Operation(summary = "Cancelar bloqueio de manutenção",
            description = "Exige ADMINISTRADOR e token CSRF. Somente bloqueios ativos. "
                    + "O bloqueio permanece como histórico e deixa de impedir o uso do recurso.")
    @ApiResponse(responseCode = "200", description = "Bloqueio cancelado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BloqueioDto.Saida.class)))
    @ApiResponse(responseCode = "404", description = "Bloqueio não encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Bloqueio não está ativo",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public BloqueioDto.Saida cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }
}