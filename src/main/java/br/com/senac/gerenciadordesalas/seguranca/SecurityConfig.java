package br.com.senac.gerenciadordesalas.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtrosDeSeguranca(
            HttpSecurity http,
            RespostasDeSeguranca respostas,
            FiltroLimiteTentativasLogin filtroLimite,
            LimitadorTentativasLogin limitador) throws Exception {
        http
                .authorizeHttpRequests(requisicoes -> requisicoes
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated())
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
