package br.com.senac.gerenciadordesalas.salas;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/salas")
@PreAuthorize("hasRole('ADMINISTRADOR')") // Garante que o CRUD inteiro é exclusivo do Administrador
public class SalaController {

    private final SalaService service;

    public SalaController(SalaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SalaResponseDTO> criar(@RequestBody @Valid SalaRequestDTO dto) {
        Sala salaCriada = service.criar(dto.toEntity());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(salaCriada.getId())
                .toUri();
        return ResponseEntity.created(uri).body(new SalaResponseDTO(salaCriada));
    }

    @GetMapping
    public ResponseEntity<List<SalaResponseDTO>> listarTodas() {
        List<SalaResponseDTO> salas = service.listarTodas().stream()
                .map(SalaResponseDTO::new)
                .toList();
        return ResponseEntity.ok(salas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaResponseDTO> buscarPorId(@PathVariable Long id) {
        Sala sala = service.buscarPorId(id);
        return ResponseEntity.ok(new SalaResponseDTO(sala));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid SalaRequestDTO dto) {
        Sala salaAtualizada = service.atualizar(id, dto.toEntity());
        return ResponseEntity.ok(new SalaResponseDTO(salaAtualizada));
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build(); // Retorna 204 No Content após inativar
    }
}