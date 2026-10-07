package br.com.senac.gerenciadordesalas.erro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String novoCorrelationId() {
        return UUID.randomUUID().toString();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException ex) {
        String correlationId = novoCorrelationId();
        List<ErroResposta.CampoErro> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErroResposta.CampoErro(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("Validacao invalida. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "VALIDACAO_INVALIDA",
                "Um ou mais campos estao invalidos. Corrija e tente novamente.",
                correlationId,
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        String correlationId = novoCorrelationId();

        log.warn("Recurso nao encontrado. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "RECURSO_NAO_ENCONTRADO",
                "O recurso solicitado nao foi encontrado.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> tratarAcessoNegado(AccessDeniedException ex) {
        String correlationId = novoCorrelationId();

        log.warn("Acesso negado. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "ACESSO_NEGADO",
                "Voce nao tem permissao para acessar este recurso.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    @ExceptionHandler(ConflitoDeEstadoException.class)
    public ResponseEntity<ErroResposta> tratarConflito(ConflitoDeEstadoException ex) {
        String correlationId = novoCorrelationId();

        log.warn("Conflito de estado. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "CONFLITO_DE_ESTADO",
                "A operacao nao pode ser concluida devido a um conflito de estado atual do recurso.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }
}