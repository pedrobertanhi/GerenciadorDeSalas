package br.com.senac.gerenciadordesalas.erro;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String correlationIdDaRequisicao(HttpServletRequest request) {
        Object existente = request.getAttribute(CorrelationIdFilter.HEADER_CORRELATION_ID);
        return existente != null ? existente.toString() : UUID.randomUUID().toString();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = correlationIdDaRequisicao(request);
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

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResposta> tratarValidacaoDeParametros(HandlerMethodValidationException ex, HttpServletRequest request) {
        String correlationId = correlationIdDaRequisicao(request);
        List<ErroResposta.CampoErro> fieldErrors = ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(err -> new ErroResposta.CampoErro(
                        err.getCodes() != null && err.getCodes().length > 0 ? err.getCodes()[0] : "parametro",
                        err.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("Parametro invalido. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "VALIDACAO_INVALIDA",
                "Um ou mais parametros estao invalidos. Corrija e tente novamente.",
                correlationId,
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErroResposta> tratarRequisicaoMalFormada(Exception ex, HttpServletRequest request) {
        String correlationId = correlationIdDaRequisicao(request);

        log.warn("Requisicao mal formada. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "REQUISICAO_INVALIDA",
                "A requisicao esta mal formada. Verifique o formato dos dados enviados.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        String correlationId = correlationIdDaRequisicao(request);

        log.warn("Recurso nao encontrado. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "RECURSO_NAO_ENCONTRADO",
                "O recurso solicitado nao foi encontrado.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErroResposta> tratarRotaNaoMapeada(Exception ex, HttpServletRequest request) {
        String correlationId = correlationIdDaRequisicao(request);

        log.warn("Rota nao mapeada. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "RECURSO_NAO_ENCONTRADO",
                "O recurso solicitado nao foi encontrado.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> tratarAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        String correlationId = correlationIdDaRequisicao(request);

        log.warn("Acesso negado. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "ACESSO_NEGADO",
                "Voce nao tem permissao para acessar este recurso.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    @ExceptionHandler(ConflitoDeEstadoException.class)
    public ResponseEntity<ErroResposta> tratarConflito(ConflitoDeEstadoException ex, HttpServletRequest request) {
        String correlationId = correlationIdDaRequisicao(request);

        log.warn("Conflito de estado. correlationId={}", correlationId);

        ErroResposta erro = new ErroResposta(
                "CONFLITO_DE_ESTADO",
                "A operacao nao pode ser concluida devido a um conflito de estado atual do recurso.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroInterno(Exception ex, HttpServletRequest request) {
        String correlationId = correlationIdDaRequisicao(request);

        log.error("Erro interno nao tratado. correlationId={}", correlationId, ex);

        ErroResposta erro = new ErroResposta(
                "ERRO_INTERNO",
                "Ocorreu um erro inesperado. Tente novamente mais tarde ou contate o suporte informando o codigo de referencia.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}