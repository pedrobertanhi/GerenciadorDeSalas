package br.com.senac.gerenciadordesalas.professor;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/professores")
public class ProfessorController {
    private final ProfessorService service;
    public ProfessorController(ProfessorService service) { this.service = service; }
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ProfessorDto.Saida> listar(@RequestParam(required = false) String competencia) {
        return service.listar(competencia);
    }
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProfessorDto.Saida consultar(@PathVariable Long id) { return service.consultar(id); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProfessorDto.Saida criar(@Valid @RequestBody ProfessorDto.Entrada entrada) {
        return service.criar(entrada);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProfessorDto.Saida atualizar(@PathVariable Long id, @Valid @RequestBody ProfessorDto.Entrada entrada) {
        return service.atualizar(id, entrada);
    }
    @PatchMapping("/{id}/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void desativar(@PathVariable Long id) { service.desativar(id); }
}