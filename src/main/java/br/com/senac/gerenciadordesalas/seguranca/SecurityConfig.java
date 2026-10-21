package br.com.senac.gerenciadordesalas.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Autenticação por formulário com sessão no servidor (ADR-005).
 * Negação por padrão: tudo exige autenticação, exceto o próprio login.
 * A autorização por perfil e por propriedade é a SEC-03.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public LimitadorTentativasLogin limitadorTentativasLogin() {
        return new LimitadorTentativasLogin();
    }

    @Bean
    public FiltroLimiteTentativasLogin filtroLimiteTentativasLogin(
            LimitadorTentativasLogin limitador, RespostasDeSeguranca respostas) {
        return new FiltroLimiteTentativasLogin(limitador, respostas);
    }

    @Bean
    public SecurityFilterChain filtrosDeSeguranca(
            HttpSecurity http,
            RespostasDeSeguranca respostas,
            FiltroLimiteTentativasLogin filtroLimite,
            LimitadorTentativasLogin limitador) throws Exception {
        http
                .authorizeHttpRequests(requisicoes -> requisicoes
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/usuarios/**",
                                "/api/admin/usuarios/**",
                                "/api/reservas/**",
                                "/api/movimentacoes/**",
                                "/api/auditoria/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/auditoria/**").denyAll()
                        .requestMatchers(HttpMethod.PUT, "/api/auditoria/**").denyAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/auditoria/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/auditoria/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/admin/**", "/api/usuarios/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST,
                                "/api/reservas/*/aprovar",
                                "/api/reservas/*/rejeitar",
                                "/api/reservas/*/iniciar",
                                "/api/reservas/*/concluir",
                                "/api/reservas/*/nao-comparecimento")
                                .hasRole("RESPONSAVEL")
                        .requestMatchers(HttpMethod.POST, "/api/movimentacoes/**")
                                .hasRole("RESPONSAVEL")
                        .requestMatchers(HttpMethod.GET, "/api/movimentacoes/minhas/**")
                                .hasRole("SOLICITANTE")
                        .requestMatchers(HttpMethod.GET, "/api/movimentacoes/operacionais/**")
                                .hasAnyRole("RESPONSAVEL", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/reservas")
                                .hasRole("SOLICITANTE")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/cancelar").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservas/minhas/**")
                                .hasRole("SOLICITANTE")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/operacionais/**")
                                .hasAnyRole("RESPONSAVEL", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST,
                                "/api/salas/**", "/api/materiais/**", "/api/manutencoes/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/salas/**", "/api/materiais/**", "/api/manutencoes/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/salas/**", "/api/materiais/**", "/api/manutencoes/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/professores", "/professores/**")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/professores", "/professores/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/professores/*/validacao")
                                .hasRole("RESPONSAVEL")
                        .requestMatchers(HttpMethod.POST, "/api/professores/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/professores/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/professores/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET,
                                "/api/perfil",
                                "/api/disponibilidade/**",
                                "/api/professores/**",
                                "/api/salas/**",
                                "/api/materiais/**",
                                "/api/manutencoes/**")
                                .authenticated()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                .authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas))
                .formLogin(formulario -> formulario
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .successHandler((requisicao, resposta, autenticacao) -> {
                            limitador.registrarSucesso(
                                    requisicao.getParameter("email"), requisicao.getRemoteAddr());
                            respostas.escreverLoginRealizado(resposta, autenticacao);
                        })
                        .failureHandler((requisicao, resposta, excecao) -> {
                            limitador.registrarFalha(
                                    requisicao.getParameter("email"), requisicao.getRemoteAddr());
                            respostas.escrever(requisicao, resposta, HttpStatus.UNAUTHORIZED,
                                    "CREDENCIAIS_INVALIDAS", "E-mail ou senha invalidos.");
                        }))
                .logout(saida -> saida
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((requisicao, resposta, autenticacao) ->
                                resposta.setStatus(HttpStatus.NO_CONTENT.value())))
                .addFilterBefore(filtroLimite, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
