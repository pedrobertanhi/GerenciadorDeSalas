package br.com.senac.gerenciadordesalas.erro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-erros")
class TestErrorController {

    @GetMapping("/nao-encontrado")
    void naoEncontrado() {
        throw new RecursoNaoEncontradoException("Recurso de teste nao encontrado");
    }

    @GetMapping("/negado")
    void negado() {
        throw new AccessDeniedException("Acesso negado ao recurso de teste");
    }

    @GetMapping("/conflito")
    void conflito() {
        throw new ConflitoDeEstadoException("Conflito de estado de teste");
    }

    @GetMapping("/erro-interno")
    void erroInterno() {
        throw new RuntimeException("Falha interna de teste");
    }

    @PostMapping("/validar")
    void validar(@Valid @org.springframework.web.bind.annotation.RequestBody CorpoDeTeste corpo) {
    }

    record CorpoDeTeste(@NotBlank(message = "nome e obrigatorio") String nome) {}
}