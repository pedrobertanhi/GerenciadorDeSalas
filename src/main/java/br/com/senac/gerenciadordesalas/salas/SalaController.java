package br.com.senac.gerenciadordesalas.salas;

import br.com.senac.gerenciadordesalas.erro.ErroResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Salas", description = "Cadastro e pesquisa de salas reservaveis")
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
@RequestMapping("/api/salas")
@PreAuthorize("hasRole('ADMINISTRADOR')") // Garante que o CRUD inteiro e exclusivo do Administrador
public class SalaController {

    private final SalaService service;

    public SalaController(SalaService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastrar sala",
            description = "Exige ADMINISTRADOR, sessao e token CSRF. O nome deve ser unico.")
    @ApiResponse(responseCode = "201", description = "Sala cadastrada",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados invalidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Nome de sala duplicado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PostMapping
    public ResponseEntity<SalaResponseDTO> criar(@RequestBody @Valid SalaRequestDTO dto) {
        Sala salaCriada = service.criar(dto.toEntity());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(salaCriada.getId())
                .toUri();
        return ResponseEntity.created(uri).body(new SalaResponseDTO(salaCriada));
    }

    @Operation(summary = "Pesquisar salas ativas",
            description = "Qualquer usuario autenticado. Aceita filtros opcionais por capacidade minima e localizacao, com paginacao e ordenacao.")
    @ApiResponse(responseCode = "200", description = "Pagina de salas ativas",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaResponseDTO.class)))
    @GetMapping
    @PreAuthorize("isAuthenticated()") // Matriz de permissoes: pesquisa liberada aos tres perfis
    public ResponseEntity<Page<SalaResponseDTO>> pesquisar(
            @Valid SalaFiltroDTO filtro,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(service.pesquisar(filtro, pageable));
    }

    @Operation(summary = "Consultar sala ativa por ID",
            description = "Qualquer usuario autenticado. Salas inativas nao sao expostas pela consulta.")
    @ApiResponse(responseCode = "200", description = "Sala encontrada",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "Sala nao encontrada ou inativa",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Matriz de permissoes: consulta liberada aos tres perfis
    public ResponseEntity<SalaResponseDTO> buscarPorId(@PathVariable Long id) {
        Sala sala = service.buscarPorId(id);
        return ResponseEntity.ok(new SalaResponseDTO(sala));
    }

    @Operation(summary = "Atualizar sala",
            description = "Exige ADMINISTRADOR, sessao e token CSRF. Preserva o estado ativo do cadastro.")
    @ApiResponse(responseCode = "200", description = "Sala atualizada",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados invalidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "404", description = "Sala nao encontrada",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "409", description = "Nome de sala duplicado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PutMapping("/{id}")
    public ResponseEntity<SalaResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid SalaRequestDTO dto) {
        Sala salaAtualizada = service.atualizar(id, dto.toEntity());
        return ResponseEntity.ok(new SalaResponseDTO(salaAtualizada));
    }

    @Operation(summary = "Inativar sala",
            description = "Exige ADMINISTRADOR e token CSRF. A inativacao e logica e preserva o historico.")
    @ApiResponse(responseCode = "204", description = "Sala inativada", content = @Content)
    @ApiResponse(responseCode = "404", description = "Sala nao encontrada",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResposta.class)))
    @PatchMapping("/{id}/inativar")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build(); // Retorna 204 No Content apos inativar
    }
}