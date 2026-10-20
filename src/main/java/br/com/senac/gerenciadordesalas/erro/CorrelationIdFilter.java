package br.com.senac.gerenciadordesalas.erro;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Executa antes dos demais filtros, inclusive o do Spring Security, para que as
 * respostas 401 e 403 também carreguem o correlation id da requisição.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString();
        response.setHeader(HEADER_CORRELATION_ID, correlationId);
        request.setAttribute(HEADER_CORRELATION_ID, correlationId);
        filterChain.doFilter(request, response);
    }
}