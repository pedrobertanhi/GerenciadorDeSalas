package br.com.senac.gerenciadordesalas.usuario;

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
import org.springframework.security.core.Authentication;
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

@Tag(name = "Usuários", description = "Administração de usuários e perfis, exclusiva do administrador")
@SecurityRequirement(name = "sessao")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Sessão ausente, expirada ou de usuário inativo (Accept: application/json)",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão ou token CSRF ausente/inválido nas escritas",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
    @ApiResponse(responseCode = "500", description = "Erro interno, sem detalhes sensíveis",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
})
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioAdminService service;

    public UsuarioController(UsuarioAdminService service) {
        this.service = service;
    }

    @Operation(summary = "Listar usuários", description = "Exige perfil ADMINISTRADOR. Filtros opcionais por perfil e por situação; ordenado por nome. Nunca retorna senha nem hash.")
    @ApiResponse(responseCode = "200", description = "Lista de usuários, possivelmente vazia",
        content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioDto.Saida.class))))
    @ApiResponse(responseCode = "400", description = "Filtro com formato inválido",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @GetMapping
    public List<UsuarioDto.Saida> listar(
            @Parameter(description = "Perfil (opcional)", example = "RESPONSAVEL") @RequestParam(required = false) Role perfil,
            @Parameter(description = "Situação: true para ativos, false para inativos (opcional)") @RequestParam(required = false) Boolean ativo) {
        return service.listar(perfil, ativo);
    }

    @Operation(summary = "Consultar usuário por ID", description = "Exige perfil ADMINISTRADOR.")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDto.Saida.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "400", description = "ID com formato inválido",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @GetMapping("/{id}")
    public UsuarioDto.Saida consultar(@Parameter(description = "Identificador do usuário", example = "1") @PathVariable Long id) {
        return service.consultar(id);
    }

    @Operation(summary = "Cadastrar usuário", description = "Exige perfil ADMINISTRADOR, sessão e token CSRF no cabeçalho X-CSRF-TOKEN. A senha é guardada somente como hash e não volta na resposta.")
    @ApiResponse(responseCode = "201", description = "Usuário criado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "JSON ou campos inválidos (incluindo senha fora da política)",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @SecurityRequirement(name = "csrf")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto.Saida criar(@Valid @RequestBody UsuarioDto.Entrada entrada, Authentication autenticacao) {
        return service.criar(entrada, autenticacao.getName());
    }

    @Operation(summary = "Atualizar nome e perfil", description = "Exige perfil ADMINISTRADOR, sessão e token CSRF. Não remove o perfil do último administrador ativo.")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDto.Saida.class)))
    @ApiResponse(responseCode = "400", description = "JSON, ID ou campos inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Operação deixaria o sistema sem administrador ativo",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @SecurityRequirement(name = "csrf")
    @PutMapping("/{id}")
    public UsuarioDto.Saida atualizar(@Parameter(description = "Identificador do usuário", example = "1") @PathVariable Long id,
                                      @Valid @RequestBody UsuarioDto.Atualizacao atualizacao, Authentication autenticacao) {
        return service.atualizar(id, atualizacao, autenticacao.getName());
    }

    @Operation(summary = "Inativar usuário", description = "Exige perfil ADMINISTRADOR, sessão e token CSRF. Não apaga o usuário; sessões abertas dele deixam de valer na próxima requisição. Não inativa o último administrador ativo.")
    @ApiResponse(responseCode = "200", description = "Usuário inativado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDto.Saida.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Usuário já inativo ou último administrador ativo",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @SecurityRequirement(name = "csrf")
    @PatchMapping("/{id}/inativar")
    public UsuarioDto.Saida inativar(@Parameter(description = "Identificador do usuário", example = "1") @PathVariable Long id,
                                     Authentication autenticacao) {
        return service.inativar(id, autenticacao.getName());
    }

    @Operation(summary = "Reativar usuário", description = "Exige perfil ADMINISTRADOR, sessão e token CSRF.")
    @ApiResponse(responseCode = "200", description = "Usuário reativado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDto.Saida.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Usuário já ativo",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    @SecurityRequirement(name = "csrf")
    @PatchMapping("/{id}/ativar")
    public UsuarioDto.Saida ativar(@Parameter(description = "Identificador do usuário", example = "1") @PathVariable Long id,
                                   Authentication autenticacao) {
        return service.ativar(id, autenticacao.getName());
    }
}
