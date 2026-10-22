package br.com.senac.gerenciadordesalas.material;

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

@Tag(name = "Materiais", description = "Cadastro de materiais e equipamentos reserváveis")
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
@RequestMapping("/api/materiais")
public class MaterialController {

    private final MaterialService service;

    public MaterialController(MaterialService service) {
        this.service = service;
    }

    @Operation(summary = "Listar materiais ativos",
            description = "Qualquer usuário autenticado. Aceita filtros opcionais por tipo e restrição.")
    @ApiResponse(responseCode = "200", description = "Lista de materiais ativos",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = MaterialDto.Saida.class))))
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MaterialDto.Saida> listar(
            @Parameter(description = "MATERIAL ou EQUIPAMENTO")
            @RequestParam(required = false) MaterialTipo tipo,
            @Parameter(description = "Filtra recursos que exigem aprovação")
            @RequestParam(required = false) Boolean restrito) {
        return service.listar(tipo, restrito);
    }

    @Operation(summary = "Consultar material ativo por ID",
            description = "Qualquer usuário autenticado. Materiais inativos não são expostos pela consulta.")
    @ApiResponse(responseCode = "200", description = "Material encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MaterialDto.Saida.class)))
    @ApiResponse(responseCode = "404", description = "Material não encontrado ou inativo",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public MaterialDto.Saida consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @Operation(summary = "Cadastrar material",
            description = "Exige ADMINISTRADOR, sessão e token CSRF. Patrimônio é opcional, normalizado e único.")
    @ApiResponse(responseCode = "201", description = "Material cadastrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MaterialDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Patrimônio duplicado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MaterialDto.Saida criar(@Valid @RequestBody MaterialDto.Entrada entrada) {
        return service.criar(entrada);
    }

    @Operation(summary = "Atualizar material",
            description = "Exige ADMINISTRADOR, sessão e token CSRF. Preserva o estado ativo do cadastro.")
    @ApiResponse(responseCode = "200", description = "Material atualizado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MaterialDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "404", description = "Material não encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Patrimônio duplicado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MaterialDto.Saida atualizar(@PathVariable Long id,
                                       @Valid @RequestBody MaterialDto.Entrada entrada) {
        return service.atualizar(id, entrada);
    }

    @Operation(summary = "Desativar material",
            description = "Exige ADMINISTRADOR e token CSRF. A desativação é lógica e preserva o histórico.")
    @ApiResponse(responseCode = "204", description = "Material desativado", content = @Content)
    @ApiResponse(responseCode = "404", description = "Material não encontrado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PatchMapping("/{id}/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void desativar(@PathVariable Long id) {
        service.desativar(id);
    }
}
