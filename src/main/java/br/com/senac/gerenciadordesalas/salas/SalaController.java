package br.com.senac.gerenciadordesalas.salas;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<SalaResponseDTO>> pesquisar(
            @Valid SalaFiltroDTO filtro,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable
    ) {
        Page<SalaResponseDTO> resultado = salaService.pesquisar(filtro, pageable);
        return ResponseEntity.ok(resultado);
    }
}