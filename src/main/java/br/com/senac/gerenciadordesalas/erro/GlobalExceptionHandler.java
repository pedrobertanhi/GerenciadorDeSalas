package br.com.senac.gerenciadordesalas.erro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}