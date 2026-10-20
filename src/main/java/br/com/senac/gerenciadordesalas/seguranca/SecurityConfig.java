package br.com.senac.gerenciadordesalas.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
    public SecurityFilterChain filtrosDeSeguranca(HttpSecurity http, RespostasDeSeguranca respostas)
            throws Exception {
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
                        .successHandler((requisicao, resposta, autenticacao) ->
                                respostas.escreverLoginRealizado(resposta, autenticacao))
                        .failureHandler((requisicao, resposta, excecao) ->
                                respostas.escrever(requisicao, resposta, HttpStatus.UNAUTHORIZED,
                                        "CREDENCIAIS_INVALIDAS", "E-mail ou senha invalidos.")))
                .logout(saida -> saida
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((requisicao, resposta, autenticacao) ->
                                resposta.setStatus(HttpStatus.NO_CONTENT.value())));

        return http.build();
    }
}