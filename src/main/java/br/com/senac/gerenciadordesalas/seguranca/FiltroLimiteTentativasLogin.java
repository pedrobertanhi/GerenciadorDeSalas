package br.com.senac.gerenciadordesalas.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Recusa temporariamente tentativas de login acima do limite antes de executar
 * a validação de senha, reduzindo adivinhação de credenciais e custo de BCrypt.
 */
public class FiltroLimiteTentativasLogin extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FiltroLimiteTentativasLogin.class);

    private final LimitadorTentativasLogin limitador;
    private final RespostasDeSeguranca respostas;

    public FiltroLimiteTentativasLogin(LimitadorTentativasLogin limitador,
                                       RespostasDeSeguranca respostas) {
        this.limitador = limitador;
        this.respostas = respostas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        if (!ehTentativaDeLogin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = request.getParameter("email");
        String origem = request.getRemoteAddr();
        Duration espera = limitador.tempoRestante(email, origem);

        if (espera.isZero() || espera.isNegative()) {
            filterChain.doFilter(request, response);
            return;
        }

        long segundos = Math.max(1, (espera.toMillis() + 999) / 1000);
        response.setHeader("Retry-After", Long.toString(segundos));
        LOGGER.warn("Tentativa de login limitada; origemId={}",
                limitador.identificadorSeguroDaOrigem(origem));
        respostas.escrever(request, response, HttpStatus.TOO_MANY_REQUESTS,
                "TENTATIVAS_EXCEDIDAS",
                "Nao foi possivel autenticar. Tente novamente mais tarde.");
    }

    private boolean ehTentativaDeLogin(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && "/login".equals(request.getServletPath());
    }
}
