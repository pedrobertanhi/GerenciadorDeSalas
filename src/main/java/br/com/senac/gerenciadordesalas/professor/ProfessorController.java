package br.com.senac.gerenciadordesalas.professor;

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
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Professores", description = "Cadastro e consulta de professores e competências")
@SecurityRequirement(name = "sessao")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Sessão ausente ou expirada (Accept: application/json)",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão ou token CSRF ausente/inválido nas escritas",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
    @ApiResponse(responseCode = "500", description = "Erro interno, sem detalhes sensíveis",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
})
@RestController
@RequestMapping("/api/professores")
public class ProfessorController {
    private final ProfessorService service;
    public ProfessorController(ProfessorService service) { this.service = service; }
    @Operation(summary = "Listar professores ativos", description = "Qualquer usuário autenticado. Retorna somente ativos; filtro exato por competência, sem distinguir maiúsculas e após remover espaços nas extremidades. Filtro vazio equivale a ausência de filtro.")
    @ApiResponse(responseCode = "200", description = "Lista de professores ativos, possivelmente vazia",
        content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProfessorDto.Saida.class))))
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ProfessorDto.Saida> listar(@Parameter(description = "Nome da competência (opcional)", example = "java") @RequestParam(required = false) String competencia) {
        return service.listar(competencia);
    }
    @Operation(summary = "Consultar professor por ID", description = "Qualquer usuário autenticado. A consulta atual também retorna professores inativos.")
    @ApiResponse(responseCode = "200", description = "Professor encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfessorDto.Saida.class)))
    @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "400", description = "ID com formato inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProfessorDto.Saida consultar(@Parameter(description = "Identificador do professor", example = "1") @PathVariable Long id) { return service.consultar(id); }
    @Operation(summary = "Cadastrar professor", description = "Exige perfil ADMINISTRADOR, sessão e token CSRF no cabeçalho X-CSRF-TOKEN. Nome é aparado; e-mail e competências são aparados e convertidos para minúsculas. Competências repetidas são unificadas.")
    @ApiResponse(responseCode = "201", description = "Cadastro salvo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfessorDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "JSON, ID ou campos inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Cadastro duplicado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProfessorDto.Saida criar(@Valid @RequestBody ProfessorDto.Entrada entrada) {
        return service.criar(entrada);
    }
    @Operation(summary = "Atualizar professor", description = "Exige perfil ADMINISTRADOR, sessão e token CSRF no cabeçalho X-CSRF-TOKEN. Nome é aparado; e-mail e competências são aparados e convertidos para minúsculas. Competências repetidas são unificadas.")
    @ApiResponse(responseCode = "200", description = "Cadastro salvo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfessorDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "JSON, ID ou campos inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Cadastro duplicado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProfessorDto.Saida atualizar(@Parameter(description = "Identificador do professor", example = "1") @PathVariable Long id, @Valid @RequestBody ProfessorDto.Entrada entrada) {
        return service.atualizar(id, entrada);
    }
    @Operation(summary = "Desativar professor", description = "Exige perfil ADMINISTRADOR, sessão e token CSRF no cabeçalho X-CSRF-TOKEN. Desativação lógica; preserva o cadastro e as competências. Repetir a operação mantém o professor inativo.")
    @ApiResponse(responseCode = "204", description = "Professor desativado, sem corpo", content = @Content)
    @ApiResponse(responseCode = "400", description = "ID com formato inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @PatchMapping("/{id}/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void desativar(@Parameter(description = "Identificador do professor", example = "1") @PathVariable Long id) { service.desativar(id); }
}
