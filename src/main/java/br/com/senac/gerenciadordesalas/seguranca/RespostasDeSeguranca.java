package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.erro.ErroResposta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Respostas do Spring Security no mesmo contrato de erro da API (ErroResposta).
 * O Spring Security age antes dos controllers, então o GlobalExceptionHandler
 * não alcança essas respostas. As mensagens são neutras e não vazam detalhes.
 */
@Component
public class RespostasDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespostasDeSeguranca(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excecao) throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO",
                "Autenticacao necessaria para acessar este recurso.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excecao) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                "Voce nao tem permissao para acessar este recurso.");
    }

    public void escrever(HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String codigo, String mensagem) throws IOException {
        String correlationId = correlationIdDaRequisicao(request, response);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErroResposta(codigo, mensagem, correlationId));
    }

    public void escreverLoginRealizado(HttpServletResponse response, Authentication autenticacao)
            throws IOException {
        String perfil = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(autoridade -> autoridade.replaceFirst("^ROLE_", ""))
                .findFirst()
                .orElse("");

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("email", autenticacao.getName(), "perfil", perfil));
    }

    private String correlationIdDaRequisicao(HttpServletRequest request, HttpServletResponse response) {
        Object existente = request.getAttribute(CorrelationIdFilter.HEADER_CORRELATION_ID);
        if (existente != null) {
            return existente.toString();
        }
        String novo = UUID.randomUUID().toString();
        response.setHeader(CorrelationIdFilter.HEADER_CORRELATION_ID, novo);
        return novo;
    }
}